package kamon.netty.instrumentation

import kamon.netty.instrumentation.mixin.RequestContextAwareMixin
import kanela.agent.scala.KanelaInstrumentation

class HttpMessageInstrumentation extends KanelaInstrumentation {

  forSubtypeOf("io.netty.handler.codec.http.HttpMessage") { builder ⇒
    builder
      .withMixin(classOf[RequestContextAwareMixin])
      .build()
  }

}
