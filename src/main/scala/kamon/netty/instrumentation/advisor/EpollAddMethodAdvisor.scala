package kamon.netty.instrumentation.advisor

import io.netty.util.concurrent.EventExecutor
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Enter, OnMethodEnter, OnMethodExit, This}
import kamon.netty.Metrics
import kamon.netty.util.EventLoopUtils.name

class EpollAddMethodAdvisor
object EpollAddMethodAdvisor {

  @OnMethodExit
  def onExit(@This eventLoop: EventExecutor): Unit = {
    Metrics.forEventLoop(name(eventLoop)).registeredChannels.increment()
  }

}
