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
import ch.openolitor.core.Macros._

trait StammdatenRoutes extends HttpService with ActorReferences with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService {
  self: StammdatenRepositoryComponent =>

  implicit val abotypIdParamConverter = string2BaseIdConverter(AbotypId.apply)
  implicit val abotypIdPath = string2BaseIdPathMatcher(AbotypId.apply)
  implicit val kundeIdPath = string2BaseIdPathMatcher(KundeId.apply)
  implicit val personIdPath = string2BaseIdPathMatcher(PersonId.apply)
  implicit val kundentypIdPath = string2BaseIdPathMatcher(CustomKundentypId.apply)
  implicit val depotIdPath = string2BaseIdPathMatcher(DepotId.apply)
  implicit val aboIdPath = string2BaseIdPathMatcher(AboId.apply)
  implicit val vertriebsartIdPath = string2BaseIdPathMatcher(VertriebsartId.apply)
  implicit val lieferungIdPath = string2BaseIdPathMatcher(LieferungId.apply)

  import StammdatenJsonProtocol._
  import EntityStore._

  //TODO: get real userid from login
  override val userId: UserId = Boot.systemUserId

  lazy val stammdatenRoute = aboTypenRoute ~ kundenRoute ~ depotsRoute ~ aboRoute ~ kundentypenRoute

  lazy val kundenRoute =
    path("kunden") {
      get(list(readRepository.getKunden)) ~
        post(create[KundeModify, KundeId](KundeId.apply _))
    } ~
      path("kunden" / kundeIdPath) { id =>
        get(detail(readRepository.getKundeDetail(id))) ~
          (put | post)(update[KundeModify, KundeId](id)) ~
          delete(remove(id))
      } ~
      path("kunden" / kundeIdPath / "abos") { kundeId =>
        post(create[AboModify, AboId](AboId.apply _))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath) { (kundeId, aboId) =>
        get(detail(readRepository.getAboDetail(aboId))) ~
          delete(remove(aboId))
      }

  lazy val kundentypenRoute =
    path("kundentypen") {
      get(list(readRepository.getKundentypen)) ~
        post(create[CustomKundentypCreate, CustomKundentypId](CustomKundentypId.apply _))
    } ~
      path("kundentypen" / kundentypIdPath) { (kundentypId) =>
        (put | post)(update[CustomKundentypModify, CustomKundentypId](kundentypId)) ~
          delete(remove(kundentypId))
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
      } ~
      path("abotypen" / abotypIdPath / "vertriebsarten") { abotypId =>
        get(list(readRepository.getVertriebsarten(abotypId))) ~
          post {
            requestInstance { request =>
              entity(as[VertriebsartModify]) { entity =>
                val vertriebsart = entity match {
                  case dl: DepotlieferungModify => copyTo[DepotlieferungModify, DepotlieferungAbotypModify](dl, "abotypId" -> abotypId)
                  case hl: HeimlieferungModify => copyTo[HeimlieferungModify, HeimlieferungAbotypModify](hl, "abotypId" -> abotypId)
                  case pl: PostlieferungModify => copyTo[PostlieferungModify, PostlieferungAbotypModify](pl, "abotypId" -> abotypId)
                }
                created(request)(vertriebsart)
              }
            }
          }
      } ~
      path("abotypen" / abotypIdPath / "vertriebsarten" / vertriebsartIdPath) { (abotypId, vertriebsartId) =>
        get(detail(readRepository.getVertriebsart(vertriebsartId))) ~
          (put | post) {
            entity(as[VertriebsartModify]) { entity =>
              val vertriebsart = entity match {
                case dl: DepotlieferungModify => copyTo[DepotlieferungModify, DepotlieferungAbotypModify](dl, "abotypId" -> abotypId)
                case hl: HeimlieferungModify => copyTo[HeimlieferungModify, HeimlieferungAbotypModify](hl, "abotypId" -> abotypId)
                case pl: PostlieferungModify => copyTo[PostlieferungModify, PostlieferungAbotypModify](pl, "abotypId" -> abotypId)
              }
              updated(vertriebsartId, vertriebsart)
            }
          } ~
          delete(remove(vertriebsartId))
      } ~
      path("abotypen" / abotypIdPath / "vertriebsarten" / vertriebsartIdPath / "lieferungen") { (abotypId, vertriebsartId) =>
        get(list(readRepository.getOffeneLieferungen(abotypId, vertriebsartId))) ~
          post {
            requestInstance { request =>
              entity(as[LieferungModify]) { entity =>
                val lieferung = copyTo[LieferungModify, LieferungAbotypCreate](entity, "abotypId" -> abotypId, "vertriebsartId" -> vertriebsartId)
                created(request)(lieferung)
              }
            }
          }
      } ~
      path("abotypen" / abotypIdPath / "vertriebsarten" / vertriebsartIdPath / "lieferungen" / lieferungIdPath) { (abotypId, vertriebsartId, lieferungId) =>
        delete(remove(lieferungId))
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

  lazy val aboRoute =
    path("abos") {
      get(list(readRepository.getAbos))
    }
}