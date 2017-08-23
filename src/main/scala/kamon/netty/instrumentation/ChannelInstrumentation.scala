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

import kamon.Kamon
import kamon.context.Context
import org.aspectj.lang.annotation._

import scala.beans.BeanProperty


trait ChannelContextAware {
  @volatile var startTime: Long = 0
  @volatile @BeanProperty var context:Context = Kamon.currentContext()
}

trait RequestContextAware {
  @volatile @BeanProperty var context:Context = Kamon.currentContext()
}

@Aspect
class ChannelInstrumentation {
  @DeclareMixin("io.netty.channel.Channel+")
  def mixinChannelToContextAware: ChannelContextAware =  new ChannelContextAware{}

  @DeclareMixin("io.netty.handler.codec.http.HttpMessage+")
  def mixinRequestToContextAware: RequestContextAware =  new RequestContextAware{}

  @After("execution(io.netty.handler.codec.http.HttpMessage+.new(..)) && this(request)")
  def afterCreation(request: RequestContextAware): Unit = {
    // Force traceContext initialization.
    request.getContext()
  }
}
