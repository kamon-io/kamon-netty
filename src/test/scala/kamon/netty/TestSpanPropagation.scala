package kamon.netty

import java.lang

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{HttpContent, LastHttpContent, _}
import io.netty.util.CharsetUtil
import io.netty.util.concurrent.GenericFutureListener
import kamon.trace.SpanContextCodec.ExtendedB3

import scala.annotation.tailrec
import scala.collection.immutable

object TestSpanPropagation extends App {

  val MAX_CONTENT_LENGTH = 1000
  val workerGroup = new NioEventLoopGroup(10)

  val host = "127.0.0.1"
  val port = 8080

  val parallel = 3
  val count = 10

  try {
    val channels: Seq[ChannelFuture] = (1 to parallel).map(i => taskThread())
    channels.foreach(_.channel().closeFuture().syncUninterruptibly())
    println("Test finished successfully!")
  } finally {
    workerGroup.shutdownGracefully()
  }

  private def taskThread(): ChannelFuture = {
    val requestGenerator = new RequestGenerator(host, count)
    println(s"Creating Bootstrap...")
    val boot = new Bootstrap()
    boot.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel]() {
        def initChannel(ch: SocketChannel) {
          val p = ch.pipeline()
          p.addLast(new HttpClientCodec())
          //          p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH))
          p.addLast(new HttpClientHandler(requestGenerator))
        }
      })
      .option[lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

    println("Connect to server")

    val channelFuture = boot.connect(host, port)

    channelFuture.addListener((f: ChannelFuture) => {
      f.channel().writeAndFlush(requestGenerator.buildRequest())
    })
  }
}

class HttpClientHandler(requestGenerator: RequestGenerator) extends SimpleChannelInboundHandler[HttpObject] {
  import scala.collection.JavaConverters._

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {

    if (msg.isInstanceOf[HttpResponse]) {
      val response = msg.asInstanceOf[HttpResponse]

      println(s"Response: $response")

      response.headers.names.asScala
        .find(_ == ExtendedB3.Headers.ParentSpanIdentifier)
        .orElse({
          println(s"Doesn't get a value for header ${ExtendedB3.Headers.ParentSpanIdentifier}.")
          None
        })
        .map(name => {
          val values = response.headers.getAll(name).asScala
          if (values.size > 1) println(s"Get more than 1 value for header ${ExtendedB3.Headers.ParentSpanIdentifier}. Values: ${values mkString ","}")
          values
        })
        .foreach(value => {
          val isTheSame = value.toString() == requestGenerator.nextSequence.toString
          s"Get the same request sequence: $isTheSame. Seq expected: ${requestGenerator.nextSequence}. Seq received: $value"
        })
//      if (HttpHeaders.isTransferEncodingChunked(response)) println("CHUNKED CONTENT {")
//      else println("CONTENT {")
    }
    if (msg.isInstanceOf[HttpContent]) {
      val content = msg.asInstanceOf[HttpContent]
//      println(content.content.toString(CharsetUtil.UTF_8))
      if (content.isInstanceOf[LastHttpContent]) {
//        println("} END OF CONTENT")

        requestGenerator.performRequest(ctx.channel())

        ctx.channel().flush()
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    ctx.close
  }
}

class RequestGenerator(host: String, maxAttempt: Int = 2) {

  var attempt = maxAttempt

  var nextSequence: Int = 0
  private def computeNextSequence = nextSequence = scala.util.Random.nextInt(10000)

  def performRequest(channel: Channel): Unit = {
    if (attempt > 0) {
      channel.write(this.buildRequest())
      attempt -= 1
    } else channel.close()
  }

  def buildRequest(keepAlive: Boolean = true): FullHttpRequest = {
    computeNextSequence
    val keepAliveValue = if (keepAlive) HttpHeaders.Values.KEEP_ALIVE else HttpHeaders.Values.CLOSE
    val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.headers().set(HttpHeaders.Names.HOST, host)
    request.headers().set(HttpHeaders.Names.CONNECTION, keepAliveValue)
    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)
    insertSpan(request.headers(), nextSequence)
    request
  }

  private def insertSpan(headers: HttpHeaders, nextSequence: Int): Unit = {
    import ExtendedB3.Headers._
    headers.set(TraceIdentifier, "111")
    headers.set(SpanIdentifier, "222")
    headers.set(ParentSpanIdentifier, nextSequence)
  }
}
