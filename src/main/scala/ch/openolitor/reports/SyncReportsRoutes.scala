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
package ch.openolitor.reports

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
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent.Future
import ch.openolitor.core.Macros._
import ch.openolitor.reports.eventsourcing.ReportsEventStoreSerializer
import stamina.Persister
import ch.openolitor.reports.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.filestore._
import akka.actor._
import scala.io.Source
import ch.openolitor.core.security.Subject
import ch.openolitor.util.parsing.UriQueryParamFilterParser
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.reports.repositories.DefaultReportsReadRepositorySyncComponent
import ch.openolitor.reports.repositories.ReportsReadRepositorySyncComponent
import java.io.ByteArrayInputStream
import scalikejdbc.DB

/**
 * This is using a Sync-Repository as there is no way to fetch the MetaData on the
 * scalikejdbc-async - RowDataResultSet
 * TODO Revert as soon as possible
 */
trait SyncReportsRoutes extends HttpService with ActorReferences
    with ConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging
    with ReportsJsonProtocol
    with ReportsEventStoreSerializer
    with ReportsDBMappings {
  self: ReportsReadRepositorySyncComponent with FileStoreComponent =>

  implicit val reportIdPath = long2BaseIdPathMatcher(ReportId.apply)

  import EntityStore._

  def syncReportsRoute(implicit subect: Subject) =
    parameters('f.?) { (f) =>
      implicit val filter = f flatMap { filterString =>
        UriQueryParamFilterParser.parse(filterString)
      }
      path("reports" / reportIdPath / "execute" ~ exportFormatPath.?) { (id, exportFormat) =>
        post {
          requestInstance { request =>
            entity(as[ReportExecute]) { reportExecute =>
              list(Future.successful {
                DB readOnly {
                  implicit session => reportsReadRepository.executeReport(reportExecute)
                }
              }, exportFormat)
            }
          }
        }
      }
    }

}

class DefaultSyncReportsRoutes(
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
    extends SyncReportsRoutes
    with DefaultReportsReadRepositorySyncComponent
