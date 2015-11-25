/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
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
package ch.openolitor.helloworld

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import spray.json._
import spray.json.DefaultJsonProtocol._

case class HelloWorld(message: String)

// this trait defines our service behavior independently from the service actor
trait HelloWorldRoutes extends HttpService {

  import HelloWorldJsonProtocol._

  val helloWorldRoute =
    pathPrefix("hello") {
      helloRoute()
    }

  /**
   * Hello World demo routes
   */
  def helloRoute(): Route =
    path("xml") {
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
      path("json") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              HelloWorld("Hello World!")
            }
          }
        }
      }
}
