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
package ch.openolitor.buchhaltung

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
import ch.openolitor.buchhaltung.eventsourcing.BuchhaltungEventStoreSerializer
import stamina.Persister
import ch.openolitor.buchhaltung.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._

trait BuchhaltungRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with BuchhaltungJsonProtocol
    with BuchhaltungEventStoreSerializer {
  self: BuchhaltungReadRepositoryComponent with FileStoreComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)

  import EntityStore._

  //TODO: get real userid from login
  override val personId: PersonId = Boot.systemPersonId

  lazy val buchhaltungRoute = rechnungenRoute

  lazy val rechnungenRoute =
    path("rechnungen") {
      get(list(buchhaltungReadRepository.getRechnungen)) ~
        post(create[RechnungModify, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get(detail(buchhaltungReadRepository.getRechnungDetail(id))) ~
          delete(remove(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "verschicken") { id =>
        (post)(verschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "mahnungverschicken") { id =>
        (post)(mahnungVerschicken(id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "bezahlen") { id =>
        (post)(entity(as[RechnungModifyBezahlt]) { entity => bezahlen(id, entity) })
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "stornieren") { id =>
        (post)(stornieren(id))
      }

  lazy val zahlungsImportsRoute =
    path("zahlungseingaenge") {
      get(list(buchhaltungReadRepository.getZahlungsEingaenge))
    }

  def verschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _]) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungVerschickenCommand(personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Verschickt")
      case _ =>
        complete("")
    }
  }
  def mahnungVerschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _]) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungMahnungVerschickenCommand(personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status MahnungVerschickt")
      case _ =>
        complete("")
    }
  }
  def bezahlen(id: RechnungId, entity: RechnungModifyBezahlt)(implicit idPersister: Persister[RechnungId, _]) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungBezahlenCommand(personId, id, entity)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Bezahlt")
      case _ =>
        complete("")
    }
  }
  def stornieren(id: RechnungId)(implicit idPersister: Persister[RechnungId, _]) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungStornierenCommand(personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit to status Storniert")
      case _ =>
        complete("")
    }
  }
}

class DefaultBuchhaltungRoutes(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory
)
    extends BuchhaltungRoutes
    with DefaultBuchhaltungReadRepositoryComponent
