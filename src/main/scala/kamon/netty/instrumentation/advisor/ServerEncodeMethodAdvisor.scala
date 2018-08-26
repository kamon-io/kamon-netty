package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.concurrent.EventExecutor
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter, This}

class ServerEncodeMethodAdvisor
object ServerEncodeMethodAdvisor {

  @OnMethodEnter
  def onEnter(@Argument(0) ctx: ChannelHandlerContext,
              @Argument(1) response: AnyRef): Unit = {
    val serverSpan = ctx.channel().getContext().get(Span.ContextKey)
    // FIXME
//    if(isError(response.getStatus.code()))
//      serverSpan.addError("error-status-response")
    serverSpan.finish()
  }

}
