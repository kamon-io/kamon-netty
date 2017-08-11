/*
 * =========================================================================================
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}
import kamon.Kamon
import kamon.netty.util.HttpUtils
import kamon.trace.Span
import kamon.trace.SpanContextCodec.Format
import kamon.util.Clock
import org.aspectj.lang.annotation._


trait ChannelSpanAware {
  @volatile var _startTime: Long = 0
  @volatile var  span: Span = _
}

@Aspect
class ChannelInstrumentation {

  @DeclareMixin("io.netty.channel.Channel+")
  def mixinHasSpanToTimeAware: ChannelSpanAware =  new ChannelSpanAware{}


  @Before("execution(* io.netty.channel.ChannelHandlerContext+.fireChannelRead(..)) && this(ctx) && args(request)")
  def onFireChannelRead(ctx:ChannelSpanAware, request: HttpRequest): Unit = {
      ctx._startTime = Clock.microTimestamp()
  }

//  @After("execution(* io.netty.handler.codec.http.HttpObjectDecoder.decode(..)) && args(ctx, *, out)")
  @After("execution(* io.netty.handler.codec.http.HttpServerCodec.HttpServerRequestDecoder.decode(..)) && args(ctx, *, out)")
    def onDecodeRequest(ctx: ChannelHandlerContext,  out:java.util.List[Object]): Unit = {
      if(out.size() > 0 && out.get(0).isInstanceOf[HttpRequest]) {
        val httpRequest = out.get(0).asInstanceOf[HttpRequest]
        val incomingSpanContext = Kamon.extract(Format.HttpHeaders, HttpUtils.textMapForHttpRequest(httpRequest))

        val span = Kamon.buildSpan(httpRequest.getUri)
          .asChildOf(incomingSpanContext)
          .withSpanTag("span.kind", "server")
          .withStartTimestamp(ctx.channel().asInstanceOf[ChannelSpanAware]._startTime)
          .start()

        span.context().baggage.add("request-uri", httpRequest.getUri)
        ctx.channel().asInstanceOf[ChannelSpanAware].span = span
//        println("ON Decode SpanID =>"  + span.context().spanID + " TraceId => "+ span.context().traceID)
      }
    }

  @Before("execution(* io.netty.handler.codec.http.HttpObjectEncoder+.encode(..)) && args(ctx, msg, *)")
  def onEncodeResponse(ctx: ChannelHandlerContext,  msg:HttpResponse): Unit = {
    val hasSpan = ctx.channel().asInstanceOf[ChannelSpanAware]
    hasSpan.span.finish()
//    println("ON Encode SpanID =>"  + hasSpan.span.context().spanID + " TraceId => "+ hasSpan.span.context().traceID + "name: =>" + hasSpan.span.context().baggage.get("request-uri"))
  }

//  val incomingSpanContext = Kamon.extract(HTTP_HEADERS, readOnlyTextMapFromHttpRequest(request))
//  val span = Kamon.buildSpan("unknown-operation")
//    .asChildOf(incomingSpanContext)
//    .withTag("span.kind", "server")
//    .startActive()
//  val continuation = span.capture()
//
//  val responseFuture = pjp.proceed().asInstanceOf[Future[HttpResponse]]
//  span.deactivate()
//
//  responseFuture.transform(
//    s = response => {
//      val requestSpan = continuation.activate()
//      if(isError(response.getStatus.code())) {
//        requestSpan.setTag("error", "true")
//      }
//
//      requestSpan.deactivate()
//      response
//    },
//
//    f = error => {
//      val requestSpan = continuation.activate()
//      requestSpan.setTag("error", "true")
//      requestSpan.deactivate()
//      error
//    }
//  )(CallingThreadExecutionContext)






//  @After("execution(* io.netty.channel.ChannelHandlerContext+.fireChannelReadComplete(..)) && this(channelHandlerContext)")
//  def onFireChannelReadComplete(channelHandlerContext:ChannelHandlerContext): Unit = {
////    if(channelHandlerContext.span != null) channelHandlerContext.span.star
//      println("cacacacacaca1")
////    }
////    pjp.proceed()
//  }
//
//  @Before("execution(* io.netty.channel.ChannelOutboundHandler+.write(..)) && args(ctx, httpContent, *)")
//  def onWrite(ctx: ChannelHandlerContext, httpContent: LastHttpContent): Unit = {
//    val channel = ctx.channel().asInstanceOf[HasSpan]
//    if(httpContent != null && channel.span != null) {
//      channel.span.finish()
//      println("puto")
//    }
//  }

//  @Pointcut(className = "io.netty.channel.ChannelOutboundHandler", methodName = "write",
//    methodParameterTypes = {"io.netty.channel.ChannelHandlerContext",
//      "java.lang.Object", "io.netty.channel.ChannelPromise"})
//  public static class OutboundAdvice {

  }


