package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import kamon.trace.Span

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
