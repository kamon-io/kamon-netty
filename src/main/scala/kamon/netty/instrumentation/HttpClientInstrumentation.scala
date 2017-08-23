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
import io.netty.handler.codec.http.HttpRequest
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

  @Around("encoderPointcut() && args(ctx, request, out)")
  def onEncodeRequest(pjp: ProceedingJoinPoint, ctx: ChannelHandlerContext, request: HttpRequest, out: util.List[AnyRef]): AnyRef = {
    val currentContext = request.getContext()
    val clientSpan = currentContext.get(Span.ContextKey)

    if (clientSpan.isEmpty()) pjp.proceed()
    else {
      val clientRequestSpan = Kamon.buildSpan(Netty.generateHttpClientOperationName(request))
        .asChildOf(clientSpan)
        .withSpanTag("span.kind", "client")
        .withSpanTag("component", "netty")
        .withSpanTag("http.method", request.getMethod.name())
        .withSpanTag("http.url", request.getUri)
        .start()

      val newContext = currentContext.withKey(Span.ContextKey, clientRequestSpan)

      ctx.channel().toContextAware().setContext(newContext)

      pjp.proceed(Array(ctx, encodeContext(newContext, request), out))
    }
  }

  @After("decoderPointcut() && args(ctx, *, out)")
  def onDecodeResponse(ctx: ChannelHandlerContext, out: java.util.List[AnyRef]): Unit = {
    if (out.size() > 0 && out.get(0).isHttpResponse()) {
      val clientSpan = ctx.channel().getContext().get(Span.ContextKey)
      clientSpan.finish()
    }
  }

  @AfterThrowing("decoderPointcut() && args(ctx, *, *)")
  def onDecodeError(ctx: ChannelHandlerContext): Unit = {
    val clientSpan = ctx.channel().getContext().get(Span.ContextKey)
    clientSpan.addSpanTag("error", value = true).finish()
  }
}
