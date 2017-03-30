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
package ch.openolitor.core.system

import spray.routing._
import ch.openolitor.core.db.ConnectionPoolContextAware
import com.typesafe.scalalogging.LazyLogging
import akka.actor._
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import scala.util.Properties
import spray.json.DefaultJsonProtocol._
import java.io.ByteArrayInputStream
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.SprayDeserializers
import ch.openolitor.core.DefaultRouteService
import ch.openolitor.core.data.DataImportService
import scala.reflect.io.File
import akka.util.Timeout
import scala.concurrent.duration._
import ch.openolitor.core.data.DataImportService.ImportData
import akka.pattern.ask
import ch.openolitor.core.data.DataImportService.ImportResult
import spray.json._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.Boot
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.filestore.FileStore
import ch.openolitor.core.security.Subject
import ch.openolitor.core.repositories._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.eventsourcing._
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.parsing.UriQueryParamFilterParser

class DefaultSystemRouteService(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory,
  override val airbrakeNotifier: ActorRef
) extends SystemRouteService with DefaultCoreReadRepositoryComponent

trait SystemRouteService extends HttpService with ActorReferences
    with ConnectionPoolContextAware with SprayDeserializers
    with DefaultRouteService
    with LazyLogging
    with StatusRoutes
    with SystemJsonProtocol
    with AsyncConnectionPoolContextAware
    with PersistenceJsonProtocol {
  self: CoreReadRepositoryComponent =>

  private var error: Option[Throwable] = None
  val system: ActorSystem
  def importService(implicit subject: Subject) = {
    val serviceName = s"oo-import-service-${subject.personId.id}"
    val identifyId = 1
    (system.actorSelection(system.child(serviceName)) ? Identify(identifyId)) map {
      case ActorIdentity(`identifyId`, Some(ref)) => ref
      case ActorIdentity(`identifyId`, None) => system.actorOf(DataImportService.props(sysConfig, entityStore, system, subject.personId), serviceName)
    }
  }

  def adminRoutes(implicit subject: Subject) = pathPrefix("admin") {
    adminRoute
  }

  def handleError(er: Throwable) = {
    error = Some(er)
  }

  def adminRoute(implicit subject: Subject): Route =
    path("status") {
      get {
        error map { e =>
          complete(StatusCodes.BadRequest, e)
        } getOrElse {
          complete("Ok")
        }
      }
    } ~
      path("import") {
        post {
          entity(as[MultipartFormData]) { formData =>
            logger.debug(s"import requested")
            val file = formData.fields.collectFirst {
              case b @ BodyPart(entity, headers) if b.name == Some("file") =>
                logger.debug(s"parse file bodypart")
                val content = new ByteArrayInputStream(entity.data.toByteArray)
                val fileName = headers.find(h => h.is("content-disposition")).get.value.split("filename=").last
                (content, fileName)
            }

            val clearBeforeImport = formData.fields.collectFirst {
              case b @ BodyPart(entity, headers) if b.name == Some("clear") =>
                entity.asString.toBoolean
            }.getOrElse(false)
            logger.debug(s"File:${file.isDefined}, clearBeforeImport:$clearBeforeImport: ")

            implicit val timeout = Timeout(300.seconds)
            file.map { file =>
              onSuccess(importService.flatMap(_ ? ImportData(clearBeforeImport, file._1))) {
                case ImportResult(Some(error), _) =>
                  logger.warn(s"Couldn't import data, received error:$error")
                  complete(StatusCodes.BadRequest, error)
                case r @ ImportResult(None, result) =>
                  complete(r.toJson.compactPrint)
                case x =>
                  logger.warn(s"Couldn't import data, unexpected result:$x")
                  complete(StatusCodes.BadRequest)
              }
            }.getOrElse(complete(StatusCodes.BadRequest, "No file found"))
          }
        }
      } ~
      path("events") {
        get {
          parameters('f.?, 'limit ? 100) { (f: Option[String], limit: Int) =>
            implicit val filter = f flatMap { filterString =>
              UriQueryParamFilterParser.parse(filterString)
            }
            onSuccess(coreReadRepository.queryPersistenceJournal(limit)) {
              case result => complete(result)
            }
          }
        }
      }
}
