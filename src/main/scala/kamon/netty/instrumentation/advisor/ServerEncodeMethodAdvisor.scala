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
import kamon.trace.Span

/**
  * Advisor for io.netty.handler.codec.http.HttpObjectEncoder::encode
  */
class ServerEncodeMethodAdvisor
object ServerEncodeMethodAdvisor {
  import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter}

  @OnMethodEnter
  def onEnter(@Argument(0) ctx: ChannelHandlerContext,
              @Argument(1) response: AnyRef): Unit = {
    if (response.isHttpResponse()) {
      val serverSpan = ctx.channel().getContext().get(Span.ContextKey)
      if(isError(response.toHttpResponse().getStatus.code()))
        serverSpan.addError("error-status-response")
      serverSpan.finish()
    }
  }
}
