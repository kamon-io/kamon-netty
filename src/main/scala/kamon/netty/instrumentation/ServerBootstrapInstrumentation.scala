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

import kamon.netty.instrumentation.advisor.{ServerChannelReadMethodAdvisor, ServerGroupMethodAdvisor}
import kamon.netty.instrumentation.mixin.EventLoopMixin
import kanela.agent.scala.KanelaInstrumentation

class ServerBootstrapInstrumentation extends KanelaInstrumentation {

  forSubtypeOf("io.netty.channel.EventLoopGroup") { builder =>
    builder
      .withMixin(classOf[EventLoopMixin])
      .build()
  }

  forTargetType("io.netty.bootstrap.ServerBootstrap") { builder =>
    builder
      .withAdvisorFor(method("group").and(takesArguments(2)), classOf[ServerGroupMethodAdvisor])
      .build()
  }

  forTargetType("io.netty.bootstrap.ServerBootstrap$ServerBootstrapAcceptor") { builder =>
    builder
      .withAdvisorFor(method("channelRead").and(takesArguments(2)), classOf[ServerChannelReadMethodAdvisor])
      .build()
  }
}

object ServerBootstrapInstrumentation {
  val BossGroupName = "boss-group"
  val WorkerGroupName = "worker-group"
  val KamonHandler = "kamon-handler"
}
