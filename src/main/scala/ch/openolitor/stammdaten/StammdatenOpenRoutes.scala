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
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import ch.openolitor.core._
import ch.openolitor.core.db._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.reporting._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.util.parsing.UriQueryParamFilterParser

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
    with FileTypeFilenameMapping
    with StammdatenPaths {
  self: StammdatenReadRepositoryAsyncComponent with BuchhaltungReadRepositoryAsyncComponent with FileStoreComponent =>

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
    extends StammdatenOpenRoutes
    with DefaultStammdatenReadRepositoryAsyncComponent
    with DefaultBuchhaltungReadRepositoryAsyncComponent
