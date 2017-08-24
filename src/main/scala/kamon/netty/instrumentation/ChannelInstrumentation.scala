package kamon.netty.instrumentation

import kamon.agent.scala.KamonInstrumentation
import kamon.netty.instrumentation.mixin.ChannelContextAwareMixin

class ChannelInstrumentation extends KamonInstrumentation {

  forSubtypeOf("io.netty.channel.Channel") { builder ⇒
    builder
      .withMixin(classOf[ChannelContextAwareMixin])
      .build()
  }
}
