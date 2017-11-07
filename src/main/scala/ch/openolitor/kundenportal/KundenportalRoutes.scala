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
package ch.openolitor.kundenportal

import spray.routing._
import spray.http._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Macros._
import ch.openolitor.buchhaltung.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.core.security.Subject
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.kundenportal.repositories.KundenportalReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.kundenportal.repositories.DefaultKundenportalReadRepositoryAsyncComponent

trait KundenportalRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with StammdatenEventStoreSerializer
    with BuchhaltungJsonProtocol
    with StammdatenDBMappings {
  self: KundenportalReadRepositoryAsyncComponent with FileStoreComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val projektIdPath = long2BaseIdPathMatcher(ProjektId.apply)
  implicit val aboIdPath = long2BaseIdPathMatcher(AboId.apply)
  implicit val abotypIdPath = long2BaseIdPathMatcher(AbotypId.apply)
  implicit val zusatzabotypIdPath = long2BaseIdPathMatcher(AbotypId.apply)
  implicit val abwesenheitIdPath = long2BaseIdPathMatcher(AbwesenheitId.apply)
  implicit val lieferungIdPath = long2BaseIdPathMatcher(LieferungId.apply)

  import EntityStore._

  def kundenportalRoute(implicit subject: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      pathPrefix("kundenportal") {
        abosRoute ~ rechnungenRoute ~ projektRoute ~ kontoDatenRoute
      }
    }

  def projektRoute(implicit subject: Subject) = {
    path("projekt") {
      get {
        get(detail(kundenportalReadRepository.getProjekt))
      }
    } ~
      path("projekt" / projektIdPath / "logo") { id =>
        get(download(ProjektStammdaten, "logo"))
      }
  }

  def kontoDatenRoute(implicit subject: Subject) = {
    path("kontodaten") {
      get {
        get(detail(kundenportalReadRepository.getKontoDaten))
      }
    }
  }

  def rechnungenRoute(implicit subject: Subject) = {
    path("rechnungen") {
      get {
        list(kundenportalReadRepository.getRechnungen)
      }
    } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get(detail(kundenportalReadRepository.getRechnungDetail(id)))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "downloadrechnung") { id =>
        (get)(
          onSuccess(kundenportalReadRepository.getRechnungDetail(id)) { detail =>
            detail flatMap { rechnung =>
              rechnung.fileStoreId map { fileStoreId =>
                download(GeneriertRechnung, fileStoreId)
              }
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "download" / Segment) { (id, fileStoreId) =>
        (get)(
          onSuccess(kundenportalReadRepository.getRechnungDetail(id)) { detail =>
            detail map { rechnung =>
              download(GeneriertMahnung, fileStoreId)
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
      }
  }

  def abosRoute(implicit subject: Subject, filter: Option[FilterExpr]) = {
    path("abos") {
      get {
        list(kundenportalReadRepository.getHauptabos)
      }
    } ~
      path("abos" / aboIdPath / "zusatzabos") { aboId =>
        get {
          list(kundenportalReadRepository.getZusatzabos(aboId))
        }
      } ~
      path("abos" / aboIdPath / "abwesenheiten") { aboId =>
        post {
          requestInstance { request =>
            entity(as[AbwesenheitModify]) { abw =>
              onSuccess(entityStore ? KundenportalCommandHandler.AbwesenheitErstellenCommand(subject.personId, subject, copyTo[AbwesenheitModify, AbwesenheitCreate](abw, "aboId" -> aboId))) {
                case UserCommandFailed =>
                  complete(StatusCodes.BadRequest, s"Abwesenheit konnte nicht erstellt werden.")
                case _ =>
                  complete("")
              }
            }
          }
        }
      } ~
      path("abos" / aboIdPath / "abwesenheiten" / abwesenheitIdPath) { (aboId, abwesenheitId) =>
        onSuccess(entityStore ? KundenportalCommandHandler.AbwesenheitLoeschenCommand(subject.personId, subject, aboId, abwesenheitId)) {
          case UserCommandFailed =>
            complete(StatusCodes.BadRequest, s"Abwesenheit konnte nicht gelöscht werden.")
          case _ =>
            complete("")
        }
      } ~
      path("abos" / abotypIdPath / "lieferungen") { abotypId =>
        get {
          list(kundenportalReadRepository.getLieferungenDetails(abotypId))
        }
      } ~
      path("abos" / abotypIdPath / "lieferungen" / lieferungIdPath) { (abotypId, lieferungId) =>
        get {
          get(detail(kundenportalReadRepository.getLieferungenDetail(lieferungId)))
        }
      } ~
      path("abos" / abotypIdPath / "zusatzabos" / zusatzabotypIdPath / "lieferungen") { (abotypId, zusatzabotypId) =>
        get {
          list(kundenportalReadRepository.getLieferungenDetails(zusatzabotypId))
        }
      }
  }
}

class DefaultKundenportalRoutes(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef
)
    extends KundenportalRoutes
    with DefaultKundenportalReadRepositoryAsyncComponent
