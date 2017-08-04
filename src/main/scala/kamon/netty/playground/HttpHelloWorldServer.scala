package kamon.netty.playground

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelOption, EventLoopGroup}
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

object HttpHelloWorldServer extends App {
  val port = args.headOption.map(_.toInt).getOrElse(8080)
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())

//  val bossGroup:EventLoopGroup = new NioEventLoopGroup(1)
//  val workerGroup:EventLoopGroup = new NioEventLoopGroup(1)

  val bossGroup:EventLoopGroup = new EpollEventLoopGroup(1)
  val workerGroup:EventLoopGroup = new EpollEventLoopGroup(1)

  try {
    val b = new ServerBootstrap()
    b.option(ChannelOption.SO_BACKLOG, Int.box(1024))
      .group(bossGroup, workerGroup)
//      .channel(classOf[NioServerSocketChannel])
      .channel(classOf[EpollServerSocketChannel])
      .childHandler(new HttpHelloWorldServerInitializer())

    val ch = b.bind(port).sync().channel()
    ch.closeFuture().sync()
  } finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}
