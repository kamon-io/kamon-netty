# Kamon Netty <img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/> 
[![Build Status](https://travis-ci.org/kamon-io/kamon-netty.svg?branch=master)](https://travis-ci.org/kamon-io/kamon-netty)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-netty_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-netty_2.12)


### Getting Started

Kamon Netty is currently available for Scala 2.11 and 2.12.

Supported releases and dependencies are shown below.

| kamon  | status | jdk  | scala            
|:------:|:------:|:----:|------------------
|  1.0.0 | experimental | 1.8+ | 2.11, 2.12

To get started with SBT, simply add the following to your `build.sbt` or `pom.xml`
file:

```scala
libraryDependencies += "io.kamon" %% "kamon-netty" % "1.0.0"
```

```xml
<dependency>
    <groupId>io.kamon</groupId>
    <artifactId>kamon-netty_2.12</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Documentation

### Event Loop Metrics ###

The metrics that you will get for an __EventLoop__ are:

* __registered-channels__: The number of registered Channels.
* __task-processing-time__: A histogram that tracks the nanoseconds the last processing of all tasks took.
* __task-queue-size__: The number of tasks that are pending for processing.
* __task-waiting-time__: A histogram that tracks the waiting time in the queue.

![Image of Netty Metrics](/img/netty-metrics.png)

### Traces ###

![Image of Netty Metrics](/img/netty-jaeger.png)
