enablePlugins(GatlingPlugin)

scalaVersion := "2.12.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.0" % "test",
  "io.gatling" % "gatling-test-framework" % "3.0.0" % "test",
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0"
)
