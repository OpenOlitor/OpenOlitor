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
import spray.routing.Directive._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import spray.httpx.unmarshalling.Unmarshaller
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import java.util.UUID
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent.Future

trait StammdatenRoutes extends HttpService with ActorReferences with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService {
  self: StammdatenRepositoryComponent =>

  implicit val abotypIdParamConverter = string2BaseIdConverter[AbotypId](AbotypId.apply)
  implicit val abotypIdPath = string2BaseIdPathMatcher[AbotypId](AbotypId.apply)
  implicit val personIdPath = string2BaseIdPathMatcher[PersonId](PersonId.apply)
  implicit val depotIdPath = string2BaseIdPathMatcher[DepotId](DepotId.apply)

  import StammdatenJsonProtocol._
  import EntityStore._

  //TODO: get real userid from login
  override val userId: UserId = Boot.systemUserId

  lazy val stammdatenRoute = aboTypenRoute ~ personenRoute ~ depotsRoute

  lazy val personenRoute =
    path("personen") {
      get(list(readRepository.getPersonen)) ~
        post(create[PersonModify, PersonId](PersonId.apply _))
    } ~
      path("personen" / personIdPath) { id =>
        get(detail(readRepository.getPersonDetail(id))) ~
          (put | post)(update[PersonModify, PersonId](id)) ~
          delete(remove(id))
      }

  lazy val aboTypenRoute =
    path("abotypen") {
      get(list(readRepository.getAbotypen)) ~
        post(create[AbotypModify, AbotypId](AbotypId.apply _))
    } ~
      path("abotypen" / abotypIdPath) { id =>
        get(detail(readRepository.getAbotypDetail(id))) ~
          (put | post)(update[AbotypModify, AbotypId](id)) ~
          delete(remove(id))
      }

  lazy val depotsRoute =
    path("depots") {
      get(list(readRepository.getDepots)) ~
        post(create[DepotModify, DepotId](DepotId.apply _))
    } ~
      path("depots" / depotIdPath) { id =>
        get(detail(readRepository.getDepotDetail(id))) ~
          (put | post)(update[DepotModify, DepotId](id)) ~
          delete(remove(id))
      }
}