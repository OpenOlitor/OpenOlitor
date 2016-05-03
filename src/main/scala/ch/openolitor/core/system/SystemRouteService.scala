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
import ch.openolitor.core.models.UserId
import ch.openolitor.core.filestore.FileStore

class DefaultSystemRouteService(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory
) extends SystemRouteService

trait SystemRouteService extends HttpService with ActorReferences
    with ConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging with StatusRoutes with SystemJsonProtocol {

  private var error: Option[Throwable] = None
  val system: ActorSystem

  //TODO: get real userid from login
  override val userId: UserId = Boot.systemUserId

  val systemRoutes = statusRoute ~ adminRoutes

  val adminRoutes = pathPrefix("admin") {
    adminRoute()
  }

  def handleError(er: Throwable) = {
    error = Some(er)
  }

  def adminRoute(): Route =
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
            val file = formData.fields.collectFirst {
              case b @ BodyPart(entity, headers) if b.name == "file" =>
                val content = new ByteArrayInputStream(entity.data.toByteArray)
                val fileName = headers.find(h => h.is("content-disposition")).get.value.split("filename=").last
                (content, fileName)
            }

            val clearBeforeImport = formData.fields.find(_.name == "clear").map(_.entity.asString.toBoolean).getOrElse(true)

            val importService = system.actorOf(DataImportService.props, "oo-import-service")
            implicit val timeout = Timeout(300.seconds)
            file.map { file =>
              onSuccess(importService ? ImportData(clearBeforeImport, file._1)) {
                case ImportResult(Some(error), _) =>
                  complete(StatusCodes.BadRequest, error)
                case r @ ImportResult(None, result) =>
                  complete(r.toJson.compactPrint)
                case x =>
                  complete(StatusCodes.BadRequest)
              }
            }.getOrElse(complete(StatusCodes.BadRequest))
          }
        }
      }
}