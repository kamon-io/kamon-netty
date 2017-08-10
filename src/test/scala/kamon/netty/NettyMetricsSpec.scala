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

import kamon.netty.Metrics.{registeredChannelsMetric, taskProcessingTimeMetric, taskQueueSizeMetric}
import kamon.testkit.BaseSpec
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.scalatest.{Matchers, WordSpec}

class NettyMetricsSpec extends WordSpec with Matchers with BaseSpec  {

  "The NettyMetrics" should {

    "track the NioEventLoop in boss-group and worker-group" in {
      Servers.withNioServer() { port =>
        val httpClient = HttpClients.createDefault
        val httpGet = new HttpGet(s"http://localhost:$port/route?param=123")
        httpClient.execute(httpGet)

        registeredChannelsMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-nio-event-loop", "worker-group-nio-event-loop")
        taskProcessingTimeMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-nio-event-loop", "worker-group-nio-event-loop")
        taskQueueSizeMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-nio-event-loop", "worker-group-nio-event-loop")
      }
    }

    "track the EpollEventLoop in boss-group and worker-group" in {
      Servers.withEpollServer() { port =>
        val httpClient = HttpClients.createDefault
        val httpGet = new HttpGet(s"http://localhost:$port/route?param=123")
        httpClient.execute(httpGet)

        registeredChannelsMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-epoll-event-loop", "worker-group-epoll-event-loop")
        taskProcessingTimeMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-epoll-event-loop", "worker-group-epoll-event-loop")
        taskQueueSizeMetric.valuesForTag("name") should contain atLeastOneOf("boss-group-epoll-event-loop", "worker-group-epoll-event-loop")
      }
    }

    "track the registeredChannels, taskProcessingTime and taskQueueSize for NioEventLoop" in {
      Servers.withNioServer() { port =>
        val httpClient = HttpClients.createDefault
        val httpGet = new HttpGet(s"http://localhost:$port/route?param=123")

        for (_ <- 1 to 10) {
          val response = httpClient.execute(httpGet)
          response.getStatusLine.getStatusCode should be(200)
          response.close()
        }

        registeredChannelsMetric.valuesForTag("name") should contain("boss-group-nio-event-loop")

        val metrics = Metrics.forEventLoop("boss-group-nio-event-loop")

        metrics.registeredChannels.value() should be > 0L
        metrics.taskProcessingTime.distribution().max should be > 0L
        metrics.taskQueueSize.distribution().max should be > 0L
      }
    }

    "track the registeredChannels, taskProcessingTime and taskQueueSize for EpollEventLoop" in {
      Servers.withNioServer() { port =>
        val httpClient = HttpClients.createDefault
        val httpGet = new HttpGet(s"http://localhost:$port/route?param=123")

        for (_ <- 1 to 10) {
          val response = httpClient.execute(httpGet)
          response.getStatusLine.getStatusCode should be(200)
          response.close()
        }

        registeredChannelsMetric.valuesForTag("name") should contain("boss-group-epoll-event-loop")

        val metrics = Metrics.forEventLoop("boss-group-epoll-event-loop")

        metrics.registeredChannels.value() should be > 0L
        metrics.taskProcessingTime.distribution().max should be > 0L
        metrics.taskQueueSize.distribution().max should be > 0L
      }
    }
  }
}


