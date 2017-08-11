/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.stream.ChunkedWriteHandler
import kamon.Kamon
import kamon.netty.instrumentation.ChannelSpanAware
import kamon.netty.util.HttpUtils
import kamon.trace.{SpanContextCodec, TextMap}

class NioEventLoopBasedServer(port: Int) {
  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup
  val b = new ServerBootstrap

  b.group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new HttpServerInitializer)

  val channel: Channel = b.bind(port).sync.channel

  def close(): Unit = {
    channel.close
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

class EpollEventLoopBasedServer(port: Int) {
  val bossGroup = new EpollEventLoopGroup(1)
  val workerGroup = new EpollEventLoopGroup
  val b = new ServerBootstrap

  b.group(bossGroup, workerGroup)
    .channel(classOf[EpollServerSocketChannel])
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new HttpServerInitializer)

  val channel: Channel = b.bind(port).sync.channel

  def close(): Unit = {
    channel.close
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

object Servers {
  def withNioServer[A](port:Int = 9001)(thunk: Int => A): A = {
    val server = new NioEventLoopBasedServer(port)
    try thunk(port) finally server.close()
  }

  def withEpollServer[A](port:Int = 9001)(thunk: Int => A): A = {
    val server = new EpollEventLoopBasedServer(port)
    try thunk(port) finally server.close()
  }
}

private class HttpServerInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline
    p.addLast(new HttpServerCodec)
    p.addLast(new ChunkedWriteHandler)
    p.addLast(new HttpServerHandler)
  }
}

private class HttpServerHandler extends ChannelInboundHandlerAdapter {
  private val Content = Array[Byte]('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd')

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    if (msg.isInstanceOf[HttpRequest]) {
      val request = msg.asInstanceOf[HttpRequest]

      if(request.getUri.equals("/raise-exception")) throw new Exception("awesome-exception")

      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(Content))
      response.headers.set("Content-Type", "text/plain")
      response.headers.set("Content-Length", response.content.readableBytes)
      // Introduce current span in order to validate it on the tests
      Kamon.inject(ctx.channel().asInstanceOf[ChannelSpanAware].span.context(), SpanContextCodec.Format.HttpHeaders, HttpUtils.textMapForHttpResponse(response))
      ctx.write(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit =
    ctx.flush()

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    ctx.close()
}