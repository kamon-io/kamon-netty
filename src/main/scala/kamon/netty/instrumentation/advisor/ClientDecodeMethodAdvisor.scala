package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, Thrown}
import kamon.trace.Span

class ClientDecodeMethodAdvisor
object ClientDecodeMethodAdvisor {

  @OnMethodExit
  def onExit(@Argument(0) _ctx: AnyRef, @Argument(2) out: java.util.List[AnyRef], @Thrown failure: Throwable): Unit = {
    val ctx = _ctx.asInstanceOf[ChannelHandlerContext]
    if (failure != null) {
      val clientSpan = ctx.channel().getContext().get(Span.ContextKey)
      clientSpan.addSpanTag("error", value = true).finish()
    } else if (out.size() > 0 && out.get(0).isHttpResponse()) {
      val clientSpan = ctx.channel().getContext().get(Span.ContextKey)
      clientSpan.finish()
    }
  }

}
