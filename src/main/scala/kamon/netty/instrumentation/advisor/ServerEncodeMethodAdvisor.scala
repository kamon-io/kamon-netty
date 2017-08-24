package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.concurrent.EventExecutor
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodEnter, This}
import kamon.trace.Span

class ServerEncodeMethodAdvisor
object ServerEncodeMethodAdvisor {

  @OnMethodEnter
  def onEnter(@This eventLoop: EventExecutor,
             @Argument(0) ctx: AnyRef,
             @Argument(1) response: HttpResponse): Unit = {
    val serverSpan = ctx.asInstanceOf[ChannelHandlerContext].channel().getContext().get(Span.ContextKey)
    if(isError(response.getStatus.code()))
      serverSpan.addSpanTag("error", value = true)
    serverSpan.finish()
  }

}
