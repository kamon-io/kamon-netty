/* =========================================================================================
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


val kamonCore         = "io.kamon"    %% "kamon-core"                   % "1.0.0-RC1-0930e36def6ce62c55d30d744b41ef475374a541"
val kamonExecutors    = "io.kamon"    %% "kamon-executors"              % "1.0.0-RC1-82bae39750d62b3060214d0c2c19cf3894b97323" exclude("io.kamon", "kamon-core")

val scalaExtension    = "io.kamon"    %% "agent-scala-extension"        % "0.0.3-experimental"

val netty             = "io.netty"    %  "netty-all"                    % "4.0.50.Final"
val nettyNative       ="io.netty"     % "netty-transport-native-epoll"  % "4.0.50.Final" classifier "linux-x86_64"

val logback           = "ch.qos.logback"            %   "logback-classic"       % "1.0.13"


lazy val root = (project in file("."))
  .settings(name := "kamon-netty")
  .enablePlugins(JavaAgent)
  .settings(isSnapshot := true)
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"))
//  .settings(javaAgents += "io.kamon"    % "kamon-agent"   % "0.0.3-experimental"  % "compile;test")
  .settings(javaAgents += "org.aspectj"    % "aspectjweaver"   % "1.8.10"  % "compile;test;runtime")
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonExecutors, scalaExtension, netty, logback, nettyNative) ++
//      providedScope(netty, nettyNative) ++
      testScope(scalatest, logbackClassic))