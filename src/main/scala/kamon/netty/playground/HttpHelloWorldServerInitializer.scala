package kamon.netty.playground

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LoggingHandler

class HttpHelloWorldServerInitializer extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline()

//    p.addLast("logger", new LoggingHandler())
    p.addLast("codec", new HttpServerCodec())
    p.addLast("handler", new HttpHelloWorldServerHandler())
  }
}
