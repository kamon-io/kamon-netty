package kamon.netty.instrumentation.advisor

import io.netty.channel.Channel
import io.netty.util.concurrent.EventExecutor
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, This}
import kamon.netty.Metrics
import kamon.netty.util.EventLoopUtils.name

class RemoveMethodAdvisor
object RemoveMethodAdvisor {

  @OnMethodExit
  def onExit(@This eventLoop: EventExecutor, @Argument(0) channel: Any): Unit = {
    if(channel.asInstanceOf[Channel].isOpen)
      Metrics.forEventLoop(name(eventLoop)).registeredChannels.decrement()
  }

}