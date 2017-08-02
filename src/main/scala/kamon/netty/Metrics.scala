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

package kamon.netty

import kamon.Kamon
import kamon.metric._
import kamon.util.MeasurementUnit._


object Metrics {

  /**
    *
    * Metrics for Netty Event Loops:
    *
    *    - registered-channels:The number of registered Channels.
    *    - last-io-processing-time: The number of milliseconds the last processing of all IO took..
    *    - last-task-processing-time: The the number of milliseconds the last processing of all tasks took..
    *    - last-processed-channels: The number of Channel's that where processed in the last run..
    *    - last-processed-channels: The number of milliseconds the  EventLoop blocked before run to pick up IO and tasks.
    */
  val registeredChannelsMetric = Kamon.gauge("netty.event-loop.registered-channels")
  val lastIoProcessingTimeMetric = Kamon.histogram("netty.event-loop.last-io-processing-time", time.nanoseconds)
  val lastTaskProcessingTimeMetric = Kamon.histogram("netty.event-loop.last-task-processing-time", time.nanoseconds)
  val lastProcessedChannelsMetric = Kamon.gauge("netty.event-loop.last-processed-channels")
  val lastBlockingAmountMetric = Kamon.histogram("netty.event-loop.last-blocking-amount", time.nanoseconds)

  def forEventLoop(name: String): EventLoopMetrics = {
    val eventLoopTags = Map("name" -> name)
    EventLoopMetrics(
      eventLoopTags,
      registeredChannelsMetric.refine(eventLoopTags),
      lastIoProcessingTimeMetric.refine(eventLoopTags),
      lastTaskProcessingTimeMetric.refine(eventLoopTags),
      lastProcessedChannelsMetric.refine(eventLoopTags),
      lastBlockingAmountMetric.refine(eventLoopTags)
    )
  }

  case class EventLoopMetrics(tags: Map[String, String], registeredChannels: Gauge, ioProcessingTime: Histogram,
                              taskProcessingTime: Histogram, channelProcessed: Gauge, blockingAmount: Histogram) {

    def cleanup(): Unit = {
      registeredChannelsMetric.remove(tags)
      lastIoProcessingTimeMetric.remove(tags)
      lastTaskProcessingTimeMetric.remove(tags)
      lastProcessedChannelsMetric.remove(tags)
      lastBlockingAmountMetric.remove(tags)
    }
  }


  /**
    *
    *  Metrics for Netty EventExecutor.
    *
    *    - pending-tasks: The number of tasks that are pending for processing.
    */
  val pendingTasksMetric = Kamon.gauge("netty.event-executor.pending-tasks")


  def forEventExecutor(name: String): EventExecutorMetrics = {
    val routerTags = Map("name" -> name)
    EventExecutorMetrics(routerTags, pendingTasksMetric.refine(routerTags))
  }

  case class EventExecutorMetrics(tags: Map[String, String], pendingTasks: Gauge) {

    def cleanup(): Unit = {
      pendingTasksMetric.remove(tags)
    }
  }
}