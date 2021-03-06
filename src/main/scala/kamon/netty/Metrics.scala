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
import kamon.metric.MeasurementUnit._
import kamon.metric._


object Metrics {

  /**
    * Metrics for Netty Event Loops:
    *
    *    - registered-channels:The number of registered Channels.
    *    - task-processing-time: The the number of nanoseconds the last processing of all tasks took.
    *    - task-queue-size: The number of tasks that are pending for processing.
    *    - task-waiting-time: The waiting time in the queue.
    */
  val registeredChannelsMetric = Kamon.minMaxCounter("netty.event-loop.registered-channels")
  val taskProcessingTimeMetric = Kamon.histogram("netty.event-loop.task-processing-time", time.nanoseconds)
  val taskQueueSizeMetric = Kamon.minMaxCounter("netty.event-loop.task-queue-size")
  val taskWaitingTimeMetric = Kamon.histogram("netty.event-loop.task-waiting-time", time.nanoseconds)


  def forEventLoop(name: String): EventLoopMetrics = {
    val eventLoopTags = Map("name" -> name)
    EventLoopMetrics(
      eventLoopTags,
      registeredChannelsMetric.refine(eventLoopTags),
      taskProcessingTimeMetric.refine(eventLoopTags),
      taskQueueSizeMetric.refine(eventLoopTags),
      taskWaitingTimeMetric.refine(eventLoopTags)
    )
  }

  case class EventLoopMetrics(tags: Map[String, String],
                              registeredChannels: MinMaxCounter,
                              taskProcessingTime: Histogram,
                              taskQueueSize: MinMaxCounter,
                              taskWaitingTime: Histogram) {

    def cleanup(): Unit = {
      registeredChannelsMetric.remove(tags)
      taskProcessingTimeMetric.remove(tags)
      taskQueueSizeMetric.remove(tags)
      taskWaitingTimeMetric.remove(tags)
    }
  }
}
