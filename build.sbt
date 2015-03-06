import NativePackagerKeys._

lazy val root = Project(
  "murtools", file(".")
).enablePlugins(PlayScala).settings(
  version := "0.1",
  scalaVersion := "2.11.5",
  resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
  libraryDependencies ++= Seq(
    filters,
    jdbc,
    "com.sendgrid"         % "sendgrid-java"                 % "2.0.0",
    "jp.t2v"               %% "play2-auth"                   % "0.13.0",
    "org.mindrot"          % "jbcrypt"                       % "0.3m",
    "org.skinny-framework" %% "skinny-orm"                   % "1.3.13",
    "org.scalikejdbc" %% "scalikejdbc-config"                % "2.2.3",
    "org.scalikejdbc" %% "scalikejdbc-play-dbplugin-adapter" % "2.3.4",
    "org.scalikejdbc" %% "scalikejdbc-play-fixture-plugin"   % "2.3.4",
    "com.github.tototoshi" %% "play-flyway"                  % "1.2.+",
    "commons-dbcp"         %  "commons-dbcp"                 % "1.4",
    "org.postgresql"       %  "postgresql"                   % "9.3-1103-jdbc3",
    "com.h2database"       %  "h2"                           % "1.4.+"
  )
).settings(scalariformSettings: _*)

herokuAppName in Compile := "murtools"
herokuJdkVersion in Compile := "1.8"
herokuStack in Compile := "cedar-14"
herokuConfigVars in Compile := Map(
  "JAVA_OPTS" -> "-Xmx384m -Xss512k -XX:+UseCompressedOops"
)
herokuProcessTypes in Compile := Map(
  "web" -> "target/universal/stage/bin/murtools -Dhttp.port=$PORT -Dconfig.resource=prod.conf"
)
herokuIncludePaths in Compile := Seq(
  "app", "conf", "public"
)
