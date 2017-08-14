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

package kamon.netty.instrumentation

import java.util
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.{DefaultHttpResponse, HttpRequest}
import kamon.Kamon
import kamon.netty.Netty
import kamon.trace.SpanContextCodec.Format
import kamon.trace._
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

import scala.collection.mutable

@Aspect
class HttpClientInstrumentation {

  @Pointcut("execution(* io.netty.handler.codec.http.HttpClientCodec.Decoder.decode(..))")
  def decoderPointcut():Unit = {}

  @Pointcut("execution(* io.netty.handler.codec.http.HttpClientCodec.Encoder.encode(..))")
  def encoderPointcut():Unit = {}

  @Around("encoderPointcut() && args(ctx, httpRequest, out)")
  def onEncodeRequest(pjp: ProceedingJoinPoint, ctx: ChannelHandlerContext, httpRequest: HttpRequest, out: util.List[AnyRef]): AnyRef = {
    val activeSpan = Kamon.activeSpan()

    if (activeSpan.isInstanceOf[Span.Empty]) {
      pjp.proceed()
    } else {
      val operationName = Netty.generateHttpClientOperationName(httpRequest)
      val clientRequestSpan = Kamon.buildSpan(operationName).asChildOf(activeSpan.context()).start()
      clientRequestSpan.addSpanTag("span.kind", "client")

      val maybeHeaders = mutable.Map.empty[String, String]
      Kamon.inject(clientRequestSpan.context(), Format.HttpHeaders, writeOnlyTextMapFromMap(maybeHeaders))
      maybeHeaders.foreach { case (key, value) => httpRequest.headers().add(key, value) }

      ctx.channel().asInstanceOf[ChannelSpanAware].span = clientRequestSpan

      pjp.proceed(Array(ctx, httpRequest, out))
    }
  }

  @After("decoderPointcut() && args(ctx, *, out)")
  def onDecodeRequest(ctx: ChannelHandlerContext, out: java.util.List[Object]): Unit = {
    val  response = out.get(0)
    if(response.isInstanceOf[DefaultHttpResponse]) {
      val span = ctx.channel().asInstanceOf[ChannelSpanAware].span
      span.finish()
    }
  }

  @AfterThrowing("decoderPointcut() && args(ctx, *, *)")
  def onDecodeError(ctx: ChannelHandlerContext): Unit = {
    val span = ctx.channel().asInstanceOf[ChannelSpanAware].span
    span.addSpanTag("error", "true").finish()
  }

  def writeOnlyTextMapFromMap(map: scala.collection.mutable.Map[String, String]): TextMap = new TextMap {
    override def put(key: String, value: String): Unit =
      map.put(key, value)

    override def get(key: String): Option[String] =
      map.get(key)

    override def values: Iterator[(String, String)] =
      map.toIterator
  }
}
