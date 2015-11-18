/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       OpenOlitor                          *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://openolitor.ch                *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.io.IO
import spray.can.Http
import akka.util.Timeout
import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor
  val service = system.actorOf(Props[HelloWorldServiceActor], "hello-world-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 9005 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 9005)
}
