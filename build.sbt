scalaVersion := "2.11.7"

enablePlugins(JavaAppPackaging)

assemblyJarName in assembly := "openolitor-server.jar"

mainClass in assembly := Some("ch.openolitor.core.Boot")

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case "library.properties"                      => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
