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

import io.netty.channel.{ChannelHandlerContext, ChannelPromise}
import io.netty.handler.codec.http.{HttpRequest, LastHttpContent}
import kamon.Kamon
import kamon.trace.SpanContextCodec.Format
import kamon.trace.TextMap
import kamon.util.HasSpan
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class ChannelInstrumentation {

  @DeclareMixin("io.netty.channel.Channel+")
  def mixinHasSpanToChannel: HasSpan =  HasSpan.fromActiveSpan()


  @Before("execution(* io.netty.channel.ChannelHandlerContext+.fireChannelRead(..)) && args(request)")
  def onFireChannelRead(request: HttpRequest): Unit = {
    val incomingSpanContext = Kamon.extract(Format.HttpHeaders, readOnlyTextMapFromHttpRequest(request))
    Kamon.buildSpan("unknown-operation")
      .asChildOf(incomingSpanContext)
      .withSpanTag("span.king", "server")
      .start()
//      .start()
  }



  def readOnlyTextMapFromHttpRequest(request: HttpRequest): TextMap = new TextMap {
    import scala.collection.JavaConverters._

    override def values: Iterator[(String, String)] = request.headers.iterator().asScala.map(x => (x.getKey,x.getValue))
    override def get(key: String): Option[String] = None
    override def put(key: String, value: String): Unit = {}
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






  @After("execution(* io.netty.channel.ChannelHandlerContext+.fireChannelReadComplete(..)) && this(channelHandlerContext)")
  def onFireChannelReadComplete(channelHandlerContext:ChannelHandlerContext): Unit = {
//    if(channelHandlerContext.span != null) channelHandlerContext.span.star
      println("cacacacacaca1")
//    }
//    pjp.proceed()
  }

  @Before("execution(* io.netty.channel.ChannelOutboundHandler+.write(..)) && args(ctx, httpContent, *)")
  def onWrite(ctx: ChannelHandlerContext, httpContent: LastHttpContent): Unit = {
    val channel = ctx.channel().asInstanceOf[HasSpan]
    if(httpContent != null && channel.span != null) {
      channel.span.finish()
      println("puto")
    }
  }

//  @Pointcut(className = "io.netty.channel.ChannelOutboundHandler", methodName = "write",
//    methodParameterTypes = {"io.netty.channel.ChannelHandlerContext",
//      "java.lang.Object", "io.netty.channel.ChannelPromise"})
//  public static class OutboundAdvice {

  }
