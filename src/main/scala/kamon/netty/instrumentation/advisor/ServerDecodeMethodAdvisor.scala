package kamon.netty.instrumentation
package advisor

import io.netty.channel.ChannelHandlerContext
import io.netty.util.concurrent.EventExecutor
import kamon.Kamon
import kamon.netty.Netty
import kamon.netty.instrumentation.mixin.ChannelContextAware
import kamon.trace.Span
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, OnMethodExit, This}

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
        .withTag("span.kind", "server")
        .withTag("component", "netty")
        .withTag("http.method", request.getMethod.name())
        .withTag("http.url", request.getUri)
        .start()

      channel.setContext(incomingContext.withKey(Span.ContextKey, serverSpan))
    }
  }

}
