package kamon.netty.instrumentation.advisor;

import io.netty.channel.EventLoop;
import java.util.Queue;
import kamon.agent.libs.net.bytebuddy.asm.Advice.OnMethodExit;
import kamon.agent.libs.net.bytebuddy.asm.Advice.Return;
import kamon.agent.libs.net.bytebuddy.asm.Advice.This;
import kamon.netty.util.MonitoredQueue;

public class NewTaskQueueMethodAdvisor {

  @OnMethodExit
  static void onExit(@This Object eventLoop, @Return(readOnly = false) Queue<Runnable> queue) {
    queue = MonitoredQueue.apply((EventLoop) eventLoop, queue);
  }
}
