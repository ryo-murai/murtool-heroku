import NativePackagerKeys._

lazy val root = Project(
  "murtools", file(".")
).enablePlugins(PlayScala).settings(
  scalaVersion := "2.11.5"
)

herokuAppName in Compile := "murtools"

herokuJdkVersion in Compile := "1.8"

herokuStack in Compile := "cedar-14"

herokuConfigVars in Compile := Map(
  "JAVA_OPTS" -> "-Xmx384m -Xss512k -XX:+UseCompressedOops"
)

herokuProcessTypes in Compile := Map(
  "web" -> "target/universal/stage/bin/murtools -Dhttp.port=$PORT"
)

herokuIncludePaths in Compile := Seq(
  "app", "conf", "public"
)
