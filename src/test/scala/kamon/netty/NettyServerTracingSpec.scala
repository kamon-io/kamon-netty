package kamon.netty

import kamon.Kamon
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.util.Registration
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar._
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

class NettyServerTracingSpec extends WordSpec with Matchers with MetricInspection with Eventually
  with Reconfigure with BeforeAndAfterAll with OptionValues {

  "The Netty Server request span propagation" should {
    "Decode a HTTP Span" in {
      Servers.withNioServer() { port =>
        Clients.withNioClient(port) { httpClient =>
          val testSpan =  Kamon.buildSpan("puto").start()
          Kamon.withActiveSpan(testSpan) {
            val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
            httpClient.execute(httpGet)

            eventually(timeout(2 seconds)) {
              val finishedSpan = reporter.nextSpan().value
              finishedSpan.operationName shouldBe s"http://localhost:$port/route?param=123"
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
