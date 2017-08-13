package kamon.netty

import kamon.Kamon
import kamon.testkit.BaseSpec
import kamon.trace.SpanContextCodec.ExtendedB3
import org.scalatest.{Matchers, WordSpec}

class NettyServerTracingSpec extends WordSpec with Matchers with BaseSpec {
  import ExtendedB3.Headers._
  "The Netty Server request span propagation" should {
    "Decode a HTTP Span" in {
      Servers.withNioServer() { port =>
        Clients.withNioClient(port) { httpClient =>
          val testSpan =  Kamon.buildSpan("puto").start()
          Kamon.withActiveSpan(testSpan) {
            val httpGet = httpClient.get(s"http://localhost:$port/route?param=123")
            httpGet.headers().add(SpanIdentifier, "111")
            httpGet.headers().add(TraceIdentifier, "222")

            val response = httpClient.execute(httpGet)
            println(response)
          }

//          response.headers().get(ParentSpanIdentifier) should be("111")
//          response.headers().get(SpanIdentifier) should not be empty
//          response.headers().get(TraceIdentifier) should be("222")
//          response.headers().get(Baggage) should not be empty
        }
      }
    }
  }
}
