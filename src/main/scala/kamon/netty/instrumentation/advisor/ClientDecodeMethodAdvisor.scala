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
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, Thrown}

/**
  * Advisor for io.netty.handler.codec.http.HttpClientCodec.Decoder::decode
  */
class ClientDecodeMethodAdvisor
object ClientDecodeMethodAdvisor {

  @OnMethodExit(onThrowable = classOf[Throwable])
  def onExit(@Argument(0) _ctx: AnyRef, @Argument(2) out: java.util.List[AnyRef],
             @Thrown failure: Throwable): Unit = {
    val ctx = _ctx.asInstanceOf[ChannelHandlerContext]
    if (failure != null) {
      val clientSpan = ctx.channel().getContext().get(Span.ContextKey)
      clientSpan.addError(failure.getMessage, failure).finish()
      throw failure
    } else if (out.size() > 0 && out.get(0).isHttpResponse()) {
      ctx.channel().getContext().get(Span.ContextKey).finish()
    }
  }

}
