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

import io.netty.channel.ChannelFuture
import io.netty.util.concurrent.EventExecutor
import kamon.netty.Metrics
import kamon.netty.util.EventLoopUtils.name
import kanela.agent.libs.net.bytebuddy.asm.Advice.{OnMethodExit, Return, This}

class RegisterMethodAdvisor
object RegisterMethodAdvisor {

  @OnMethodExit
  def onExit(@This eventLoop: EventExecutor, @Return _channelFuture: AnyRef): Unit = {
    val channelFuture = _channelFuture.asInstanceOf[ChannelFuture]
    val registeredChannels = Metrics.forEventLoop(name(eventLoop)).registeredChannels

    if (channelFuture.isSuccess) registeredChannels.increment()
    else channelFuture.addListener((future: ChannelFuture) => {
      if(future.isSuccess) registeredChannels.increment()
    })
  }


}
