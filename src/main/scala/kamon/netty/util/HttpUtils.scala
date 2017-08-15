package kamon.netty.util

import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse}
import kamon.context.TextMap

object HttpUtils {

  def textMapForHttpRequest(request: HttpRequest): TextMap = textMapForHeaders(request.headers())

  def textMapForHttpResponse(response: HttpResponse): TextMap = textMapForHeaders(response.headers())

  private def textMapForHeaders(headers: HttpHeaders): TextMap = new TextMap {
    import scala.collection.JavaConverters._

    override def values: Iterator[(String, String)] = headers.iterator().asScala.map(x => (x.getKey,x.getValue))
    override def get(key: String): Option[String] = Option(headers.get(key))
    override def put(key: String, value: String): Unit = headers.set(key, value)
  }
}
