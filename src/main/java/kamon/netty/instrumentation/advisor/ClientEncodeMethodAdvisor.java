package kamon.netty.instrumentation.advisor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import kamon.agent.libs.net.bytebuddy.asm.Advice.Argument;
import kamon.agent.libs.net.bytebuddy.asm.Advice.OnMethodEnter;
import kamon.netty.instrumentation.HttpRequestContext;

public class ClientEncodeMethodAdvisor {

  @OnMethodEnter
  static void onEnter(@Argument(value = 0) Object ctx,
                      @Argument(value = 1, readOnly = false) HttpRequest request) {
    request = HttpRequestContext.withContext(request, (ChannelHandlerContext) ctx);
  }
}
