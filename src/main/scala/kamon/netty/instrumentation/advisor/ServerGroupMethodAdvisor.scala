package kamon.netty.instrumentation.advisor

import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter}
import kamon.netty.instrumentation.ServerBootstrapInstrumentation.{BossGroupName, WorkerGroupName}
import kamon.netty.instrumentation.mixin.NamedEventLoopGroup

class ServerGroupMethodAdvisor
object ServerGroupMethodAdvisor {

  @OnMethodEnter
  def onEnter(@Argument(value = 0) _bossGroup: AnyRef,
              @Argument(value = 1) _workerGroup: AnyRef): Unit = {
    val bossGroup = _bossGroup.asInstanceOf[NamedEventLoopGroup]
    val workerGroup = _workerGroup.asInstanceOf[NamedEventLoopGroup]
    if(bossGroup == workerGroup) {
      bossGroup.name = BossGroupName
      workerGroup.name = BossGroupName
    } else {
      bossGroup.name = BossGroupName
      workerGroup.name = WorkerGroupName
    }
  }

}
