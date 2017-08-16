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
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}
import kamon.Kamon
import kamon.netty.Netty
import kamon.trace._
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class HttpClientInstrumentation {

  @Pointcut("execution(* io.netty.handler.codec.http.HttpClientCodec.Decoder.decode(..))")
  def decoderPointcut():Unit = {}

  @Pointcut("execution(* io.netty.handler.codec.http.HttpClientCodec.Encoder.encode(..))")
  def encoderPointcut():Unit = {}

  @Around("encoderPointcut() && args(ctx, httpRequest, out)")
  def onEncodeRequest(pjp: ProceedingJoinPoint, ctx: ChannelHandlerContext, httpRequest: HttpRequest, out: util.List[AnyRef]): AnyRef = {
    val channel = ctx.channel().toContextAware()
    val currentSpan = channel.context.get(Span.ContextKey)

    if (currentSpan.isEmpty()) pjp.proceed()
    else {
      val operationName = Netty.generateHttpClientOperationName(httpRequest)
      val clientRequestSpan = Kamon.buildSpan(operationName)
        .asChildOf(currentSpan)
        .withSpanTag("span.kind", "client")
        .start()

      val textMap = Kamon.contextCodec.HttpHeaders.encode(channel.context)
      textMap.values.foreach { case (key, value) => httpRequest.headers().add(key, value) }

      channel.setContext(channel.context.withKey(Span.ContextKey, clientRequestSpan))

      pjp.proceed(Array(ctx, httpRequest, out))
    }
  }

  @After("decoderPointcut() && args(ctx, *, out)")
  def onDecodeResponse(ctx: ChannelHandlerContext, out: java.util.List[AnyRef]): Unit = {
    if (out.size() > 0 && out.get(0).isInstanceOf[HttpResponse]) {
      val span = ctx.channel().toContextAware().context.get(Span.ContextKey)
      span.finish()
    }
  }

  @AfterThrowing("decoderPointcut() && args(ctx, *, *)")
  def onDecodeError(ctx: ChannelHandlerContext): Unit = {
    val span = ctx.channel().toContextAware().context.get(Span.ContextKey)
    span.addSpanTag("error", "true").finish()
  }

}
