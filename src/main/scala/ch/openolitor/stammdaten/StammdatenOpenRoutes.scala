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
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.reporting._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryComponent
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.core.security.Subject
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.models.AboGuthabenModify
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.core.security.RequestFailed

trait StammdatenOpenRoutes extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with StammdatenJsonProtocol
    with StammdatenEventStoreSerializer
    with BuchhaltungJsonProtocol
    with Defaults
    with AuslieferungLieferscheinReportService
    with AuslieferungEtikettenReportService
    with KundenBriefReportService
    with DepotBriefReportService
    with ProduzentenBriefReportService
    with ProduzentenabrechnungReportService
    with FileTypeFilenameMapping {
  self: StammdatenReadRepositoryComponent with BuchhaltungReadRepositoryComponent with FileStoreComponent =>

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
  implicit val sammelbestellungIdPath = long2BaseIdPathMatcher(SammelbestellungId.apply)
  implicit val produktIdPath = long2BaseIdPathMatcher(ProduktId.apply)
  implicit val produktekategorieIdPath = long2BaseIdPathMatcher(ProduktekategorieId.apply)
  implicit val produzentIdPath = long2BaseIdPathMatcher(ProduzentId.apply)
  implicit val tourIdPath = long2BaseIdPathMatcher(TourId.apply)
  implicit val projektIdPath = long2BaseIdPathMatcher(ProjektId.apply)
  implicit val abwesenheitIdPath = long2BaseIdPathMatcher(AbwesenheitId.apply)
  implicit val auslieferungIdPath = long2BaseIdPathMatcher(AuslieferungId.apply)
  implicit val projektVorlageIdPath = long2BaseIdPathMatcher(ProjektVorlageId.apply)
  implicit val korbStatusPath = enumPathMatcher(KorbStatus.apply(_) match {
    case x => Some(x)
  })
  implicit val vorlageTypePath = enumPathMatcher(VorlageTyp.apply(_) match {
    case UnknownFileType => None
    case x => Some(x)
  })

  import EntityStore._

  def stammdatenOpenRoute =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      pathPrefix("open") {
        projectsRoute ~ lieferplanungRoute
      }

    }

  def projectsRoute =
    path("projekt") {
      get(detail(stammdatenReadRepository.getProjektPublik))
    } ~
      path("projekt" / projektIdPath / "logo") { id =>
        get(download(ProjektStammdaten, "logo"))
      }

  def lieferplanungRoute =
    path("lastlieferplanungen") {
      get(list(stammdatenReadRepository.getLastClosedLieferplanungenDetail))
    }
}

class DefaultStammdatenOpenRoutes(
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
    extends StammdatenOpenRoutes
    with DefaultStammdatenReadRepositoryComponent
    with DefaultBuchhaltungReadRepositoryComponent
