package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, Thrown}

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
