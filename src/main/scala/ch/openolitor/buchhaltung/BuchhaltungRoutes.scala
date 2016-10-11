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
import scala.io.Source
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportParser
import ch.openolitor.buchhaltung.zahlungsimport.ZahlungsImportRecordResult
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryComponent
import ch.openolitor.buchhaltung.reporting.RechnungReportService
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.reporting.MahnungReportService

trait BuchhaltungRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with BuchhaltungJsonProtocol
    with BuchhaltungEventStoreSerializer
    with RechnungReportService
    with MahnungReportService
    with BuchhaltungDBMappings {
  self: BuchhaltungReadRepositoryComponent with FileStoreComponent with StammdatenReadRepositoryComponent =>

  implicit val rechnungIdPath = long2BaseIdPathMatcher(RechnungId.apply)
  implicit val zahlungsImportIdPath = long2BaseIdPathMatcher(ZahlungsImportId.apply)
  implicit val zahlungsEingangIdPath = long2BaseIdPathMatcher(ZahlungsEingangId.apply)

  import EntityStore._

  def buchhaltungRoute(implicit subect: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      rechnungenRoute ~ zahlungsImportsRoute
    }

  def rechnungenRoute(implicit subect: Subject, filter: Option[FilterExpr]) =
    path("rechnungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(buchhaltungReadRepository.getRechnungen, exportFormat)) ~
        post(create[RechnungCreate, RechnungId](RechnungId.apply _))
    } ~
      path("rechnungen" / "aktionen" / "downloadrechnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.fileStoreId.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download rechnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Rechnungen_" + System.currentTimeMillis + ".zip", GeneriertRechnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "downloadmahnungen") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              onSuccess(buchhaltungReadRepository.getByIds(rechnungMapping, cont.ids)) { rechnungen =>
                val fileStoreIds = rechnungen.map(_.mahnungFileStoreIds.map(FileStoreFileId(_))).flatten
                logger.debug(s"Download mahnungen with filestoreRefs:$fileStoreIds")
                downloadAll("Mahnungen_" + System.currentTimeMillis + ".zip", GeneriertMahnung, fileStoreIds)
              }
            }
          }
        }
      } ~
      path("rechnungen" / "aktionen" / "verschicken") {
        post {
          requestInstance { request =>
            entity(as[RechnungenContainer]) { cont =>
              verschicken(cont.ids)
            }
          }
        }
      } ~
      path("rechnungen" / "berichte" / "rechnungen") {
        (post)(rechnungBerichte())
      } ~
      path("rechnungen" / "berichte" / "mahnungen") {
        (post)(mahnungBerichte())
      } ~
      path("rechnungen" / rechnungIdPath) { id =>
        get(detail(buchhaltungReadRepository.getRechnungDetail(id))) ~
          delete(remove(id)) ~
          (put | post)(update[RechnungModify, RechnungId](id))
      } ~
      path("rechnungen" / rechnungIdPath / "aktionen" / "downloadrechnung") { id =>
        (get)(
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
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
          onSuccess(buchhaltungReadRepository.getRechnungDetail(id)) { detail =>
            detail map { rechnung =>
              download(GeneriertMahnung, fileStoreId)
            } getOrElse (complete(StatusCodes.BadRequest))
          }
        )
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
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "rechnung") { id =>
        (post)(rechnungBericht(id))
      } ~
      path("rechnungen" / rechnungIdPath / "berichte" / "mahnung") { id =>
        (post)(mahnungBericht(id))
      }

  def zahlungsImportsRoute(implicit subect: Subject) =
    path("zahlungsimports") {
      get(list(buchhaltungReadRepository.getZahlungsImports)) ~
        (put | post)(upload { (form, content, fileName) =>
          // parse
          ZahlungsImportParser.parse(Source.fromInputStream(content).getLines) match {
            case Success(importResult) =>
              storeToFileStore(ZahlungsImportDaten, None, content, fileName) { (fileId, meta) =>
                createZahlungsImport(fileId, importResult.records)
              }
            case Failure(e) => complete(StatusCodes.BadRequest, s"Die Datei konnte nicht gelesen werden: $e")
          }
        })
    } ~
      path("zahlungsimports" / zahlungsImportIdPath) { id =>
        get(detail(buchhaltungReadRepository.getZahlungsImportDetail(id)))
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / zahlungsEingangIdPath / "aktionen" / "erledigen") { (_, zahlungsEingangId) =>
        post(entity(as[ZahlungsEingangModifyErledigt]) { entity => zahlungsEingangErledigen(entity) })
      } ~
      path("zahlungsimports" / zahlungsImportIdPath / "zahlungseingaenge" / "aktionen" / "automatischerledigen") { id =>
        post(entity(as[Seq[ZahlungsEingangModifyErledigt]]) { entities => zahlungsEingaengeErledigen(entities) })
      }

  def verschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def verschicken(ids: Seq[RechnungId])(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungenVerschickenCommand(subject.personId, ids)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten keine Rechnungen in den Status 'Verschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def mahnungVerschicken(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungMahnungVerschickenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'MahnungVerschickt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def bezahlen(id: RechnungId, entity: RechnungModifyBezahlt)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungBezahlenCommand(subject.personId, id, entity)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def stornieren(id: RechnungId)(implicit idPersister: Persister[RechnungId, _], subject: Subject) = {
    onSuccess(entityStore ? BuchhaltungCommandHandler.RechnungStornierenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Storniert' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def createZahlungsImport(file: String, zahlungsEingaenge: Seq[ZahlungsImportRecordResult])(implicit idPersister: Persister[ZahlungsImportId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsImportCreateCommand(subject.personId, file, zahlungsEingaenge))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Rechnung konnte nicht in den Status 'Bezahlt' gesetzt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingangErledigen(entity: ZahlungsEingangModifyErledigt)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingangErledigenCommand(subject.personId, entity))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Der Zahlungseingang konnte nicht erledigt werden")
      case _ =>
        complete("")
    }
  }

  def zahlungsEingaengeErledigen(entities: Seq[ZahlungsEingangModifyErledigt])(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    onSuccess((entityStore ? BuchhaltungCommandHandler.ZahlungsEingaengeErledigenCommand(subject.personId, entities))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Zahlungseingänge erledigt werden")
      case _ =>
        complete("")
    }
  }

  def rechnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateRechnungReports _)(RechnungId.apply)
  }

  def rechnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateRechnungReports _)(RechnungId.apply)
  }

  def mahnungBericht(id: RechnungId)(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](Some(id), generateMahnungReports _)(RechnungId.apply)
  }

  def mahnungBerichte()(implicit idPersister: Persister[ZahlungsEingangId, _], subject: Subject) = {
    implicit val personId = subject.personId
    generateReport[RechnungId](None, generateMahnungReports _)(RechnungId.apply)
  }
}

class DefaultBuchhaltungRoutes(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef
)
    extends BuchhaltungRoutes
    with DefaultBuchhaltungReadRepositoryComponent
    with DefaultStammdatenReadRepositoryComponent
