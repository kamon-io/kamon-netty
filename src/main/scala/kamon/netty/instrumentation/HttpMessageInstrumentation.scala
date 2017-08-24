package kamon.netty.instrumentation

import kamon.agent.scala.KamonInstrumentation
import kamon.netty.instrumentation.mixin.RequestContextAwareMixin

class HttpMessageInstrumentation extends KamonInstrumentation {

  forSubtypeOf("io.netty.handler.codec.http.HttpMessage") { builder ⇒
    builder
      .withMixin(classOf[RequestContextAwareMixin])
      .build()
  }

}
