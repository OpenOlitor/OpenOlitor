organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.14"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     					 % sprayV,
    "io.spray"            %%  "spray-routing" 					 % sprayV,
    "io.spray"            %%  "spray-testkit" 					 % sprayV  % "test",
    "io.spray" 			  %%  "spray-json"    					 % "1.3.2",
    "com.typesafe.akka"   %%  "akka-actor"    					 % akkaV,
    "com.typesafe.akka"   %%  "akka-persistence-experimental"    % akkaV,    
    "com.typesafe.akka"   %%  "akka-testkit"  			    	 % akkaV   % "test",    
    "com.github.dnvriend" %% "akka-persistence-inmemory" 		 % "1.0.0" % "test",
    "org.specs2"          %%  "specs2-core"   					 % "2.4.2" % "test",
    "org.scalaz" 		  %% "scalaz-core"						 % "7.1.5",
    //akka persistence journal driver
    "com.okumin" 		  %% "akka-persistence-sql-async" 		 % "0.3.1",
    "com.github.mauricio" %% "mysql-async" 						 % "0.2.16"
  )
}

Revolver.settings
