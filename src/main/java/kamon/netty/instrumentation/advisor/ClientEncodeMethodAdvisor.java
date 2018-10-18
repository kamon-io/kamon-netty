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

package kamon.netty.instrumentation.advisor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import kamon.netty.instrumentation.HttpRequestContext;
import kanela.agent.libs.net.bytebuddy.asm.Advice.Argument;
import kanela.agent.libs.net.bytebuddy.asm.Advice.OnMethodEnter;

public class ClientEncodeMethodAdvisor {

  @OnMethodEnter
  static void onEnter(@Argument(value = 0) ChannelHandlerContext ctx,
                      @Argument(value = 1, readOnly = false) Object request) {
      if (request instanceof HttpRequest) {
          request = HttpRequestContext.withContext((HttpRequest)request, ctx);
      }
  }
}
