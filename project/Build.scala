import sbt._
import Keys._

object BuildSettings {
  val akkaV = "2.4.1"
  val sprayV = "1.3.3"
  val scalalikeV = "2.3.0"
  
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "ch.openolitor.scalamacros",
    version := "1.0.0",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
    resolvers += "Spray" at "http://repo.spray.io",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    mainClass in (Compile, run) := Some("ch.openolitor.core.Boot"),
    libraryDependencies ++= {	  
	  Seq(
	    "io.spray"            %%  "spray-can"     					 % sprayV,
	    "io.spray"            %%  "spray-routing-shapeless2" 		 % sprayV,
	    "io.spray"            %%  "spray-testkit" 					 % sprayV  % "test",
	    "io.spray" 			  %%  "spray-json"    					 % "1.3.2",
	    "com.wandoulabs.akka" %%  "spray-websocket" 				 % "0.1.4",
	    "com.typesafe.akka"   %%  "akka-actor"    					 % akkaV,
	    "com.typesafe.akka"   %%  "akka-persistence"    % akkaV,
	    "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaV,
	    "com.typesafe.akka"   %%  "akka-slf4j"    					 % akkaV,
	    "com.typesafe.akka"   %%  "akka-testkit"  			    	 % akkaV   % "test",    
	    "com.github.dnvriend" %%  "akka-persistence-inmemory" 		 % "1.0.5" % "test",
	    "org.specs2"          %%  "specs2-core"   					 % "2.4.2" % "test",
	    "org.scalaz" 		  %%  "scalaz-core"						 % "7.1.5",
	    //use scala logging to log outside of the actor system
	    "com.typesafe.scala-logging" %% "scala-logging"				 % "3.1.0",
	    //akka persistence journal driver
	    "com.okumin" 		  %% "akka-persistence-sql-async" 		 % "0.3.1", 
	    "com.github.mauricio" %% "mysql-async" 						 % "0.2.16",
	    //
	    "org.scalikejdbc" 	  %% "scalikejdbc-config"				 % scalalikeV,
	    "org.scalikejdbc" 	  %% "scalikejdbc-syntax-support-macro"  % scalalikeV,
	    "ch.qos.logback"  	  %  "logback-classic"    		  		 % "1.1.3",
	    "org.mariadb.jdbc"	  %  "mariadb-java-client"               % "1.3.2",
	    // Libreoffice document API
	    "org.apache.odftoolkit"	  %  "simple-odf"					 % "0.8.1-incubating"
	  )
	}
  )
}

object OpenOlitorBuild extends Build {
    import BuildSettings._ 
        
    lazy val macroSub = Project("macro", file("macro"), settings = buildSettings ++ Seq(
     libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
   ))
   lazy val main = Project("main", file("."), settings = buildSettings ) dependsOn(macroSub)
   lazy val root = Project("root", file("root"), settings = buildSettings) aggregate(macroSub, main)
}