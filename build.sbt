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

val kamonCore         = "io.kamon"    %% "kamon-core"                     % "2.0.4"
val kamonTestkit      = "io.kamon"    %% "kamon-testkit"                  % "2.0.4"
val kamonCommon       = "io.kamon"    %% "kamon-instrumentation-common"   % "2.0.1"
val kanela            = "io.kamon"    %  "kanela-agent"                   % "1.0.5"

val netty           = "io.netty"        %  "netty-all"                      % "4.1.16.Final"
val nettyNative     = "io.netty"        %  "netty-transport-native-epoll"   % "4.1.16.Final"    classifier "linux-x86_64"
val logback         = "ch.qos.logback"  %  "logback-classic"                % "1.0.13"


lazy val root = (project in file("."))
  .settings(Seq(
      name := "kamon-netty",
      scalaVersion := "2.12.10",
      crossScalaVersions := Seq("2.12.10", "2.13.1")))
  .enablePlugins(JavaAgent)
  .settings(instrumentationSettings)
  .settings(isSnapshot := true)
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "releases"))
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"))
  .settings(resolvers += Resolver.mavenLocal)
  .settings(javaAgents += "io.kamon"    % "kanela-agent"   % "1.0.5"  % "compile;test")
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonCommon) ++
      providedScope(netty,  nettyNative, kanela) ++
      testScope(scalatest, kamonTestkit, logbackClassic, logback))


