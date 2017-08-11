package kamon.netty

import kamon.testkit.BaseSpec
import kamon.trace.SpanContextCodec.ExtendedB3
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.scalatest.{Matchers, WordSpec}

class NettyServerTracingSpec extends WordSpec with Matchers with BaseSpec {
  import ExtendedB3.Headers._
  "The Netty Server request span propagation" should {
    "Decode a HTTP Span" in {
      Servers.withNioServer() { port =>
        val httpClient = HttpClients.createDefault
        val httpGet = new HttpGet(s"http://localhost:$port/route?param=123")
        httpGet.addHeader(SpanIdentifier, "111")
        httpGet.addHeader(TraceIdentifier, "222")
        val response = httpClient.execute(httpGet)

        response.close()
        response.getHeaders(ParentSpanIdentifier).toList.map(_.getValue) should contain("111")
        response.getHeaders(SpanIdentifier).toList.map(_.getValue) should not be empty
        response.getHeaders(TraceIdentifier).toList.map(_.getValue) should contain("222")
        response.getHeaders(Baggage).toList.map(_.getValue) should not be empty
      }
    }
  }
}
