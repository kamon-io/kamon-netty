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

import kamon.netty.instrumentation.advisor.{EpollAddMethodAdvisor, NewTaskQueueMethodAdvisor, RemoveMethodAdvisor}
import kanela.agent.api.instrumentation.InstrumentationBuilder


class EpollEventLoopInstrumentation extends InstrumentationBuilder {
//
//  onType("io.netty.channel.epoll.EpollEventLoop")
//      .advise(method("add"), classOf[EpollAddMethodAdvisor])
//      .advise(method("remove"), classOf[RemoveMethodAdvisor])
//      .advise(method("newTaskQueue"), classOf[NewTaskQueueMethodAdvisor])

//  @After("execution(* io.netty.channel.epoll.EpollEventLoop.add(..)) && this(eventLoop)")
//  def onAdd(eventLoop: EventExecutor): Unit =
//    Metrics.forEventLoop(name(eventLoop)).registeredChannels.increment()
//
//  @After("execution(* io.netty.channel.epoll.EpollEventLoop.remove(..)) && args(channel) && this(eventLoop)")
//  def onRemove(eventLoop: EventExecutor, channel:Channel): Unit = {
//    if(channel.isOpen)
//      Metrics.forEventLoop(name(eventLoop)).registeredChannels.decrement()
//  }
//
//  @Around("execution(* io.netty.channel.epoll.EpollEventLoop.newTaskQueue(..)) && this(eventLoop)")
//  def onNewTaskQueue(pjp: ProceedingJoinPoint, eventLoop: EventLoop): Any = {
//    val queue = pjp.proceed().asInstanceOf[util.Queue[Runnable]]
//    MonitoredQueue(eventLoop, queue)
//  }
}


