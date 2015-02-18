lazy val root = Project(
  "murtools", file(".")
).enablePlugins(PlayScala).settings(
  scalaVersion := "2.11.5"
)
