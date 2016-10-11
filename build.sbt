scalaVersion := "2.11.8"

enablePlugins(JavaServerAppPackaging)

name := "openolitor-server"
mainClass in Compile := Some("ch.openolitor.core.Boot")

sources in EditSource <++= baseDirectory.map(d => (d / "manifest_test.yml").get)
sources in EditSource <++= baseDirectory.map(d => (d / "manifest_int.yml").get)
sources in EditSource <++= baseDirectory.map(d => (d / "manifest_prod.yml").get)
variables in EditSource += ("foo", "bar")
targetDirectory in EditSource <<= baseDirectory(_ / "target")

assemblyJarName in assembly := "openolitor-server.jar"

mainClass in assembly := Some("ch.openolitor.core.Boot")

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case "library.properties"                      => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
