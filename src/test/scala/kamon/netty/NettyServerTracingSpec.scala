package kamon.netty

import kamon.Kamon
import kamon.context.Context
import kamon.testkit.{MetricInspection, Reconfigure, SpanInspector, TestSpanReporter}
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
      Servers.withNioServer() { port =>
        Clients.withNioClient(port) { httpClient =>
          val testSpan = Kamon.buildSpan("test-span").start()
          Kamon.withContext(Context.create(Span.ContextKey, testSpan)) {
            val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
            httpClient.execute(httpGet)

            eventually(timeout(2 seconds)) {
              val serverFinishedSpan = reporter.nextSpan().value
              val clientFinishedSpan = reporter.nextSpan().value

              serverFinishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
              serverFinishedSpan.tags should contain("span.kind" -> TagValue.String("server"))

              clientFinishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
              clientFinishedSpan.tags should contain("span.kind" -> TagValue.String("client"))
            }
          }
        }
      }
    }

    "contain a span error when a Internal Server Error(500) occurs" in {
      Servers.withNioServer() { port =>
        Clients.withNioClient(port) { httpClient =>
          val testSpan =  Kamon.buildSpan("test-span-with-error").start()
          Kamon.withContext(Context.create(Span.ContextKey, testSpan)) {
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

  def inspect(span: Span): SpanInspector =
    SpanInspector(span)
}
