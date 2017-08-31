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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponse
import kamon.Kamon
import kamon.netty.Netty
import kamon.trace.Span
import org.aspectj.lang.annotation.{After, Aspect, Before}

@Aspect
class HttpServerInstrumentation {

  @After("execution(* io.netty.handler.codec.http.HttpObjectDecoder+.decode(..)) && args(ctx, *, out)")
  def onDecodeRequest(ctx: ChannelHandlerContext,  out:java.util.List[AnyRef]): Unit = {
    if (out.size() > 0 && out.get(0).isHttpRequest()) {
      val request = out.get(0).toHttpRequest()
      val channel = ctx.channel().toContextAware()
      val incomingContext = decodeContext(request)
      val serverSpan = Kamon.buildSpan(Netty.generateOperationName(request))
        .asChildOf(incomingContext.get(Span.ContextKey))
        .withStartTimestamp(channel.startTime)
        .withTag("span.kind", "server")
        .withTag("component", "netty")
        .withTag("http.method", request.getMethod.name())
        .withTag("http.url", request.getUri)
        .start()

      channel.setContext(incomingContext.withKey(Span.ContextKey, serverSpan))
    }
  }

  @Before("execution(* io.netty.handler.codec.http.HttpObjectEncoder+.encode(..)) && args(ctx, response, *)")
  def onEncodeResponse(ctx: ChannelHandlerContext, response:HttpResponse): Unit = {
    val serverSpan = ctx.channel().getContext().get(Span.ContextKey)
    if(isError(response.getStatus.code()))
      serverSpan.addTag("error", value = true)
    serverSpan.finish()
  }
}

