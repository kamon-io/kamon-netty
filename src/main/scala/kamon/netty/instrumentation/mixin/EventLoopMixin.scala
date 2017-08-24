package kamon.netty.instrumentation.mixin

class EventLoopMixin extends NamedEventLoopGroup

trait NamedEventLoopGroup {
  var name: String = _
}
