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

import io.netty.channel.EventLoop;
import java.util.Queue;
import kamon.netty.util.MonitoredQueue;
import kanela.agent.libs.net.bytebuddy.asm.Advice.OnMethodExit;
import kanela.agent.libs.net.bytebuddy.asm.Advice.Return;
import kanela.agent.libs.net.bytebuddy.asm.Advice.This;

public class NewTaskQueueMethodAdvisor {

  @OnMethodExit
  static void onExit(@This Object eventLoop, @Return(readOnly = false) Queue<Runnable> queue) {
    queue = MonitoredQueue.apply((EventLoop) eventLoop, queue);
  }
}
