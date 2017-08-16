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
import kamon.context.Context
import kamon.netty.Clients.withNioClient
import kamon.netty.Servers.withNioServer
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span
import kamon.trace.Span.TagValue
import kamon.util.Registration
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._

class NettyServerTracingSpec extends WordSpec with Matchers with MetricInspection with Eventually
  with Reconfigure with BeforeAndAfterAll with OptionValues {

  "The Netty Server request span propagation" should {
    "propagate the span from the client to the server" in {
      withNioServer() { port =>
        withNioClient(port) { httpClient =>
          val clientSpan =  Kamon.buildSpan("test-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
            httpClient.execute(httpGet)

            eventually(timeout(2 seconds)) {
              val serverFinishedSpan = reporter.nextSpan().value
              val clientFinishedSpan = reporter.nextSpan().value

              serverFinishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))
            }
          }
        }
      }
    }

    "contain a span error when a internal server error(500) occurs" in {
      withNioServer() { port =>
        withNioClient(port) { httpClient =>
          val clientSpan =  Kamon.buildSpan("test-span-with-error").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val httpGet = httpClient.get(s"http://localhost:$port/error")
            httpClient.execute(httpGet)

            eventually(timeout(2 seconds)) {
              val serverFinishedSpan = reporter.nextSpan().value
              val clientFinishedSpan = reporter.nextSpan().value

              serverFinishedSpan.operationName shouldBe s"http://localhost:$port/error"
              serverFinishedSpan.tags should contain allOf("span.kind" -> TagValue.String("server"), "error" -> TagValue.String("true"))

              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))
              clientFinishedSpan.operationName shouldBe s"http://localhost:$port/error"
            }
          }
        }
      }
    }

    "propagate the span from the client to the server with chunk-encoded request" in {
      withNioServer() { port =>
        withNioClient(port) { httpClient =>
          val clientSpan = Kamon.buildSpan("client-chunk-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val (httpPost, chunks) = httpClient.postWithChunks(s"http://localhost:$port/fetch-in-chunks", "test 1", "test 2")
            httpClient.executeWithContent(httpPost, chunks)

            eventually(timeout(5 seconds)) {
              val serverFinishedSpan = reporter.nextSpan().value
              val clientFinishedSpan = reporter.nextSpan().value

              serverFinishedSpan.operationName shouldBe s"http://localhost:$port/fetch-in-chunks"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"http://localhost:$port/fetch-in-chunks"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan.context.parentID.string shouldBe clientSpan.context.spanID.string
              clientFinishedSpan.context.parentID.string shouldBe clientSpan.context.spanID.string

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "propagate the span from the client to the server with chunk-encoded response" in {
      withNioServer() { port =>
        withNioClient(port) { httpClient =>
          val clientSpan = Kamon.buildSpan("client-chunk-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val (httpPost, chunks) = httpClient.postWithChunks(s"http://localhost:$port/fetch-in-chunks", "test 1", "test 2")
            httpClient.executeWithContent(httpPost, chunks)

            eventually(timeout(2 seconds)) {
              val serverFinishedSpan = reporter.nextSpan().value
              val clientFinishedSpan = reporter.nextSpan().value

              serverFinishedSpan.operationName shouldBe s"http://localhost:$port/fetch-in-chunks"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"http://localhost:$port/fetch-in-chunks"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan.context.parentID.string shouldBe clientSpan.context.spanID.string
              clientFinishedSpan.context.parentID.string shouldBe clientSpan.context.spanID.string

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "create a new span when it's coming a request without one" in {
      withNioServer() { port =>
        withNioClient(port) { httpClient =>
          val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
          httpClient.execute(httpGet)

          eventually(timeout(2 seconds)) {
            val serverFinishedSpan = reporter.nextSpan().value

            serverFinishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
            serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

            serverFinishedSpan.context.parentID.string shouldBe ""

            reporter.nextSpan() shouldBe empty
          }
        }
      }
    }
  }

  @volatile var registration: Registration = _
  val reporter = new TestSpanReporter()

  override protected def beforeAll(): Unit = {
    enableFastSpanFlushing()
    sampleAlways()
    registration = Kamon.addReporter(reporter)
  }

  override protected def afterAll(): Unit = {
    registration.cancel()
  }
}
