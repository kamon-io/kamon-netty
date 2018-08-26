package kamon.netty.instrumentation

import kamon.netty.instrumentation.mixin.ChannelContextAwareMixin
import kanela.agent.scala.KanelaInstrumentation

class ChannelInstrumentation extends KanelaInstrumentation {

  forSubtypeOf("io.netty.channel.Channel") { builder ⇒
    builder
      .withMixin(classOf[ChannelContextAwareMixin])
      .build()
  }
}
