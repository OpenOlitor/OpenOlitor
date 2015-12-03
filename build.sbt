organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

resolvers += "Spray" at "http://repo.spray.io"

libraryDependencies ++= {
  val akkaV = "2.3.14"
  val sprayV = "1.3.3"
  val scalalikeV = "2.3.0"
  Seq(
    "io.spray"            %%  "spray-can"     					 % sprayV,
    "io.spray"            %%  "spray-routing-shapeless2" 		 % sprayV,
    "io.spray"            %%  "spray-testkit" 					 % sprayV  % "test",
    "io.spray" 			  %%  "spray-json"    					 % "1.3.2",
    "com.wandoulabs.akka" %%  "spray-websocket" 				 % "0.1.4",
    "com.typesafe.akka"   %%  "akka-actor"    					 % akkaV,
    "com.typesafe.akka"   %%  "akka-persistence-experimental"    % akkaV,    
    "com.typesafe.akka"   %%  "akka-slf4j"    					 % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  			    	 % akkaV   % "test",    
    "com.github.dnvriend" %%  "akka-persistence-inmemory" 		 % "1.0.5" % "test",
    "org.specs2"          %%  "specs2-core"   					 % "2.4.2" % "test",
    "org.scalaz" 		  %%  "scalaz-core"						 % "7.1.5",
    //use scala logging to log outside of the actor system
    "com.typesafe.scala-logging" %% "scala-logging"				 % "3.1.0",
    //akka persistence journal driver
    "com.okumin" 		  %% "akka-persistence-sql-async" 		 % "0.2.1", //use older version to stay compatible with akka 1.3.x
    "com.github.mauricio" %% "mysql-async" 						 % "0.2.16",
    //
    "org.scalikejdbc" 	  %% "scalikejdbc-config"				 % scalalikeV,
    "org.scalikejdbc" 	  %% "scalikejdbc-syntax-support-macro"  % scalalikeV,
    "ch.qos.logback"  	  %  "logback-classic"    		  		 % "1.1.3",
    "org.mariadb.jdbc"	  %  "mariadb-java-client"               % "1.3.2"    
  )
}

Revolver.settings
