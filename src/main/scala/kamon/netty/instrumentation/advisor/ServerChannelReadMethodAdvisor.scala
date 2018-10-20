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

package kamon.netty.instrumentation
package advisor

import io.netty.channel.Channel
import kamon.netty.instrumentation.ServerBootstrapInstrumentation.KamonHandler
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit}

/**
  * Advisor for io.netty.bootstrap.ServerBootstrap.ServerBootstrapAcceptor::channelRead
  */
class ServerChannelReadMethodAdvisor
object ServerChannelReadMethodAdvisor {

  @OnMethodExit
  def onExit(@Argument(1) child: AnyRef): Unit = {
    val pipeline = child.asInstanceOf[Channel].pipeline()
    if(pipeline.get(KamonHandler) == null)
      pipeline.addFirst(KamonHandler, new KamonHandlerPortable.KamonHandler())
  }

}
