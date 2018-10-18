/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kamon.netty.instrumentation.advisor

import kamon.netty.instrumentation.ServerBootstrapInstrumentation.{BossGroupName, WorkerGroupName}
import kamon.netty.instrumentation.mixin.NamedEventLoopGroup
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter}

class ServerGroupMethodAdvisor
object ServerGroupMethodAdvisor {

  @OnMethodEnter
  def onEnter(@Argument(value = 0) _bossGroup: AnyRef,
              @Argument(value = 1) _workerGroup: AnyRef): Unit = {
    val bossGroup = _bossGroup.asInstanceOf[NamedEventLoopGroup]
    val workerGroup = _workerGroup.asInstanceOf[NamedEventLoopGroup]
    if(bossGroup == workerGroup) {
      bossGroup.setName(BossGroupName)
      workerGroup.setName(BossGroupName)
    } else {
      bossGroup.setName(BossGroupName)
      workerGroup.setName(WorkerGroupName)
    }
  }
}
