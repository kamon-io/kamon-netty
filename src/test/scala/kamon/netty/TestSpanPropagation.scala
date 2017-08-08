package kamon.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{HttpContent, LastHttpContent, _}
import io.netty.util.CharsetUtil

object TestSpanPropagation extends App {

  val workerGroup = new NioEventLoopGroup

  try {
    val boot = new Bootstrap()
    boot.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel]() {
        def initChannel(ch: SocketChannel) {
          val p = ch.pipeline()
          p.addLast(new HttpClientCodec())
          p.addLast(new HttpClientHandler())
        }
      })
      .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

    println("connect to server")

    val host = "127.0.0.1"
    val port = 8080

    val ch  = boot.connect(host, port).sync().channel()

    val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.headers().set(HttpHeaders.Names.HOST, host)
    request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE)
    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)

    // Send the HTTP request.
    ch.writeAndFlush(request)

    ch.closeFuture().sync()
    println("client exit")
  } finally {
    workerGroup.shutdownGracefully()
  }
}

class HttpClientHandler extends SimpleChannelInboundHandler[HttpObject] {
  import scala.collection.JavaConverters._

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {

    if (msg.isInstanceOf[HttpResponse]) {
      val response = msg.asInstanceOf[HttpResponse]
      println("STATUS: " + response.getStatus)
      println("VERSION: " + response.getProtocolVersion)
      if (!response.headers.isEmpty) {
        for {
          name <- response.headers.names.asScala
          value <- response.headers.getAll(name).asScala
        } println("HEADER: " + name + " = " + value)
      }
//      if (HttpUtil.isTransferEncodingChunked(response)) System.err.println("CHUNKED CONTENT {")
//      else System.err.println("CONTENT {")
    }
    if (msg.isInstanceOf[HttpContent]) {
      val content = msg.asInstanceOf[HttpContent]
      println(content.content.toString(CharsetUtil.UTF_8))
      if (content.isInstanceOf[LastHttpContent]) {
        println("END OF CONTENT")
        ctx.close
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    ctx.close
  }
}
