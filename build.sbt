name := """ehealth-sample-spark-vdc"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= {
	val sparkVersion = "2.3.0"
        val hadoopVersion = "2.8.2"
  Seq(
    jdbc,
    ehcache,
    ws,
    "com.typesafe.play" %% "play-json" % "2.5.7",
    "org.webjars" % "bootstrap" % "4.0.0-2",
    "io.swagger" %% "swagger-play2" % "1.6.0",
    "org.webjars" %% "webjars-play" % "2.6.3",
    "org.webjars" % "swagger-ui" % "3.13.0",
    "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B4",
    "org.codehaus.janino" % "janino" % "3.0.8",
    guice,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.spark" % "spark-core_2.11" % sparkVersion exclude("org.apache.hadoop","hadoop-client"),
    "org.apache.spark" % "spark-sql_2.11" % sparkVersion,
    "org.apache.hadoop" % "hadoop-aws" % hadoopVersion,
    "com.amazonaws" % "aws-java-sdk-bundle" % "1.11.234",
    "mysql" % "mysql-connector-java" % "6.0.6",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    specs2 % Test,
    "org.yaml" % "snakeyaml" % "1.21"

  )
}

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }
// libraryDependencies ~= { _.map(_.exclude("com.fasterxml.jackson.module", "jackson-module-scala_2.11")) }
// libraryDependencies ~= { _.map(_.exclude("org.scala-lang.modules", "scala-parser-combinators_2.11")) }

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case "about.html" => MergeStrategy.rename
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "mozilla/public-suffix-list.txt"  => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", xs @ _*) => 
    MergeStrategy.discard
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


enablePlugins(sbtdocker.DockerPlugin, JavaServerAppPackaging)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java:8-jre-alpine")
    copy(artifact, artifactTargetPath)
    expose(9000)
    entryPoint("java", "-Dplay.http.secret.key='wspl4r'", "-jar", artifactTargetPath)
  }

}

