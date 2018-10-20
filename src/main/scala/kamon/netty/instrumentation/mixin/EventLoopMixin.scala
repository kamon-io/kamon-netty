/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
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

package kamon.netty.instrumentation.mixin

trait NamedEventLoopGroup {
  def setName(name: String)
  def getName: String
}

/**
  * Mixin for io.netty.channel.EventLoopGroup
  */
class EventLoopMixin extends NamedEventLoopGroup {
  @volatile var _name: String = _
  override def setName(name: String): Unit = _name = name
  override def getName: String = _name
}
