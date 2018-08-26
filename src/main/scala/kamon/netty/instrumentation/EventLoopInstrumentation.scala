/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

import kamon.netty.instrumentation.advisor.{EpollAddMethodAdvisor, NewTaskQueueMethodAdvisor, NioCancelMethodAdvisor, RemoveMethodAdvisor}
import kanela.agent.scala.KanelaInstrumentation

class EventLoopInstrumentation extends KanelaInstrumentation {

  forTargetType("io.netty.channel.nio.NioEventLoop") { builder ⇒
    builder
      .withAdvisorFor(method("cancel"), classOf[NioCancelMethodAdvisor])
      .withAdvisorFor(method("newTaskQueue"), classOf[NewTaskQueueMethodAdvisor])
      .build()
  }

  forTargetType("io.netty.channel.epoll.EpollEventLoop") { builder ⇒
    builder
      .withAdvisorFor(method("add"), classOf[EpollAddMethodAdvisor])
      .withAdvisorFor(method("remove"), classOf[RemoveMethodAdvisor])
      .withAdvisorFor(method("newTaskQueue"), classOf[NewTaskQueueMethodAdvisor])
      .build()
  }

  forTargetType("io.netty.channel.SingleThreadEventLoop") { builder ⇒
    builder
      .withAdvisorFor(method("register"), classOf[EpollAddMethodAdvisor])
      .build()
  }

//  @Around("execution(* io.netty.channel.SingleThreadEventLoop.register(..)) && this(eventLoop)")
//  def onRegister(pjp: ProceedingJoinPoint, eventLoop: NioEventLoop): Any = {
//    val future = pjp.proceed().asInstanceOf[ChannelFuture]
//    val registeredChannels = Metrics.forEventLoop(name(eventLoop)).registeredChannels
//
//    if (future.isSuccess) registeredChannels.increment()
//    else future.addListener(registeredChannelListener(registeredChannels))
//    future
//  }

//  @Before("execution(* io.netty.channel.nio.NioEventLoop.cancel(..)) && this(eventLoop)")
//  def onCancel(eventLoop: NioEventLoop): Unit = {
//    val registeredChannels = Metrics.forEventLoop(name(eventLoop)).registeredChannels
//    registeredChannels.decrement()
//  }

//  @Around("execution(* io.netty.channel.nio.NioEventLoop.newTaskQueue(..)) && this(eventLoop)")
//  def onNewTaskQueue(pjp: ProceedingJoinPoint, eventLoop: NioEventLoop): Any = {
//    val queue = pjp.proceed().asInstanceOf[util.Queue[Runnable]]
//    MonitoredQueue(eventLoop, queue)
//  }
}


