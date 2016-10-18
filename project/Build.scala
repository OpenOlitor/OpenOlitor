import sbt._
import Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object BuildSettings {
  val specs2V = "2.4.17" // based on spray 1.3.x built in support 
  val akkaV = "2.4.+"
  val sprayV = "1.3.+"
  val scalalikeV = "2.3.+"
 
  val buildSettings = SbtScalariform.scalariformSettings ++ Seq(
    organization := "ch.openolitor.scalamacros",
    version := "1.0.1",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
    resolvers += "Spray" at "http://repo.spray.io",
    resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    mainClass in (Compile, run) := Some("ch.openolitor.core.Boot"),

    libraryDependencies ++= {	  
	  Seq(
	    "io.spray"                     %%  "spray-can"     					              % sprayV,
	    "io.spray"                     %%  "spray-caching"     					          % sprayV,
	    "io.spray"                     %%  "spray-routing-shapeless2" 		        % sprayV,
	    "io.spray"                     %%  "spray-testkit" 					              % sprayV  % "test",
	    "io.spray" 			               %%  "spray-json"    					              % sprayV,
	    "io.spray" 			               %%  "spray-client"  					              % sprayV,
	    "com.wandoulabs.akka"          %%  "spray-websocket" 				              % "0.1.4",
	    "com.typesafe.akka"            %%  "akka-actor"    					              % akkaV,
	    "com.typesafe.akka"            %%  "akka-persistence"                     % akkaV,    
	    "com.typesafe.akka"            %%  "akka-persistence-query-experimental"  % akkaV,
	    "com.typesafe.akka"            %%  "akka-slf4j"    					              % akkaV,
	    "com.typesafe.akka"            %%  "akka-stream"    					              % akkaV,
	    "com.typesafe.akka"            %%  "akka-testkit"  			    	            % akkaV       % "test",    
	    "com.github.dnvriend"          %%  "akka-persistence-inmemory" 		        % "1.0.5"     % "test",
	    "org.specs2"                   %%  "specs2-core"   					              % specs2V     % "test",
	    "org.specs2"                   %%  "specs2-mock"                          % specs2V     % "test",
	    "org.specs2"                   %%  "specs2-junit"                         % specs2V     % "test",
	    "org.mockito"                  %   "mockito-core"                         % "1.10.19"   % "test",
	    "org.scalaz" 		               %%  "scalaz-core"						              % "7.1.8",
	    //use scala logging to log outside of the actor system
	    "com.typesafe.scala-logging"   %%  "scala-logging"				                % "3.1.0",
	    //akka persistence journal driver
	    "com.okumin" 		               %%  "akka-persistence-sql-async" 	        % "0.3.+",
	    "org.scalikejdbc"              %%  "scalikejdbc-async"                    % "0.5.+",
	    "com.github.mauricio"          %%  "mysql-async" 						              % "0.2.16",
	    //                             
	    "org.scalikejdbc" 	           %%  "scalikejdbc-config"				            % scalalikeV,
	    "org.scalikejdbc"              %%  "scalikejdbc-test"                     % scalalikeV   % "test",
	    "com.h2database"               %   "h2"                                   % "1.4.191"    % "test",
	    "org.scalikejdbc" 	           %%  "scalikejdbc-syntax-support-macro"     % scalalikeV,
	    "ch.qos.logback"  	           %   "logback-classic"    		  		        % "1.1.3",
	    "org.mariadb.jdbc"	           %   "mariadb-java-client"                  % "1.3.2",
	    // Libreoffice document API
	    "org.apache.odftoolkit"        %   "simple-odf"					                  % "0.8.1-incubating",
	    "com.jsuereth"                 %%  "scala-arm"                            % "1.4",
	    //simple websocket client
	    "org.jfarcand"                 %   "wcs"                                  % "1.5",
	    "com.scalapenos"               %%  "stamina-json"                         % "0.1.1",
      // s3
      "com.amazonaws"                %   "aws-java-sdk"                         % "1.11.4",
      "de.svenkubiak"                %   "jBCrypt"                              % "0.4.1",
      "me.lessis"                    %%  "courier"                              % "0.1.3",
      "com.github.nscala-time"       %%  "nscala-time"                          % "2.14.0"
	  )
	}
  )
}

object OpenOlitorBuild extends Build {
  import BuildSettings._

  lazy val sprayJsonMacro = RootProject(uri("git://github.com/zackangelo/spray-json-macros.git"))
  lazy val macroSub = Project("macro", file("macro"), settings = buildSettings ++ Seq(
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value))
  lazy val main = Project("main", file("."), settings = buildSettings) dependsOn (macroSub, sprayJsonMacro)
  lazy val root = Project("root", file("root"), settings = buildSettings) aggregate (macroSub, main, sprayJsonMacro)
}
