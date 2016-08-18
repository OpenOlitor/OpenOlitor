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
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import stamina.Persister
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.BuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.DefaultBuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryComponent
import ch.openolitor.stammdaten.models.AboGuthabenModify
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.core.security.RequestFailed
import ch.openolitor.stammdaten.reporting.AuslieferungLieferscheinReportService

trait StammdatenRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with StammdatenJsonProtocol
    with StammdatenEventStoreSerializer
    with BuchhaltungJsonProtocol
    with Defaults
    with AuslieferungLieferscheinReportService
    with FileTypeFilenameMapping {
  self: StammdatenReadRepositoryComponent with BuchhaltungReadRepositoryComponent with FileStoreComponent =>

  implicit val abotypIdParamConverter = long2BaseIdConverter(AbotypId.apply)
  implicit val abotypIdPath = long2BaseIdPathMatcher(AbotypId.apply)
  implicit val kundeIdPath = long2BaseIdPathMatcher(KundeId.apply)
  implicit val pendenzIdPath = long2BaseIdPathMatcher(PendenzId.apply)
  implicit val personIdPath = long2BaseIdPathMatcher(PersonId.apply)
  implicit val kundentypIdPath = long2BaseIdPathMatcher(CustomKundentypId.apply)
  implicit val depotIdPath = long2BaseIdPathMatcher(DepotId.apply)
  implicit val aboIdPath = long2BaseIdPathMatcher(AboId.apply)
  implicit val vertriebIdPath = long2BaseIdPathMatcher(VertriebId.apply)
  implicit val vertriebsartIdPath = long2BaseIdPathMatcher(VertriebsartId.apply)
  implicit val lieferungIdPath = long2BaseIdPathMatcher(LieferungId.apply)
  implicit val lieferplanungIdPath = long2BaseIdPathMatcher(LieferplanungId.apply)
  implicit val lieferpositionIdPath = long2BaseIdPathMatcher(LieferpositionId.apply)
  implicit val bestellungIdPath = long2BaseIdPathMatcher(BestellungId.apply)
  implicit val produktIdPath = long2BaseIdPathMatcher(ProduktId.apply)
  implicit val produktekategorieIdPath = long2BaseIdPathMatcher(ProduktekategorieId.apply)
  implicit val produzentIdPath = long2BaseIdPathMatcher(ProduzentId.apply)
  implicit val tourIdPath = long2BaseIdPathMatcher(TourId.apply)
  implicit val projektIdPath = long2BaseIdPathMatcher(ProjektId.apply)
  implicit val abwesenheitIdPath = long2BaseIdPathMatcher(AbwesenheitId.apply)
  implicit val auslieferungIdPath = long2BaseIdPathMatcher(AuslieferungId.apply)
  implicit val projektVorlageIdPath = long2BaseIdPathMatcher(ProjektVorlageId.apply)
  implicit val vorlageTypePath = enumPathMatcher(VorlageTyp.apply(_) match {
    case UnknownFileType => None
    case x => Some(x)
  })

  import EntityStore._

  def stammdatenRoute(implicit subject: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      aboTypenRoute ~ kundenRoute ~ depotsRoute ~ aboRoute ~
        kundentypenRoute ~ pendenzenRoute ~ produkteRoute ~ produktekategorienRoute ~
        produzentenRoute ~ tourenRoute ~ projektRoute ~ lieferplanungRoute ~ auslieferungenRoute ~ vorlagenRoute
    }

  def kundenRoute(implicit subject: Subject) =
    path("kunden") {
      get(list(stammdatenReadRepository.getKunden)) ~
        post(create[KundeModify, KundeId](KundeId.apply _))
    } ~
      path("kunden" / kundeIdPath) { id =>
        get(detail(stammdatenReadRepository.getKundeDetail(id))) ~
          (put | post)(update[KundeModify, KundeId](id)) ~
          delete(remove(id))
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
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "abwesenheiten") { (_, aboId) =>
        post {
          requestInstance { request =>
            entity(as[AbwesenheitModify]) { abw =>
              created(request)(copyTo[AbwesenheitModify, AbwesenheitCreate](abw, "aboId" -> aboId))
            }
          }
        }
      } ~
      path("kunden" / kundeIdPath / "abos" / aboIdPath / "abwesenheiten" / abwesenheitIdPath) { (_, aboId, abwesenheitId) =>
        delete(remove(abwesenheitId))
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
      path("kunden" / kundeIdPath / "rechnungen") { (kundeId) =>
        get(list(buchhaltungReadRepository.getKundenRechnungen(kundeId)))
      }

  def kundentypenRoute(implicit subject: Subject) =
    path("kundentypen") {
      get(list(stammdatenReadRepository.getKundentypen)) ~
        post(create[CustomKundentypCreate, CustomKundentypId](CustomKundentypId.apply _))
    } ~
      path("kundentypen" / kundentypIdPath) { (kundentypId) =>
        (put | post)(update[CustomKundentypModify, CustomKundentypId](kundentypId)) ~
          delete(remove(kundentypId))
      }

  def aboTypenRoute(implicit subject: Subject) =
    path("abotypen") {
      get(list(stammdatenReadRepository.getAbotypen)) ~
        post(create[AbotypModify, AbotypId](AbotypId.apply _))
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

  def depotsRoute(implicit subject: Subject) =
    path("depots") {
      get(list(stammdatenReadRepository.getDepots)) ~
        post(create[DepotModify, DepotId](DepotId.apply _))
    } ~
      path("depots" / depotIdPath) { id =>
        get(detail(stammdatenReadRepository.getDepotDetail(id))) ~
          (put | post)(update[DepotModify, DepotId](id)) ~
          delete(remove(id))
      }

  def aboRoute(implicit subject: Subject, filter: Option[FilterExpr]) =
    path("abos") {
      get {
        list(stammdatenReadRepository.getAbos)
      }
    } ~
      path("abos" / "aktionen" / "anzahllieferungenrechnungen") {
        post {
          entity(as[AboRechnungCreate]) { rechnungCreate =>
            createAnzahlLieferungenRechnungen(rechnungCreate)
          }
        }
      } ~
      path("abos" / "aktionen" / "bisguthabenrechnungen") {
        post {
          entity(as[AboRechnungCreate]) { rechnungCreate =>
            createBisGuthabenRechnungen(rechnungCreate)
          }
        }
      }

  def pendenzenRoute(implicit subject: Subject) =
    path("pendenzen") {
      get(list(stammdatenReadRepository.getPendenzen))
    }

  def produkteRoute(implicit subject: Subject) =
    path("produkte") {
      get(list(stammdatenReadRepository.getProdukte)) ~
        post(create[ProduktModify, ProduktId](ProduktId.apply _))
    } ~
      path("produkte" / produktIdPath) { id =>
        (put | post)(update[ProduktModify, ProduktId](id)) ~
          delete(remove(id))
      }

  def produktekategorienRoute(implicit subject: Subject) =
    path("produktekategorien") {
      get(list(stammdatenReadRepository.getProduktekategorien)) ~
        post(create[ProduktekategorieModify, ProduktekategorieId](ProduktekategorieId.apply _))
    } ~
      path("produktekategorien" / produktekategorieIdPath) { id =>
        (put | post)(update[ProduktekategorieModify, ProduktekategorieId](id)) ~
          delete(remove(id))
      }

  def produzentenRoute(implicit subject: Subject) =
    path("produzenten") {
      get(list(stammdatenReadRepository.getProduzenten)) ~
        post(create[ProduzentModify, ProduzentId](ProduzentId.apply _))
    } ~
      path("produzenten" / produzentIdPath) { id =>
        get(detail(stammdatenReadRepository.getProduzentDetail(id))) ~
          (put | post)(update[ProduzentModify, ProduzentId](id)) ~
          delete(remove(id))
      }

  def tourenRoute(implicit subject: Subject) =
    path("touren") {
      get(list(stammdatenReadRepository.getTouren)) ~
        post(create[TourCreate, TourId](TourId.apply _))
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
      }

  def lieferplanungRoute(implicit subject: Subject) =
    path("lieferplanungen") {
      get(list(stammdatenReadRepository.getLieferplanungen)) ~
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
        (put | post)(update[LieferungPlanungAdd, LieferungId](lieferungId)) ~
          delete(update(lieferungId, LieferungPlanungRemove()))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "lieferungen" / lieferungIdPath / "lieferpositionen") { (lieferplanungId, lieferungId) =>
        get(list(stammdatenReadRepository.getLieferpositionen(lieferungId))) ~
          (put | post)(update[LieferpositionenModify, LieferungId](lieferungId))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "aktionen" / "abschliessen") { id =>
        (post)(lieferplanungAbschliessen(id))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "aktionen" / "verrechnen") { id =>
        (post)(lieferplanungVerrechnen(id))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "bestellungen") { lieferplanungId =>
        get(list(stammdatenReadRepository.getBestellungen(lieferplanungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "bestellungen" / bestellungIdPath / "positionen") { (lieferplanungId, bestellungId) =>
        get(list(stammdatenReadRepository.getBestellpositionen(bestellungId)))
      } ~
      path("lieferplanungen" / lieferplanungIdPath / "bestellungen" / bestellungIdPath / "aktionen" / "erneutBestellen") { (lieferplanungId, bestellungId) =>
        (post)(bestellungErneutVersenden(bestellungId))
      }

  def lieferplanungAbschliessen(id: LieferplanungId)(implicit idPersister: Persister[LieferplanungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.LieferplanungAbschliessenCommand(subject.personId, id)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not transit Lieferplanung to status Abschliessen")
      case _ =>
        stammdatenReadRepository.getBestellungen(id) map {
          _ map { bestellung =>
            onSuccess(entityStore ? StammdatenCommandHandler.BestellungAnProduzentenVersenden(subject.personId, bestellung.id)) {
              case UserCommandFailed =>
                complete(StatusCodes.BadRequest, s"Could not execute BestellungAnProduzentenVersenden on Bestellung")
              case _ =>
                complete("")
            }
          }
        }

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

  def bestellungErneutVersenden(bestellungId: BestellungId)(implicit idPersister: Persister[BestellungId, _], subject: Subject) = {
    onSuccess(entityStore ? StammdatenCommandHandler.BestellungAnProduzentenVersenden(subject.personId, bestellungId)) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Could not execute neuBestellen on Lieferung")
      case _ =>
        complete("")
    }
  }

  def auslieferungenRoute(implicit subject: Subject) =
    path("depotauslieferungen") {
      get(list(stammdatenReadRepository.getDepotAuslieferungen))
    } ~
      path("depotauslieferungen" / auslieferungIdPath) { auslieferungId =>
        get(detail(stammdatenReadRepository.getDepotAuslieferungDetail(auslieferungId)))
      } ~
      path("tourauslieferungen") {
        get(list(stammdatenReadRepository.getTourAuslieferungen))
      } ~
      path("tourauslieferungen" / auslieferungIdPath) { auslieferungId =>
        get(detail(stammdatenReadRepository.getTourAuslieferungDetail(auslieferungId))) ~
          (put | post)(update[TourAuslieferungModify, AuslieferungId](auslieferungId))
      } ~
      path("postauslieferungen") {
        get(list(stammdatenReadRepository.getPostAuslieferungen))
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
      path("depotauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageDepotLieferschein) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageDepotLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageDepotLieferschein) _)(AuslieferungId.apply)
      } ~
      path("depotauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageDepotLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageTourLieferschein) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlageTourLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageTourLieferschein) _)(AuslieferungId.apply)
      } ~
      path("tourauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlageTourLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / "berichte" / "lieferschein") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlagePostLieferschein) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / "berichte" / "lieferetiketten") {
        implicit val personId = subject.personId
        generateReport[AuslieferungId](None, generateAuslieferungLieferscheinReports(VorlagePostLieferetiketten) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / auslieferungIdPath / "berichte" / "lieferschein") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlagePostLieferschein) _)(AuslieferungId.apply)
      } ~
      path("postauslieferungen" / auslieferungIdPath / "berichte" / "lieferetiketten") { auslieferungId =>
        implicit val personId = subject.personId
        generateReport[AuslieferungId](Some(auslieferungId), generateAuslieferungLieferscheinReports(VorlagePostLieferetiketten) _)(AuslieferungId.apply)
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

  def createAnzahlLieferungenRechnungen(rechnungCreate: AboRechnungCreate)(implicit idPersister: Persister[AboId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.CreateAnzahlLieferungenRechnungenCommand(subject.personId, rechnungCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen AboIds erstellt werden.")
      case _ =>
        complete("")
    }
  }

  def createBisGuthabenRechnungen(rechnungCreate: AboRechnungCreate)(implicit idPersister: Persister[AboId, _], subject: Subject) = {
    onSuccess((entityStore ? StammdatenCommandHandler.CreateBisGuthabenRechnungenCommand(subject.personId, rechnungCreate))) {
      case UserCommandFailed =>
        complete(StatusCodes.BadRequest, s"Es konnten nicht alle Rechnungen für die gegebenen AboIds erstellt werden.")
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
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory
)
    extends StammdatenRoutes
    with DefaultStammdatenReadRepositoryComponent
    with DefaultBuchhaltungReadRepositoryComponent