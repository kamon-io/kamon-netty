package kamon.netty

import kamon.Kamon
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span.TagValue
import kamon.util.Registration
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._
import org.scalatest._

class NettyServerTracingSpec extends WordSpec with Matchers with MetricInspection with Eventually
  with Reconfigure with BeforeAndAfterAll with OptionValues {

  "The Netty Server request span propagation" should {
    "propagate the span from the client to the server" in {
      Servers.withNioServer() { port =>
        Clients.withNioClient(port) { httpClient =>
          val testSpan =  Kamon.buildSpan("undefined-span").start()
          Kamon.withActiveSpan(testSpan) {
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
