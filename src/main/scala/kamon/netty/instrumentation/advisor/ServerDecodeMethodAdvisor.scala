package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import io.netty.util.concurrent.EventExecutor
import kamon.Kamon
import kamon.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, This}
import kamon.netty.Netty
import kamon.netty.instrumentation.mixin.ChannelContextAware
import kamon.trace.Span

class ServerDecodeMethodAdvisor
object ServerDecodeMethodAdvisor {

  @OnMethodExit
  def onExit(@This eventLoop: EventExecutor,
             @Argument(0) ctx: AnyRef,
             @Argument(2) out: java.util.List[AnyRef]): Unit = {
    if (out.size() > 0 && out.get(0).isHttpRequest()) {
      val request = out.get(0).toHttpRequest()
      val channel = ctx.asInstanceOf[ChannelHandlerContext].channel().asInstanceOf[ChannelContextAware]//.toContextAware()
      val incomingContext = decodeContext(request)
      val serverSpan = Kamon.buildSpan(Netty.generateOperationName(request))
        .asChildOf(incomingContext.get(Span.ContextKey))
        .withStartTimestamp(channel.startTime)
        .withSpanTag("span.kind", "server")
        .withSpanTag("component", "netty")
        .withSpanTag("http.method", request.getMethod.name())
        .withSpanTag("http.url", request.getUri)
        .start()

      channel.setContext(incomingContext.withKey(Span.ContextKey, serverSpan))
    }
  }

}
