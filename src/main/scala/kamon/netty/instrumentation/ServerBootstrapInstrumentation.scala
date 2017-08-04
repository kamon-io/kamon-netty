package kamon.netty.instrumentation

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ServerBootstrapInstrumentation {

  @After("execution(* io.netty.bootstrap.ServerBootstrap.group(..)) && args(bossGroup, workerGroup)")
  def onNewServerBootstrap(jp:JoinPoint,bossGroup:NamedEventLoopGroup, workerGroup:NamedEventLoopGroup):Unit ={
    if(bossGroup == workerGroup) {
      bossGroup.name = "boss-group"
      workerGroup.name = "boss-group"
    } else {
      bossGroup.name = "boss-group"
      workerGroup.name = "worker-group"
    }
  }
}