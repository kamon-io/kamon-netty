/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpRequest
import kamon.Kamon
import kamon.netty.Netty
import kamon.trace.Span

object HttpRequestContext {

  def withContext(request: HttpRequest, ctx: ChannelHandlerContext): HttpRequest = {
    val currentContext = request.getContext()
    val clientSpan = currentContext.get(Span.ContextKey)
    if (clientSpan.nonEmpty()) {
      val clientRequestSpan = Kamon.buildSpan(Netty.generateHttpClientOperationName(request))
        .asChildOf(clientSpan)
        .withMetricTag("span.kind", "client")
        .withMetricTag("component", "netty")
        .withMetricTag("http.method", request.getMethod.name())
        .withTag("http.url", request.getUri)
        .start()

      val newContext = currentContext.withKey(Span.ContextKey, clientRequestSpan)

      ctx.channel().toContextAware().setContext(newContext)

      encodeContext(newContext, request)
    } else request
  }
}

object KamonHandlerPortable {

  @ChannelHandler.Sharable
  class KamonHandler extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
      ctx.channel().toContextAware().setStartTime(Kamon.clock().instant())
      super.channelRead(ctx, msg)
    }
  }
}
