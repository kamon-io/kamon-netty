package kamon.netty.instrumentation

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpRequest
import kamon.Kamon
import kamon.netty.Netty
import kamon.trace.Span
import kamon.util.Clock

object HttpRequestContext {

  def withContext(request: HttpRequest, ctx: ChannelHandlerContext): HttpRequest = {
    val currentContext = request.getContext()
    val clientSpan = currentContext.get(Span.ContextKey)
    if (clientSpan.nonEmpty()) {
      val clientRequestSpan = Kamon.buildSpan(Netty.generateHttpClientOperationName(request))
        .asChildOf(clientSpan)
        .withSpanTag("span.kind", "client")
        .withSpanTag("component", "netty")
        .withSpanTag("http.method", request.getMethod.name())
        .withSpanTag("http.url", request.getUri)
        .start()

      val newContext = currentContext.withKey(Span.ContextKey, clientRequestSpan)

      ctx.channel().toContextAware().setContext(newContext)

      encodeContext(newContext, request)
    } else request
  }

}


object KamonHandlerPortable {

  @ChannelHandler.Sharable
  class KamonHandler extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
      ctx.channel().toContextAware().startTime = Clock.microTimestamp()
      super.channelRead(ctx, msg)
    }
  }
}
