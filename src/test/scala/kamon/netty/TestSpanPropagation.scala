package kamon.netty

import java.lang

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.{HttpContent, LastHttpContent, _}
import io.netty.util.CharsetUtil

import scala.annotation.tailrec

object TestSpanPropagation extends App {

  val MAX_CONTENT_LENGTH = 1000
  val workerGroup = new NioEventLoopGroup

  try {
    val requestGenerator = new RequestGenerator()
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

    println("connect to server")

    val host = "127.0.0.1"
    val port = 8080

    val ch  = boot.connect(host, port).sync().channel()

    // Send the HTTP request N times.
    callN(ch, requestGenerator.buildRequest(host))(5)

    ch.closeFuture().sync()
    println("client exit")
  } finally {
    workerGroup.shutdownGracefully()
  }

  @tailrec
  def callN(channel: Channel, request: => FullHttpRequest)(count: Int): Unit = {
    if (count >= 0) {
      channel.writeAndFlush(request).sync()
      callN(channel, request)(count - 1)
    } else channel.close().sync()
  }
}

class HttpClientHandler(requestGenerator: RequestGenerator) extends SimpleChannelInboundHandler[HttpObject] {
  import scala.collection.JavaConverters._

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {

    if (msg.isInstanceOf[HttpResponse]) {
      val response = msg.asInstanceOf[HttpResponse]

      println(s"Response: $response")

      response.headers.names.asScala
        .find(_ == requestGenerator.HeaderKamonTest)
        .orElse({
          println(s"Doesn't get a value for header ${requestGenerator.HeaderKamonTest}.")
          None
        })
        .map(name => {
          val values = response.headers.getAll(name).asScala
          if (values.size > 1) println(s"Get more than 1 value for header ${requestGenerator.HeaderKamonTest}. Values: ${values mkString ","}")
          values
        })
        .foreach(value => {
          val isTheSame = value.toString() == requestGenerator.nextSequence.toString
          s"Get the same request sequence: $isTheSame. Seq expected: ${requestGenerator.nextSequence}. Seq received: $value"
        })
//      if (!response.headers.isEmpty) {
//        for {
//          name <- response.headers.names.asScala
//          value <- response.headers.getAll(name).asScala
//        } println("HEADER: " + name + " = " + value)
//      }
      if (HttpHeaders.isTransferEncodingChunked(response)) println("CHUNKED CONTENT {")
      else println("CONTENT {")
    }
    if (msg.isInstanceOf[HttpContent]) {
      val content = msg.asInstanceOf[HttpContent]
      println(content.content.toString(CharsetUtil.UTF_8))
      if (content.isInstanceOf[LastHttpContent]) {
        println("} END OF CONTENT")
//        ctx.close
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace()
    ctx.close
  }
}

class RequestGenerator() {

  val HeaderKamonTest = "kamon-test-sequence"
  private def computeNextSequence = scala.util.Random.nextInt(10000)
  val nextSequence: Int = computeNextSequence

  def buildRequest(host: String, keepAlive: Boolean = true): FullHttpRequest = {
    computeNextSequence
    val keepAliveValue = if (keepAlive) HttpHeaders.Values.KEEP_ALIVE else HttpHeaders.Values.CLOSE
    val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.headers().set(HeaderKamonTest, nextSequence)
    request.headers().set(HttpHeaders.Names.HOST, host)
    request.headers().set(HttpHeaders.Names.CONNECTION, keepAliveValue)
    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)
    request
  }
}
