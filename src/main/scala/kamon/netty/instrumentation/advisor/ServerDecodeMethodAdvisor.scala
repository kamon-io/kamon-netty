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
package advisor

import io.netty.channel.ChannelHandlerContext
import kamon.Kamon
import kamon.netty.Netty
import kamon.netty.instrumentation.mixin.ChannelContextAware
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit}

/**
  * Advisor for io.netty.handler.codec.http.HttpObjectDecoder::decode
  */
class ServerDecodeMethodAdvisor
object ServerDecodeMethodAdvisor {

  @OnMethodExit
  def onExit(@Argument(0) ctx: AnyRef,
             @Argument(2) out: java.util.List[AnyRef]): Unit = {
    if (out.size() > 0 && out.get(0).isHttpRequest()) {
      val request = out.get(0).toHttpRequest()
      val channel = ctx.asInstanceOf[ChannelHandlerContext].channel().asInstanceOf[ChannelContextAware]//.toContextAware()
      val incomingContext = decodeContext(request)
      val serverSpan = Kamon.buildSpan(Netty.generateOperationName(request))
        .asChildOf(incomingContext.get(Span.ContextKey))
        .withFrom(channel.getStartTime)
        .withMetricTag("span.kind", "server")
        .withMetricTag("component", "netty")
        .withMetricTag("http.method", request.getMethod.name())
        .withTag("http.url", request.getUri)
        .start()

      channel.setContext(incomingContext.withKey(Span.ContextKey, serverSpan))
    }
  }
}
