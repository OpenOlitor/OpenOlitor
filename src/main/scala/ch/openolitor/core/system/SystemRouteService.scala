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

class DefaultSystemRouteService(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val actorRefFactory: ActorRefFactory
) extends SystemRouteService

trait SystemRouteService extends HttpService with ActorReferences
    with ConnectionPoolContextAware with SprayDeserializers with DefaultRouteService with LazyLogging with StatusRoutes {

  private var error: Option[Throwable] = None
  val system: ActorSystem

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
            onSuccess(importService ? ImportData(clearBeforeImport, file.content)) {

            }
          }
        }
      }
}