package kamon.netty.instrumentation.mixin

import kamon.Kamon
import kamon.agent.api.instrumentation.Initializer
import kamon.context.Context

import scala.beans.BeanProperty


trait RequestContextAware {
  @volatile @BeanProperty var context: Context = _
}


/**
  * --
  */
class RequestContextAwareMixin extends RequestContextAware {

  @Initializer
  def init(): Unit = context = Kamon.currentContext()
}
