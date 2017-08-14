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
import kamon.netty.Netty
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
  def onFireChannelRead(ctx:ChannelSpanAware, request: HttpRequest): Unit =
      ctx._startTime = Clock.microTimestamp()

  @After("execution(* io.netty.handler.codec.http.HttpServerCodec.HttpServerRequestDecoder.decode(..)) && args(ctx, *, out)")
  def onDecodeRequest(ctx: ChannelHandlerContext,  out:java.util.List[Object]): Unit = {
    if(out.size() > 0 && out.get(0).isInstanceOf[HttpRequest]) {
      val request = out.get(0).asInstanceOf[HttpRequest]
      val incomingSpanContext = Kamon.extract(Format.HttpHeaders, HttpUtils.textMapForHttpRequest(request))

      val span = Kamon.buildSpan(Netty.generateOperationName(request))
        .asChildOf(incomingSpanContext)
        .withSpanTag("span.kind", "server")
        .withStartTimestamp(ctx.channel().asInstanceOf[ChannelSpanAware]._startTime)
        .start()

      ctx.channel().asInstanceOf[ChannelSpanAware].span = span
    }
  }

  @Before("execution(* io.netty.handler.codec.http.HttpObjectEncoder+.encode(..)) && args(ctx, response, *)")
  def onEncodeResponse(ctx: ChannelHandlerContext, response:HttpResponse): Unit = {
    val span = ctx.channel().asInstanceOf[ChannelSpanAware].span

    if(isError(response.getStatus.code())) {
      span.addSpanTag("error", "true")
    }
    span.finish()
  }

  private def isError(statusCode: Int): Boolean =
    statusCode >= 500 && statusCode < 600
}


