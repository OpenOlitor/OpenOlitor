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

import org.joda.time.DateTime
import spray.routing._
import spray.http._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.models._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import stamina.Persister
import ch.openolitor.stammdaten.reporting._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.models.AboGuthabenModify
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr

trait StammdatenRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with StammdatenJsonProtocol
    with StammdatenEventStoreSerializer
    with BuchhaltungJsonProtocol
    with Defaults
    with AuslieferungLieferscheinReportService
    with AuslieferungEtikettenReportService
    with AuslieferungKorbUebersichtReportService
    with KundenBriefReportService
    with DepotBriefReportService
    with ProduzentenBriefReportService
    with ProduzentenabrechnungReportService
    with LieferplanungReportService
    with FileTypeFilenameMapping
    with StammdatenPaths {
  self: StammdatenReadRepositoryAsyncComponent with BuchhaltungReadRepositoryAsyncComponent with FileStoreComponent =>

  import EntityStore._

  def stammdatenRoute(implicit subject: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      kontoDatenRoute ~ aboTypenRoute ~ zusatzAboTypenRoute ~ kundenRoute ~ depotsRoute ~ aboRoute ~ personenRoute ~
        kundentypenRoute ~ pendenzenRoute ~ produkteRoute ~ produktekategorienRoute ~
        produzentenRoute ~ tourenRoute ~ projektRoute ~ lieferplanungRoute ~ auslieferungenRoute ~ lieferantenRoute ~ vorlagenRoute
    }

  def kontoDatenRoute(implicit subject: Subject) =
    path("kontodaten") {
      get(detail(stammdatenReadRepository.getKontoDaten)) ~
        post(create[KontoDatenModify, KontoDatenId](KontoDatenId.apply _))
    } ~
      path("kontodaten" / kontoDatenIdPath) { id =>
        get(detail(stammdatenReadRepository.getKontoDaten)) ~
          (put | post)(update[KontoDatenModify, KontoDatenId](id))
      }

  def kundenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("kunden" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getKundenUebersicht, exportFormat)) ~
        post(create[KundeModify, KundeId](KundeId.apply _))
    } ~
      path("kunden" / kundeIdPath) { id =>
        get(detail(stammdatenReadRepository.getKundeDetail(id))) ~
          (put | post)(update[KundeModify, KundeId](id)) ~
          delete(remove(id))
      } ~
      path("kunden" / "berichte" / "kundenbrief") {
        implicit val personId = subject.personId
        implicit val timeout = Timeout(600.seconds) //generating documents might take a lot longer
        generateReport[KundeId](None, generateKundenBriefReports(VorlageKundenbrief) _)(KundeId.apply)
      } ~
      path("kunden" / kundeIdPath / "abos") { kundeId =>
        post {
          requestInstance { request =>
            entity(as[AboModify]) {
              case dl: DepotlieferungAboModify =>
                created(request)(dl)
              case hl: HeimlieferungAboModify =>
                created(request)(hl)
              case pl: PostlieferungAboModify =>
                created(request)(pl)
            }
          }
        }
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath) { (kundeId, aboId) =>
        get(detail(stammdatenReadRepository.getAboDetail(aboId))) ~
          (put | post) {
            entity(as[AboModify]) {
              case dl: DepotlieferungAboModify =>
                updated(aboId, dl)
              case hl: HeimlieferungAboModify =>
                updated(aboId, hl)
              case pl: PostlieferungAboModify =>
                updated(aboId, pl)
            }
          } ~
          delete(remove(aboId))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "aktionen" / "guthabenanpassen") { (kundeId, aboId) =>
        (put | post)(update[AboGuthabenModify, AboId](aboId))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "aktionen" / "vertriebsartanpassen") { (kundeId, aboId) =>
        (put | post)(update[AboVertriebsartModify, AboId](aboId))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "koerbe") { (_, aboId) =>
        get(list(stammdatenReadRepository.getKoerbeLieferung(aboId)))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "abwesenheiten") { (_, aboId) =>
        post {
          requestInstance { request =>
            entity(as[AbwesenheitModify]) { abw =>
              abwesenheitCreate(copyTo[AbwesenheitModify, AbwesenheitCreate](abw, "aboId" -> aboId))
            }
          }
        }
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "abwesenheiten" / abwesenheitIdPath) { (_, aboId, abwesenheitId) =>
        deleteAbwesenheit(abwesenheitId)
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "zusatzAbos") { (kundeId, aboId) =>
        get(list(stammdatenReadRepository.getZusatzaboPerAbo(aboId))) ~
          post(create[ZusatzAboCreate, AboId](AboId.apply _))
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "zusatzAbos" / aboIdPath) { (kundeId, hauptAboId, id) =>
        get(detail(stammdatenReadRepository.getZusatzAboDetail(id))) ~
          (put | post)(update[ZusatzAboModify, AboId](id)) ~
          delete(remove(id))
      } ~
      path("kunden" / kundeIdPath / "pendenzen") { kundeId =>
        get(list(stammdatenReadRepository.getPendenzen(kundeId))) ~
          post {
            requestInstance { request =>
              entity(as[PendenzModify]) { p =>
                created(request)(copyTo[PendenzModify, PendenzCreate](p, "kundeId" -> kundeId, "generiert" -> FALSE))
              }
            }
          }
      } ~
      path("kunden" / kundeIdPath / "pendenzen" / pendenzIdPath) { (kundeId, pendenzId) =>
        get(detail(stammdatenReadRepository.getPendenzDetail(pendenzId))) ~
          (put | post)(update[PendenzModify, PendenzId](pendenzId)) ~
          delete(remove(pendenzId))
      } ~
      path("kunden" / kundeIdPath / "personen" / personIdPath) { (kundeId, personId) =>
        delete(remove(personId))
      } ~
      path("kunden" / kundeIdPath / "personen" / personIdPath / "aktionen" / "logindeaktivieren") { (kundeId, personId) =>
        (post)(disableLogin(kundeId, personId))
      } ~
      path("kunden" / kundeIdPath / "personen" / personIdPath / "aktionen" / "loginaktivieren") { (kundeId, personId) =>
        (post)(enableLogin(kundeId, personId))
      } ~
      path("kunden" / kundeIdPath / "personen" / personIdPath / "aktionen" / "einladungsenden") { (kundeId, personId) =>
        (post)(sendEinladung(kundeId, personId))
      } ~
      path("kunden" / kundeIdPath / "personen" / personIdPath / "aktionen" / "rollewechseln") { (kundeId, personId) =>
        post {
          requestInstance { request =>
            entity(as[Rolle]) { rolle =>
              changeRolle(kundeId, personId, rolle)
            }
          }
        }
      } ~
      path("kunden" / kundeIdPath / "rechnungen") { (kundeId) =>
        get(list(buchhaltungReadRepository.getKundenRechnungen(kundeId)))
      }

  def personenRoute(implicit subject: Subject, filter: Option[FilterExpr]) = {
    path("personen" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getPersonenUebersicht, exportFormat))
    }
  }

  def kundentypenRoute(implicit subject: Subject) =
    path("kundentypen") {
      get(list(stammdatenReadRepository.getCustomKundentypen)) ~
        post(create[CustomKundentypCreate, CustomKundentypId](CustomKundentypId.apply _))
    } ~
      path("kundentypen" / kundentypIdPath) { (kundentypId) =>
        (put | post)(update[CustomKundentypModify, CustomKundentypId](kundentypId)) ~
          delete(remove(kundentypId))
      }

  def aboTypenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("abotypen") {
      get(list(stammdatenReadRepository.getAbotypen)) ~
        post(create[AbotypModify, AbotypId](AbotypId.apply _))
    } ~
      path("abotypen" / "personen" / "alle") {
        get(list(stammdatenReadRepository.getPersonenByAbotypen))
      } ~
      path("abotypen" / "personen" / "aktiv") {
        get(list(stammdatenReadRepository.getPersonenAboAktivByAbotypen))
      } ~
      path("abotypen" / abotypIdPath) { id =>
        get(detail(stammdatenReadRepository.getAbotypDetail(id))) ~
          (put | post)(update[AbotypModify, AbotypId](id)) ~
          delete(remove(id))
      } ~
      path("abotypen" / abotypIdPath / "vertriebe") { abotypId =>
        get(list(stammdatenReadRepository.getVertriebe(abotypId))) ~
          post(create[VertriebModify, VertriebId](VertriebId.apply _))
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath) { (abotypId, vertriebId) =>
        get(detail(stammdatenReadRepository.getVertrieb(vertriebId))) ~
          (put | post)(update[VertriebModify, VertriebId](vertriebId)) ~
          delete(remove(vertriebId))
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath / "vertriebsarten") { (abotypId, vertriebId) =>
        get(list(stammdatenReadRepository.getVertriebsarten(vertriebId))) ~
          post {
            requestInstance { request =>
              entity(as[VertriebsartModify]) {
                case dl: DepotlieferungModify =>
                  created(request)(copyTo[DepotlieferungModify, DepotlieferungAbotypModify](dl, "vertriebId" -> vertriebId))
                case hl: HeimlieferungModify =>
                  created(request)(copyTo[HeimlieferungModify, HeimlieferungAbotypModify](hl, "vertriebId" -> vertriebId))
                case pl: PostlieferungModify =>
                  created(request)(copyTo[PostlieferungModify, PostlieferungAbotypModify](pl, "vertriebId" -> vertriebId))
              }
            }
          }
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath / "vertriebsarten" / vertriebsartIdPath) { (abotypId, vertriebId, vertriebsartId) =>
        get(detail(stammdatenReadRepository.getVertriebsart(vertriebsartId))) ~
          (put | post) {
            entity(as[VertriebsartModify]) {
              case dl: DepotlieferungModify =>
                updated(vertriebsartId, copyTo[DepotlieferungModify, DepotlieferungAbotypModify](dl, "vertriebId" -> vertriebId))
              case hl: HeimlieferungModify =>
                updated(vertriebsartId, copyTo[HeimlieferungModify, HeimlieferungAbotypModify](hl, "vertriebId" -> vertriebId))
              case pl: PostlieferungModify =>
                updated(vertriebsartId, copyTo[PostlieferungModify, PostlieferungAbotypModify](pl, "vertriebId" -> vertriebId))
            }
          } ~
          delete(remove(vertriebsartId))
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath / "lieferungen") { (abotypId, vertriebId) =>
        get(list(stammdatenReadRepository.getUngeplanteLieferungen(abotypId, vertriebId))) ~
          post {
            requestInstance { request =>
              entity(as[LieferungAbotypCreate]) { entity =>
                created(request)(entity)
              }
            }
          }
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath / "lieferungen" / "aktionen" / "generieren") { (abotypId, vertriebId) =>
        post {
          requestInstance { request =>
            entity(as[LieferungenAbotypCreate]) { entity =>
              created(request)(entity)
            }
          }
        }
      } ~
      path("abotypen" / abotypIdPath / "vertriebe" / vertriebIdPath / "lieferungen" / lieferungIdPath) { (abotypId, vertriebId, lieferungId) =>
        delete(remove(lieferungId))
      }

  def zusatzAboTypenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("zusatzAbotypen") {
      get(list(stammdatenReadRepository.getZusatzAbotypen)) ~
        post(create[ZusatzAbotypModify, AbotypId](AbotypId.apply))
    } ~
      path("zusatzAbotypen" / zusatzAbotypIdPath) { id =>
        get(detail(stammdatenReadRepository.getZusatzAbotypDetail(id))) ~
          (put | post)(update[ZusatzAbotypModify, AbotypId](id)) ~
          delete(remove(id))
      }

  def depotsRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("depots") {
      get(list(stammdatenReadRepository.getDepots)) ~
        post(create[DepotModify, DepotId](DepotId.apply _))
    } ~
      path("depots" / "personen" / "alle") {
        get(list(stammdatenReadRepository.getPersonenByDepots))
      } ~
      path("depots" / "personen" / "aktiv") {
        get(list(stammdatenReadRepository.getPersonenAboAktivByDepots))
      } ~
      path("depots" / "berichte" / "depotbrief") {
        implicit val personId = subject.personId
        implicit val timeout = Timeout(600.seconds) //generating documents might take a lot longer
        generateReport[DepotId](None, generateDepotBriefReports(VorlageDepotbrief) _)(DepotId.apply)
      } ~
      path("depots" / depotIdPath) { id =>
        get(detail(stammdatenReadRepository.getDepotDetail(id))) ~
          (put | post)(update[DepotModify, DepotId](id)) ~
          delete(remove(id))
      }

  def aboRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("abos" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getAbos, exportFormat))
    } ~
      path("abos" / "aktionen" / "anzahllieferungenrechnungspositionen") {
        post {
          entity(as[AboRechnungsPositionBisAnzahlLieferungenCreate]) { rechnungCreate =>
            createAnzahlLieferungenRechnungsPositionen(rechnungCreate)
          }
        }
      } ~
      path("abos" / "aktionen" / "bisguthabenrechnungspositionen") {
        post {
          entity(as[AboRechnungsPositionBisGuthabenCreate]) { rechnungCreate =>
            createBisGuthabenRechnungsPositionen(rechnungCreate)
          }
        }
      }

  def pendenzenRoute(implicit subject: Subject) =
    path("pendenzen" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getPendenzen, exportFormat))
    } ~
      path("pendenzen" / pendenzIdPath) { pendenzId =>
        (put | post)(update[PendenzModify, PendenzId](pendenzId))
      }

  def produkteRoute(implicit subject: Subject) =
    path("produkte" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getProdukte, exportFormat)) ~
        post(create[ProduktModify, ProduktId](ProduktId.apply _))
    } ~
      path("produkte" / produktIdPath) { id =>
        (put | post)(update[ProduktModify, ProduktId](id)) ~
          delete(remove(id))
      }

  def produktekategorienRoute(implicit subject: Subject) =
    path("produktekategorien" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getProduktekategorien, exportFormat)) ~
        post(create[ProduktekategorieModify, ProduktekategorieId](ProduktekategorieId.apply _))
    } ~
      path("produktekategorien" / produktekategorieIdPath) { id =>
        (put | post)(update[ProduktekategorieModify, ProduktekategorieId](id)) ~
          delete(remove(id))
      }

  def produzentenRoute(implicit subject: Subject) =
    path("produzenten" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getProduzenten, exportFormat)) ~
        post(create[ProduzentModify, ProduzentId](ProduzentId.apply _))
    } ~
      path("produzenten" / produzentIdPath) { id =>
        get(detail(stammdatenReadRepository.getProduzentDetail(id))) ~
          (put | post)(update[ProduzentModify, ProduzentId](id)) ~
          delete(remove(id))
      } ~
      path("produzenten" / "berichte" / "produzentenbrief") {
        implicit val personId = subject.personId
        implicit val timeout = Timeout(600.seconds) //generating documents might take a lot longer
        generateReport[ProduzentId](None, generateProduzentenBriefReports(VorlageProduzentenbrief) _)(ProduzentId.apply)
      }

  def tourenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("touren" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getTouren, exportFormat)) ~
        post(create[TourCreate, TourId](TourId.apply _))
    } ~
      path("touren" / "personen" / "alle") {
        get(list(stammdatenReadRepository.getPersonenByTouren))
      } ~
      path("touren" / "personen" / "aktiv") {
        get(list(stammdatenReadRepository.getPersonenAboAktivByTouren))
      } ~
      path("touren" / tourIdPath) { id =>
        get(detail(stammdatenReadRepository.getTourDetail(id))) ~
          (put | post)(update[TourModify, TourId](id)) ~
          delete(remove(id))
      }

  def projektRoute(implicit subject: Subject) =
    path("projekt") {
      get(detail(stammdatenReadRepository.getProjekt)) ~
        post(create[ProjektModify, ProjektId](ProjektId.apply _))
    } ~
      path("projekt" / projektIdPath) { id =>
        get(detail(stammdatenReadRepository.getProjekt)) ~
          (put | post)(update[ProjektModify, ProjektId](id))
      } ~
      path("projekt" / projektIdPath / "logo") { id =>
        get(download(ProjektStammdaten, "logo")) ~
          (put | post)(uploadStored(ProjektStammdaten, Some("logo")) { (id, metadata) =>
            //TODO: update projekt stammdaten entity
            complete("Logo uploaded")
          })
      } ~
      path("projekt" / projektIdPath / "style-admin") { id =>
        get(download(ProjektStammdaten, "style-admin")) ~
          (put | post)(uploadStored(ProjektStammdaten, Some("style-admin")) { (id, metadata) =>
            complete("Style 'style-admin' uploaded")
          })
      } ~
      path("projekt" / projektIdPath / "style-kundenportal") { id =>
        get(download(ProjektStammdaten, "style-kundenportal")) ~
          (put | post)(uploadStored(ProjektStammdaten, Some("style-kundenportal")) { (id, metadata) =>
            complete("Style 'style-kundenportal' uploaded")
          })
      }

  def lieferplanungRoute(implicit subject: Subject) =
    path("lieferplanungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getLieferplanungen, exportFormat)) ~
        post(create[LieferplanungCreate, LieferplanungId](LieferplanungId.apply _))
    } ~
      path("lieferplanungen" / lieferplanungIdPath) { id =>
        get(detail(stammdatenReadRepository.getLieferplanung(id))) ~
          (put | post)(update[LieferplanungModify, LieferplanungId](id)) ~
          delete(remove(id))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "lieferungen") { lieferplanungId =>
        get(list(stammdatenReadRepository.getLieferungenDetails(lieferplanungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "getVerfuegbareLieferungen") { lieferplanungId =>
        get(list(stammdatenReadRepository.getVerfuegbareLieferungen(lieferplanungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "lieferungen" / lieferungIdPath) { (lieferplanungId, lieferungId) =>
        (put | post)(create[LieferungPlanungAdd, LieferungId]((x: Long) => lieferungId)) ~
          delete(remove(lieferungId.getLieferungOnLieferplanungId()))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / korbStatusPath / "aboIds") { (lieferplanungId, korbStatus) =>
        get(list(stammdatenReadRepository.getAboIds(lieferplanungId, korbStatus)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "auslieferungen") { (lieferplanungId) =>
        get(list(stammdatenReadRepository.getAuslieferungen(lieferplanungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "lieferungen" / lieferungIdPath / korbStatusPath / "aboIds") { (lieferplanungId, lieferungId, korbStatus) =>
        get(list(stammdatenReadRepository.getAboIds(lieferungId, korbStatus)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "lieferungen" / lieferungIdPath / korbStatusPath / "hauptaboIds") { (lieferplanungId, lieferungId, korbStatus) =>
        get(list(stammdatenReadRepository.getZusatzaboIds(lieferungId, korbStatus)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "aktionen" / "abschliessen") { id =>
        (post)(lieferplanungAbschliessen(id))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "aktionen" / "modifizieren") { id =>
        post {
          requestInstance { request =>
            entity(as[LieferplanungPositionenModify]) { lieferplanungModify =>
              lieferplanungModifizieren(lieferplanungModify)
            }
          }
        }
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "aktionen" / "verrechnen") { id =>
        (post)(lieferplanungVerrechnen(id))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "sammelbestellungen") { lieferplanungId =>
        get(list(stammdatenReadRepository.getSammelbestellungen(lieferplanungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "sammelbestellungen" / sammelbestellungIdPath / "bestellungen" / bestellungIdPath / "positionen") { (lieferplanungId, sammelbestellungId, bestellungId) =>
        get(list(stammdatenReadRepository.getBestellpositionen(bestellungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "sammelbestellungen" / sammelbestellungIdPath / "aktionen" / "erneutBestellen") { (lieferplanungId, sammelbestellungId) =>
        (post)(sammelbestellungErneutVersenden(sammelbestellungId))
      } ~
      path("lieferplanungen" / "berichte" / "lieferplanung") {
        implicit val personId = subject.personId
        generateReport[LieferplanungId](None, generateLieferplanungReports(VorlageLieferplanung) _)(LieferplanungId.apply)
      }

  def lieferplanungAbschliessen(id: LieferplanungId)(implicit idPersister: Persister[LieferplanungId, _], subject: Subject) = {
    implicit val timeout = Timeout(30.seconds)
    onSuccess(entityStore ? StammdatenCommandHandler.LieferplanungAbschliessenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit Lieferplanung to status Abschliessen")
      case _ =>
        // TODO OO-589
        if (false) {
          stammdatenReadRepository.getSammelbestellungen(id) map {
            _ map { sammelbestellung =>
              onSuccess(entityStore ? StammdatenCommandHandler.SammelbestellungAnProduzentenVersendenCommand(subject.personId, sammelbestellung.id)) {
                case UserCommandFailed =>
                  complete(StatusCodes.BadRequest, s"Could not execute SammelbestellungAnProduzentenVersenden on Bestellung")
                case _ =>
                  complete("")
              }
            }
          }
        } else {
          complete("")
        }

        complete("")
    }
  }

  def lieferplanungModifizieren(lieferplanungModify: LieferplanungPositionenModify)(implicit idPersister: Persister[LieferplanungId, _], subject: Subject) = {
    implicit val timeout = Timeout(30.seconds)
    onSuccess(entityStore ? StammdatenCommandHandler.LieferplanungModifyCommand(subject.personId, lieferplanungModify)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not modify Lieferplanung")
      case _ =>
        complete("")
    }
  }

  def abwesenheitCreate(abw: AbwesenheitCreate)(implicit idPersister: Persister[AbwesenheitId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.AbwesenheitCreateCommand(subject.personId, abw)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not store Abwesenheit")
      case _ =>
        complete("")
    }
  }

  def lieferplanungVerrechnen(id: LieferplanungId)(implicit idPersister: Persister[LieferplanungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.LieferplanungAbrechnenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit Lieferplanung to status Verrechnet")
      case _ =>
        complete("")
    }
  }

  def sammelbestellungErneutVersenden(id: SammelbestellungId)(implicit idPersister: Persister[SammelbestellungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.SammelbestellungAnProduzentenVersendenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not execute neuBestellen on Lieferung")
      case _ =>
        complete("")
    }
  }

  def lieferantenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("lieferanten" / "sammelbestellungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getSammelbestellungen, exportFormat))
    } ~
      path("lieferanten" / "sammelbestellungen" / "aktionen" / "abgerechnet") {
        post {
          requestInstance { request =>
            entity(as[SammelbestellungAusgeliefert]) { entity =>
              sammelbestellungenAlsAbgerechnetMarkieren(entity.datum, entity.ids)
            }
          }
        }
      } ~
      path("lieferanten" / "sammelbestellungen" / sammelbestellungIdPath) { (sammelbestellungId) =>
        get(list(stammdatenReadRepository.getSammelbestellungDetail(sammelbestellungId)))
      } ~
      path("lieferanten" / "sammelbestellungen" / "berichte" / "abrechnung") {
        implicit val personId = subject.personId
        generateReport[SammelbestellungId](None, generateProduzentenabrechnungReports(VorlageProduzentenabrechnung) _)(SammelbestellungId.apply)
      }

  def sammelbestellungenAlsAbgerechnetMarkieren(datum: DateTime, ids: Seq[SammelbestellungId])(implicit idPersister: Persister[SammelbestellungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.SammelbestellungenAlsAbgerechnetMarkierenCommand(subject.personId, datum, ids)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Bestellungen konnten nicht als abgerechnet markiert werden.")
      case _ =>
        complete("")
    }
  }

  def auslieferungenRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("depotauslieferungen" ~ exportFormatPath.?) { exportFormat =>
      get(list(stammdatenReadRepository.getDepotAuslieferungen, exportFormat))
    } ~
      path("depotauslieferungen" / auslieferungIdPath) { auslieferungId =>
        get(detail(stammdatenReadRepository.getDepotAuslieferungDetail(auslieferungId)))
      } ~
      path("tourauslieferungen" ~ exportFormatPath.?) { exportFormat =>
        get(list(stammdatenReadRepository.getTourAuslieferungen, exportFormat))
      } ~
      path("tourauslieferungen" / auslieferungIdPath) { auslieferungId =>
        get(detail(stammdatenReadRepository.getTourAuslieferungDetail(auslieferungId))) ~
          (put | post)(update[TourAuslieferungModify, AuslieferungId](auslieferungId))
      } ~
      path("postauslieferungen" ~ exportFormatPath.?) { exportFormat =>
        get(list(stammdatenReadRepository.getPostAuslieferungen, exportFormat))
      } ~
      path("postauslieferungen" / auslieferungIdPath) { auslieferungId =>
        get(detail(stammdatenReadRepository.getPostAuslieferungDetail(auslieferungId)))
      } ~
      path("(depot|tour|post)auslieferungen".r / "aktionen" / "ausliefern") { _ =>
        auslieferungenAlsAusgeliefertMarkierenRoute
      } ~
      path("(depot|tour|post)auslieferungen".r / auslieferungIdPath / "aktionen" / "ausliefern") { (prefix, auslieferungId) =>
        post {
          auslieferungenAlsAusgeliefertMarkieren(Seq(auslieferungId))
        }
      } ~
      path("(depot|tour|post)auslieferungen".r / "berichte" / "korbuebersicht") { _ =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungKorbUebersichtReports(VorlageKorbUebersicht) _)(AuslieferungId.apply)
      } ~
      path("(depot|tour|post)auslieferungen".r / auslieferungIdPath / "berichte" / "korbuebersicht") { (_, auslieferungId) =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungKorbUebersichtReports(VorlageKorbUebersicht) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageDepotLieferschein) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungEtikettenReports(VorlageDepotLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageDepotLieferschein) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungEtikettenReports(VorlageDepotLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageTourLieferschein) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungEtikettenReports(VorlageTourLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageTourLieferschein) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungEtikettenReports(VorlageTourLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlagePostLieferschein) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungEtikettenReports(VorlagePostLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlagePostLieferschein) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungEtikettenReports(VorlagePostLieferetiketten) _)(AuslieferungId.apply)
      }

  def auslieferungenAlsAusgeliefertMarkierenRoute(implicit subject: Subject) =
    post {
      requestInstance { request =>
        entity(as[Seq[AuslieferungId]]) { ids =>
          auslieferungenAlsAusgeliefertMarkieren(ids)
        }
      }
    }

  def auslieferungenAlsAusgeliefertMarkieren(ids: Seq[AuslieferungId])(implicit idPersister: Persister[AuslieferungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.AuslieferungenAlsAusgeliefertMarkierenCommand(subject.personId, ids)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Auslieferungen konnten nicht als ausgeliefert markiert werden.")
      case _ =>
        complete("")
    }
  }

  def deleteAbwesenheit(abwesenheitId: AbwesenheitId)(implicit idPersister: Persister[AuslieferungId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.DeleteAbwesenheitCommand(subject.personId, abwesenheitId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Abwesenheit kann nicht gelöscht werden. Die Lieferung ist bereits abgeschlossen.")
      case _ =>
        complete("")
    }
  }

  def createAnzahlLieferungenRechnungsPositionen(rechnungCreate: AboRechnungsPositionBisAnzahlLieferungenCreate)(implicit idPersister: Persister[AboId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.CreateAnzahlLieferungenRechnungsPositionenCommand(subject.personId, rechnungCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen AboIds erstellt werden.")
      case _ =>
        complete("")
    }
  }

  def createBisGuthabenRechnungsPositionen(rechnungCreate: AboRechnungsPositionBisGuthabenCreate)(implicit idPersister: Persister[AboId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.CreateBisGuthabenRechnungsPositionenCommand(subject.personId, rechnungCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen AboIds erstellt werden.")
      case _ =>
        complete("")
    }
  }

  def disableLogin(kundeId: KundeId, personId: PersonId)(implicit idPersister: Persister[KundeId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.LoginDeaktivierenCommand(subject.personId, kundeId, personId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Das Login konnte nicht deaktiviert werden.")
      case _ =>
        complete("")
    }
  }

  def enableLogin(kundeId: KundeId, personId: PersonId)(implicit idPersister: Persister[KundeId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.LoginAktivierenCommand(subject.personId, kundeId, personId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Das Login konnte nicht aktiviert werden.")
      case _ =>
        complete("")
    }
  }

  def sendEinladung(kundeId: KundeId, personId: PersonId)(implicit idPersister: Persister[KundeId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.EinladungSendenCommand(subject.personId, kundeId, personId))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Einladung konnte nicht gesendet werden.")
      case _ =>
        complete("")
    }
  }

  def changeRolle(kundeId: KundeId, personId: PersonId, rolle: Rolle)(implicit idPersister: Persister[KundeId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.RolleWechselnCommand(subject.personId, kundeId, personId, rolle))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Die Rolle der Person konnte nicht gewechselt werden.")
      case _ =>
        complete("")
    }
  }

  def vorlagenRoute(implicit subject: Subject) =
    path("vorlagetypen") {
      get {
        complete(VorlageTyp.AlleVorlageTypen.map(_.asInstanceOf[VorlageTyp]))
      }
    } ~
      path("vorlagen") {
        get(list(stammdatenReadRepository.getProjektVorlagen)) ~
          post(create[ProjektVorlageCreate, ProjektVorlageId](ProjektVorlageId.apply _))
      } ~
      //Standardvorlagen
      path("vorlagen" / vorlageTypePath / "dokument") { vorlageType =>
        get(tryDownload(vorlageType, defaultFileTypeId(vorlageType)) { _ =>
          //Return vorlage from resources
          fileTypeResourceAsStream(vorlageType, None) match {
            case Left(resource) =>
              complete(StatusCodes.BadRequest, s"Vorlage konnte im folgenden Pfad nicht gefunden werden: $resource")
            case Right(is) => {
              val name = vorlageType.toString
              respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", name))))(stream(is))
            }
          }
        }) ~
          (put | post)(uploadStored(vorlageType, Some(defaultFileTypeId(vorlageType))) { (id, metadata) =>
            complete("Standardvorlage gespeichert")
          })
      } ~
      //Projektvorlagen
      path("vorlagen" / projektVorlageIdPath) { id =>
        (put | post)(update[ProjektVorlageModify, ProjektVorlageId](id)) ~
          //TODO: remove from filestore as well
          delete(remove(id))
      } ~
      path("vorlagen" / projektVorlageIdPath / "dokument") { id =>
        get {
          onSuccess(stammdatenReadRepository.getProjektVorlage(id)) {
            case Some(vorlage) if vorlage.fileStoreId.isDefined =>
              download(vorlage.typ, vorlage.fileStoreId.get)
            case Some(vorlage) =>
              complete(StatusCodes.BadRequest, s"Bei dieser Projekt-Vorlage ist kein Dokument hinterlegt: $id")
            case None =>
              complete(StatusCodes.NotFound, s"Projekt-Vorlage nicht gefunden: $id")
          }
        } ~
          (put | post) {
            onSuccess(stammdatenReadRepository.getProjektVorlage(id)) {
              case Some(vorlage) =>
                val fileStoreId = vorlage.fileStoreId.getOrElse(generateFileStoreId(vorlage))
                uploadStored(vorlage.typ, Some(fileStoreId)) { (storeFileStoreId, metadata) =>
                  updated(id, ProjektVorlageUpload(storeFileStoreId))
                }
              case None =>
                complete(StatusCodes.NotFound, s"Projekt-Vorlage nicht gefunden: $id")
            }
          }
      }

  private def generateFileStoreId(vorlage: ProjektVorlage) = {
    vorlage.name.replace(" ", "_") + ".odt"
  }
}

class DefaultStammdatenRoutes(
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
    extends StammdatenRoutes
    with DefaultStammdatenReadRepositoryAsyncComponent
    with DefaultBuchhaltungReadRepositoryAsyncComponent
