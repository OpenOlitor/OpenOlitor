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
\*                                                                              */
package ch.openolitor

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import spray.json._
import DefaultJsonProtocol._ // if you don't supply your own Protocol (see below)

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class HelloWorldServiceActor extends Actor with HelloWorldService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

case class HelloWorld(message: String)

object OpenOlitorJsonProtocol extends DefaultJsonProtocol {
  implicit val helloWorldFormat = jsonFormat1(HelloWorld)
}

// this trait defines our service behavior independently from the service actor
trait HelloWorldService extends HttpService {

  import OpenOlitorJsonProtocol._

  val myRoute =
    path("hello" / "xml") {
      get {
        respondWithMediaType(`text/xml`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Hello World</h1>
              </body>
            </html>
          }
        }
      }
    } ~
      path("hello" / "json") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              HelloWorld("Hello World!")
            }
          }
        }
      }
}