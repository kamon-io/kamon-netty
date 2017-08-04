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

import io.netty.util.concurrent.EventExecutor
import kamon.netty.Metrics
import kamon.netty.util.Latency
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class EpollEventLoopInstrumentation {
  import EventLoopUtils._

  private val EpollEventLoopClass = Class.forName("io.netty.channel.epoll.EpollEventLoop")

  @Around("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.runAllTasks(..)) && this(eventLoop)")
  def onRunAllTasks(pjp:ProceedingJoinPoint, eventLoop: EventExecutor): Any = {
    if(eventLoop.getClass.isAssignableFrom(EpollEventLoopClass)) {
      val processingTime = Metrics.forEventLoop(name(eventLoop)).taskProcessingTime
//      println("processAll")
      Latency.measure(processingTime)(pjp.proceed())
    } else pjp.proceed()
  }

  @Before("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.addTask(..)) && this(eventLoop)")
  def onAddTask(eventLoop: EventExecutor): Unit = {
    if(eventLoop.getClass.isAssignableFrom(EpollEventLoopClass)) {
      val n = name(eventLoop)
      println("addTask: " + n)
      Metrics.forEventLoop(name(eventLoop)).pendingTasks.increment()
    }
  }

  @Around("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.pollTask(..)) && this(eventLoop)")
  def onPollTask(pjp: ProceedingJoinPoint, eventLoop: EventExecutor): Any = {
    val polledTask = pjp.proceed()
    if(eventLoop.getClass.isAssignableFrom(EpollEventLoopClass)) {
      if(polledTask != null) {
        println("polledTask: " + name(eventLoop))
        Metrics.forEventLoop(name(eventLoop)).pendingTasks.decrement()
      }
    }
    polledTask
  }

  @After("execution(* io.netty.channel.epoll.EpollEventLoop.add(..)) && this(eventLoop)")
  def onAdd(eventLoop: EventExecutor): Unit =
    Metrics.forEventLoop(name(eventLoop)).registeredChannels.increment()

  @After("execution(* io.netty.channel.epoll.EpollEventLoop.remove(..)) && this(eventLoop)")
  def onRemove(eventLoop: EventExecutor): Unit =
    Metrics.forEventLoop(name(eventLoop)).registeredChannels.decrement()
}
