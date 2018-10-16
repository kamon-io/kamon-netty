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

import io.netty.buffer.PoolThreadCache
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}
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

class NettyHTTPTracingSpec extends WordSpec with Matchers with MetricInspection with Eventually
  with Reconfigure with BeforeAndAfterAll with OptionValues {

  private val logger = InternalLoggerFactory.getInstance(this.getClass)
  var count = 0
  "The Netty HTTP span propagation" should {
    "propagate the span from the client to the server" in {
      withNioServer(9001) { port =>
        withNioClient(port) { httpClient =>
          val clientSpan =  Kamon.buildSpan("test-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
            httpClient.execute(httpGet)

            eventually(timeout(5 seconds)) {
              count += 1
              logger.info(s"************************************* [$count] |_ ZERO Span")
              val serverFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |. First Span - server | ${logFinishedSpan(serverFinishedSpan)}")
              val clientFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |. Second Span - client | ${logFinishedSpan(clientFinishedSpan)}")

              serverFinishedSpan.operationName shouldBe "route.get"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"localhost:$port/route"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan.context.traceID shouldBe clientFinishedSpan.context.traceID
              serverFinishedSpan.context.parentID shouldBe clientFinishedSpan.context.spanID

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "contain a span error when an internal server error(500) occurs" in {
      withNioServer(9002) { port =>
        withNioClient(port) { httpClient =>
          val clientSpan =  Kamon.buildSpan("test-span-with-error").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val httpGet = httpClient.get(s"http://localhost:$port/error")
            httpClient.execute(httpGet)


            eventually(timeout(5 seconds)) {
              count += 1
              logger.info(s"************************************* [$count] |_ ZERO Span")
              val serverFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |- First Span - server | ${logFinishedSpan(serverFinishedSpan)}")
              val clientFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |- Second Span - client | ${logFinishedSpan(clientFinishedSpan)}")

              serverFinishedSpan.operationName shouldBe "error.get"
              serverFinishedSpan.tags should contain allOf("span.kind" -> TagValue.String("server"), "error" -> TagValue.True)

              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))
              clientFinishedSpan.operationName shouldBe s"localhost:$port/error"

              serverFinishedSpan.context.parentID shouldBe clientFinishedSpan.context.spanID
              clientFinishedSpan.context.parentID shouldBe clientSpan.context.spanID

              serverFinishedSpan.context.traceID shouldBe clientFinishedSpan.context.traceID
              serverFinishedSpan.context.parentID shouldBe clientFinishedSpan.context.spanID

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "propagate the span from the client to the server with chunk-encoded request" in {
      withNioServer(9003) { port =>
        withNioClient(port) { httpClient =>
          val clientSpan = Kamon.buildSpan("client-chunk-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val (httpPost, chunks) = httpClient.postWithChunks(s"http://localhost:$port/fetch-in-chunks-request", "test 1", "test 2")
            httpClient.executeWithContent(httpPost, chunks)

            eventually(timeout(5 seconds)) {
              count += 1
              logger.info(s"************************************* [$count] |_ ZERO Span")
              val serverFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] || First Span - server | ${logFinishedSpan(serverFinishedSpan)}")
              val clientFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] || Second Span - client | ${logFinishedSpan(clientFinishedSpan)}")

              serverFinishedSpan.operationName shouldBe "fetch-in-chunks-request.post"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"localhost:$port/fetch-in-chunks-request"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan.context.parentID shouldBe clientFinishedSpan.context.spanID
              clientFinishedSpan.context.parentID shouldBe clientSpan.context.spanID

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "propagate the span from the client to the server with chunk-encoded response" in {
      withNioServer(9004) { port =>
        withNioClient(port) { httpClient =>
          val clientSpan = Kamon.buildSpan("client-chunk-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            val (httpPost, chunks) = httpClient.postWithChunks(s"http://localhost:$port/fetch-in-chunks-response", "test 1", "test 2")
            httpClient.executeWithContent(httpPost, chunks)

            eventually(timeout(5 seconds)) {
              count += 1
              logger.info(s"************************************* [$count] |_ ZERO Span")
              val serverFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |* First Span - server | ${logFinishedSpan(serverFinishedSpan)}")
              val clientFinishedSpan = reporter.nextSpan().value
              logger.info(s"************************************* [$count] |* Second Span - client | ${logFinishedSpan(clientFinishedSpan)}")

              serverFinishedSpan.operationName shouldBe "fetch-in-chunks-response.post"
              serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"localhost:$port/fetch-in-chunks-response"
              clientFinishedSpan.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan.context.parentID shouldBe clientFinishedSpan.context.spanID
              clientFinishedSpan.context.parentID shouldBe clientSpan.context.spanID

              reporter.nextSpan() shouldBe empty
            }
          }
        }
      }
    }

    "create a new span when it's coming a request without one" in {
      withNioServer(9005) { port =>
        withNioClient(port) { httpClient =>
          val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
          httpClient.execute(httpGet)

          eventually(timeout(5 seconds)) {
            count += 1
            logger.info(s"************************************* [$count] |; ZERO Span")
            val serverFinishedSpan = reporter.nextSpan().value
            logger.info(s"************************************* [$count] |; First Span - server | ${logFinishedSpan(serverFinishedSpan)}")

            serverFinishedSpan.operationName shouldBe "route.get"
            serverFinishedSpan.tags should contain ("span.kind" -> TagValue.String("server"))

            serverFinishedSpan.context.parentID.string shouldBe ""

            reporter.nextSpan() shouldBe empty
          }
        }
      }
    }

    "create a new span for each request" in {
      withNioServer(9006) { port =>
        withNioClient(port) { httpClient =>
          val clientSpan =  Kamon.buildSpan("test-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, clientSpan)) {
            httpClient.execute(httpClient.get(s"http://localhost:$port/route?param=123"))
            httpClient.execute(httpClient.get(s"http://localhost:$port/route?param=123"))

            eventually(timeout(5 seconds)) {
              count +=1
              logger.info(s"************************************* [$count] |_ ZERO Span")
              val serverFinishedSpan1 = reporter.nextSpan().value
              logger.info(s"************************************* [$count] | First Span - server | ${logFinishedSpan(serverFinishedSpan1)}")
              val clientFinishedSpan1 = reporter.nextSpan().value
              logger.info(s"************************************* [$count] | First Span - client | ${logFinishedSpan(clientFinishedSpan1)}")
              val serverFinishedSpan2 = reporter.nextSpan().value
              logger.info(s"************************************* [$count] | Second Span - server | ${logFinishedSpan(serverFinishedSpan2)}")
              val clientFinishedSpan2 = reporter.nextSpan().value
              logger.info(s"************************************* [$count] | Second Span - client | ${logFinishedSpan(clientFinishedSpan2)}")

              serverFinishedSpan1.operationName shouldBe "route.get"
              serverFinishedSpan1.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan1.operationName shouldBe s"localhost:$port/route"
              clientFinishedSpan1.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan1.context.traceID shouldBe clientFinishedSpan1.context.traceID
              serverFinishedSpan1.context.parentID shouldBe clientFinishedSpan1.context.spanID

              serverFinishedSpan2.operationName shouldBe "route.get"
              serverFinishedSpan2.tags should contain ("span.kind" -> TagValue.String("server"))

              clientFinishedSpan2.operationName shouldBe s"localhost:$port/route"
              clientFinishedSpan2.tags should contain ("span.kind" -> TagValue.String("client"))

              serverFinishedSpan2.context.traceID shouldBe clientFinishedSpan2.context.traceID
              serverFinishedSpan2.context.parentID shouldBe clientFinishedSpan2.context.spanID

              clientFinishedSpan1.context.parentID shouldBe clientFinishedSpan2.context.parentID

              clientFinishedSpan1.context.parentID shouldBe clientSpan.context.spanID

              reporter.nextSpan() shouldBe empty
            }
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

  def logFinishedSpan(span: Span.FinishedSpan): String = {
    s"[OperationName=${span.operationName}][Kind=${span.tags.get("span.kind")}][parentID=${span.context.parentID}]"
  }
}
