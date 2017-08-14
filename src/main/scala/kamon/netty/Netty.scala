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

package kamon.netty

import com.typesafe.config.Config
import io.netty.handler.codec.http.HttpRequest
import kamon.Kamon
import kamon.util.DynamicAccess

object Netty {
  @volatile private var nameGenerator: NameGenerator = new DefaultNameGenerator()
  loadConfiguration(Kamon.config())

  def generateOperationName(request: HttpRequest): String =
    nameGenerator.generateOperationName(request)

  def generateHttpClientOperationName(request: HttpRequest): String =
    nameGenerator.generateHttpClientOperationName(request)

  Kamon.onReconfigure((newConfig: Config) => Netty.loadConfiguration(newConfig))

  private def loadConfiguration(config: Config): Unit = synchronized {
    val dynamic = new DynamicAccess(getClass.getClassLoader)
    val nameGeneratorFQCN = config.getString("kamon.netty.name-generator")
    nameGenerator = dynamic.createInstanceFor[NameGenerator](nameGeneratorFQCN, Nil).get
  }
}

trait NameGenerator {
  def generateOperationName(request: HttpRequest): String
  def generateHttpClientOperationName(request: HttpRequest): String
}

class DefaultNameGenerator extends NameGenerator {

  override def generateHttpClientOperationName(request: HttpRequest): String =
    request.getUri

  override def generateOperationName(request: HttpRequest): String =
    request.getUri
}
