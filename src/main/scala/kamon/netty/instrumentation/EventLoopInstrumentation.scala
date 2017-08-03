package kamon.netty.instrumentation

import io.netty.util.concurrent.EventExecutor
import kamon.netty.Metrics
import kamon.netty.util.Latency
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{AfterReturning, Around, Aspect, Before}

@Aspect
class EventLoopInstrumentation {


  @Before("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.addTask(..)) && this(eventLoop)")
  def onAddTask(eventLoop: EventExecutor): Unit = {
    val name1 = eventLoop.getClass.getSimpleName
    println("addTask: " + eventLoop.hashCode() + "thread: " + Thread.currentThread().getName)

    Metrics.forEventLoop(name1).pendingTasks.increment()
  }

  @Before("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.removeTask(..)) && this(eventLoop)")
  def onRemoveTasks(eventLoop: EventExecutor): Unit = {
    val name = eventLoop.getClass.getSimpleName
    println("removeTask: " + eventLoop.hashCode() + "thread: " + Thread.currentThread().getName)

    Metrics.forEventLoop(name).pendingTasks.increment()
  }

  @Around("execution(* io.netty.util.concurrent.SingleThreadEventExecutor+.runAllTasks(..)) && this(eventLoop)")
  def onRunAllTasks(pjp:ProceedingJoinPoint, eventLoop: EventExecutor): Any = {
    println("runAllTasks: " + eventLoop.hashCode() + "thread: " + Thread.currentThread().getName)
    Latency.measure(Metrics.forEventLoop(eventLoop.getClass.getSimpleName).taskProcessingTime)(pjp.proceed())
  }
}
