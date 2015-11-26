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
package ch.openolitor.stammdaten

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core.ActorReferences
import spray.httpx.unmarshalling.Unmarshaller
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

trait StammdatenRoutes extends HttpService with ActorReferences {
  self: StammdatenRepositoryComponent =>

  import StammdatenJsonProtocol._

  val stammdatenRoute =
    path("abotypen") {
      get {
        //fetch list of abotypen
        onSuccess(readRepository.getAbotypen) { abotypen =>
          complete(abotypen)
        }
      } ~
        post {
          entity(as[Abotyp]) { abotyp =>
            //create abotyp
            complete(abotyp)
          }
        }
    } ~
      path("abotypen" / Segment) { id =>
        get {
          complete {
            //get detail of abotyp
            ""
          }
        } ~
          put {
            entity(as[Abotyp]) { abotyp =>
              //update abotyp
              complete(abotyp)
            }
          } ~
          delete {
            complete {
              //delete abottyp
              ""
            }
          }
      }
}