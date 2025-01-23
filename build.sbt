ThisBuild / organization := "es.eriktorr"
ThisBuild / version := "1.0.0"
ThisBuild / idePackagePrefix := Some("es.eriktorr")
Global / excludeLintKeys += idePackagePrefix

ThisBuild / scalaVersion := "3.3.4"

ThisBuild / semanticdbEnabled := true
ThisBuild / javacOptions ++= Seq("-source", "21", "-target", "21")

Global / cancelable := true
Global / fork := true
Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias(
  "check",
  "; unusedCompileDependenciesTest; scalafixAll; scalafmtSbtCheck; scalafmtCheckAll",
)

lazy val MUnitFramework = new TestFramework("munit.Framework")
lazy val warts = Warts.unsafe.filter(_ != Wart.DefaultArguments)

lazy val withBaseSettings: Project => Project = _.settings(
  Compile / doc / sources := Seq(),
  Compile / compile / wartremoverErrors ++= warts,
  Test / compile / wartremoverErrors ++= warts,
  libraryDependencies ++= Seq(
    "io.chrisdavenport" %% "cats-scalacheck" % "0.3.2" % Test,
    "org.apache.logging.log4j" % "log4j-core" % "2.24.3" % Test,
    "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.3" % Test,
    "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.3" % Test,
    "org.scalameta" %% "munit" % "1.0.4" % Test,
    "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test,
    "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
    "org.typelevel" %% "scalacheck-effect" % "1.0.4" % Test,
    "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4" % Test,
  ),
  Test / envVars := Map(
    "SBT_TEST_ENV_VARS" -> "true",
    "ONLINE_RETAILER_JDBC_CONNECTIONS" -> "2:4",
    "ONLINE_RETAILER_JDBC_CONNECT_URL" -> "jdbc:postgresql://localhost:5432/online_retailer",
    "ONLINE_RETAILER_JDBC_PASSWORD" -> "online_retailer_password",
    "ONLINE_RETAILER_JDBC_USERNAME" -> "online_retailer_username",
    "ONLINE_RETAILER_GRPC_HOST" -> "grpc.test",
    "ONLINE_RETAILER_GRPC_PORT" -> "1234",
    "TSID_NODE" -> "1",
  ),
  Test / testFrameworks += MUnitFramework,
  Test / testOptions += Tests.Argument(MUnitFramework, "--exclude-tags=online"),
)

lazy val usingProtobuf: Project => Project = withBaseSettings.compose(
  _.settings(
    Compile / tpolecatExcludeOptions ++= Set(
      org.typelevel.scalacoptions.ScalacOptions.warnValueDiscard,
      org.typelevel.scalacoptions.ScalacOptions.warnUnusedImports,
    ),
    wartremoverExcluded += sourceManaged.value,
    unusedCompileDependenciesFilter -= moduleFilter("io.grpc", "grpc-core"),
    unusedCompileDependenciesFilter -= moduleFilter("io.grpc", "grpc-protobuf"),
  ),
)

lazy val usingDoobie: Project => Project = withBaseSettings.compose(
  _.settings(
    unusedCompileDependenciesFilter -= moduleFilter("org.tpolecat", "doobie-postgres"),
  ),
)

lazy val `clothing-client` = project
  .in(file("modules/clothing/clothing-client"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(),
  )
  .dependsOn(
    `clothing-dsl` % "test->test;compile->compile",
    `commons-grpc`,
    `clothing-protobuf`,
  )

lazy val `clothing-dsl` = project
  .in(file("modules/clothing/clothing-dsl"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.arainko" %% "ducktape" % "0.2.7",
      "io.github.iltotore" %% "iron" % "2.6.0",
      "io.hypersistence" % "hypersistence-tsid" % "2.1.3",
      "org.typelevel" %% "cats-collections-core" % "0.9.9",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(`commons-lang` % "test->test;compile->compile")

lazy val `clothing-protobuf` = project
  .in(file("modules/clothing/clothing-protobuf"))
  .configure(usingProtobuf)
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(
    `clothing-dsl` % "test->test;compile->compile",
    `commons-grpc`,
  )
  .enablePlugins(Fs2Grpc)

lazy val `clothing-service` = project
  .in(file("modules/clothing/clothing-service"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lmax" % "disruptor" % "3.4.4" % Runtime,
      "com.monovore" %% "decline-effect" % "2.5.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.3" % Runtime,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
    ),
    Universal / maintainer := "https://github.com/etorres/online-retailer",
  )
  .dependsOn(
    `commons-database` % "test->test;compile->compile",
    `commons-grpc` % "test->test;compile->compile",
    `clothing-dsl` % "test->test;compile->compile",
    `clothing-protobuf`,
  )
  .enablePlugins(JavaAppPackaging)

lazy val `commons-database` = project
  .in(file("modules/commons/commons-database"))
  .configure(usingDoobie)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.common" %% "tagging" % "2.3.5",
      "com.zaxxer" % "HikariCP" % "6.2.1" exclude ("org.slf4j", "slf4j-api"),
      "io.github.iltotore" %% "iron-decline" % "2.6.0",
      "org.flywaydb" % "flyway-core" % "11.2.0",
      "org.flywaydb" % "flyway-database-postgresql" % "11.2.0" % Runtime,
      "org.postgresql" % "postgresql" % "42.7.5" % Runtime,
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC6" % Test,
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC6",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC6",
      "org.typelevel" %% "cats-collections-core" % "0.9.9",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
    ),
  )
  .dependsOn(`commons-lang` % "test->test;compile->compile")

lazy val `commons-grpc` = project
  .in(file("modules/commons/commons-grpc"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.comcast" %% "ip4s-core" % "3.6.0",
      "com.monovore" %% "decline" % "2.5.0",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "fs2-grpc-runtime" % "2.7.21",
    ),
  )
  .dependsOn(`commons-lang` % "test->test;compile->compile")

lazy val `commons-lang` = project
  .in(file("modules/commons/commons-lang"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "os-lib" % "0.11.3" % Test,
      "com.lmax" % "disruptor" % "3.4.4" % Test,
      "io.circe" %% "circe-core" % "0.14.10" % Test,
      "io.circe" %% "circe-parser" % "0.14.10" % Test,
      "io.github.iltotore" %% "iron" % "2.6.0" % Optional,
      "io.hypersistence" % "hypersistence-tsid" % "2.1.3" % Optional,
      "org.apache.logging.log4j" % "log4j-core" % "2.24.3" % Test,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.3" % Test,
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.3" % Test,
      "org.typelevel" %% "cats-collections-core" % "0.9.9" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Optional,
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0" % Test,
      "org.typelevel" %% "squants" % "1.8.3" % Optional,
    ),
  )

lazy val `electronics-client` = project
  .in(file("modules/electronics/electronics-client"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(),
  )
  .dependsOn(
    `commons-grpc`,
    `electronics-dsl` % "test->test;compile->compile",
    `electronics-protobuf`,
  )

lazy val `electronics-dsl` = project
  .in(file("modules/electronics/electronics-dsl"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.arainko" %% "ducktape" % "0.2.7",
      "io.github.iltotore" %% "iron" % "2.6.0",
      "io.hypersistence" % "hypersistence-tsid" % "2.1.3",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(`commons-lang` % "test->test;compile->compile")

lazy val `electronics-protobuf` = project
  .in(file("modules/electronics/electronics-protobuf"))
  .configure(usingProtobuf)
  .settings(
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(
    `commons-grpc`,
    `electronics-dsl` % "test->test;compile->compile",
  )
  .enablePlugins(Fs2Grpc)

lazy val `electronics-service` = project
  .in(file("modules/electronics/electronics-service"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lmax" % "disruptor" % "3.4.4" % Runtime,
      "com.monovore" %% "decline-effect" % "2.5.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.3" % Runtime,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
    ),
    Universal / maintainer := "https://github.com/etorres/online-retailer",
  )
  .dependsOn(
    `commons-database` % "test->test;compile->compile",
    `commons-grpc` % "test->test;compile->compile",
    `electronics-dsl` % "test->test;compile->compile",
    `electronics-protobuf`,
  )
  .enablePlugins(JavaAppPackaging)

lazy val `product-search` = project
  .in(file("modules/products/product-search"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(),
    Universal / maintainer := "https://github.com/etorres/online-retailer",
  )
  .dependsOn(`clothing-client`)
  .enablePlugins(JavaAppPackaging)

lazy val `stock-client` = project
  .in(file("modules/stock/stock-client"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(),
  )
  .dependsOn(
    `commons-grpc`,
    `stock-dsl` % "test->test;compile->compile",
    `stock-protobuf`,
  )

lazy val `stock-dsl` = project
  .in(file("modules/stock/stock-dsl"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.iltotore" %% "iron" % "2.6.0",
      "io.hypersistence" % "hypersistence-tsid" % "2.1.3",
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(`commons-lang` % "test->test;compile->compile")

lazy val `stock-protobuf` = project
  .in(file("modules/stock/stock-protobuf"))
  .configure(usingProtobuf)
  .settings(
    libraryDependencies ++= Seq(
      "io.github.arainko" %% "ducktape" % "0.2.7",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.typelevel" %% "squants" % "1.8.3",
    ),
  )
  .dependsOn(
    `commons-grpc`,
    `stock-dsl` % "test->test;compile->compile",
  )
  .enablePlugins(Fs2Grpc)

lazy val `stock-service` = project
  .in(file("modules/stock/stock-service"))
  .configure(withBaseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.lmax" % "disruptor" % "3.4.4" % Runtime,
      "com.monovore" %% "decline-effect" % "2.5.0",
      "org.apache.logging.log4j" % "log4j-core" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-layout-template-json" % "2.24.3" % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.24.3" % Runtime,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
    ),
    Universal / maintainer := "https://github.com/etorres/online-retailer",
  )
  .dependsOn(
    `commons-database` % "test->test;compile->compile",
    `commons-grpc` % "test->test;compile->compile",
    `stock-dsl` % "test->test;compile->compile",
    `stock-protobuf`,
  )
  .enablePlugins(JavaAppPackaging)

lazy val root = project
  .in(file("."))
  .aggregate(
    `clothing-client`,
    `clothing-dsl`,
    `clothing-protobuf`,
    `clothing-service`,
    `commons-database`,
    `commons-grpc`,
    `commons-lang`,
    `electronics-client`,
    `electronics-dsl`,
    `electronics-protobuf`,
    `electronics-service`,
    `product-search`,
    `stock-client`,
    `stock-dsl`,
    `stock-protobuf`,
    `stock-service`,
  )
  .settings(
    name := "online-retailer",
    Compile / doc / sources := Seq(),
    publish := {},
    publishLocal := {},
  )
