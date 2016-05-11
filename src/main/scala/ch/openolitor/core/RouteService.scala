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
package ch.openolitor.core

import akka.actor._
import ch.openolitor.helloworld.HelloWorldRoutes
import ch.openolitor.stammdaten.StammdatenRoutes
import ch.openolitor.stammdaten.DefaultStammdatenRoutes
import spray.routing.HttpService
import ch.openolitor.core._
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import java.util.UUID
import ch.openolitor.core.domain._
import ch.openolitor.core.domain.EntityStore._
import akka.pattern.ask
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http._
import spray.http.StatusCodes
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpHeaders.Location
import spray.json._
import ch.openolitor.core.BaseJsonProtocol._
import ch.openolitor.stammdaten.FileStoreRoutes
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spray.routing.Route
import scala.util._
import ch.openolitor.core.BaseJsonProtocol.IdResponse
import stamina.Persister
import stamina.json.JsonPersister
import ch.openolitor.core.system.StatusRoutes
import java.io.ByteArrayInputStream
import ch.openolitor.core.filestore._
import spray.routing.StandardRoute
import akka.util.ByteString
import ch.openolitor.stammdaten.StreamSupport
import scala.reflect.ClassTag
import ch.openolitor.buchhaltung.BuchhaltungRoutes
import ch.openolitor.buchhaltung.DefaultBuchhaltungRoutes
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.system.DefaultSystemRouteService
import ch.openolitor.core.system.SystemRouteService
import spray.routing.RequestContext
import java.io.InputStream

object RouteServiceActor {
  def props(entityStore: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props =
    Props(classOf[DefaultRouteServiceActor], entityStore, sysConfig, system, sysConfig.mandantConfiguration.name, ConfigFactory.load)
}

trait RouteServiceComponent {

  val entityStore: ActorRef
  val sysConfig: SystemConfig
  val system: ActorSystem
  val fileStore: FileStore
  val actorRefFactory: ActorRefFactory

  val stammdatenRouteService: StammdatenRoutes
  val buchhaltungRouteService: BuchhaltungRoutes
  val systemRouteService: SystemRouteService
}

trait DefaultRouteServiceComponent extends RouteServiceComponent {
  override lazy val stammdatenRouteService = new DefaultStammdatenRoutes(entityStore, sysConfig, fileStore, actorRefFactory)
  override lazy val buchhaltungRouteService = new DefaultBuchhaltungRoutes(entityStore, sysConfig, fileStore, actorRefFactory)
  override lazy val systemRouteService = new DefaultSystemRouteService(entityStore, sysConfig, system, fileStore, actorRefFactory)
}

// we don't implement our route structure directly in the service actor because(entityStore, sysConfig, system, fileStore, actorRefFactory)
// we want to be able to test it independently, without having to spin up an actor
trait RouteServiceActor
    extends Actor with ActorReferences
    with DefaultRouteService
    with HelloWorldRoutes
    with StatusRoutes
    with FileStoreRoutes
    with DefaultFileStoreComponent
    with CORSSupport
    with BaseJsonProtocol {
  self: RouteServiceComponent =>

  //initially run db evolution  
  override def preStart() = {
    runDBEvolution()
  }

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  val receive = runRoute(cors(dbEvolutionRoutes))

  val initializedDB = runRoute(cors(helloWorldRoute ~ systemRouteService.systemRoutes ~ stammdatenRouteService.stammdatenRoute ~ buchhaltungRouteService.buchhaltungRoute ~ fileStoreRoute))

  val dbEvolutionRoutes =
    pathPrefix("db") {
      dbEvolutionRoute()
    }

  def dbEvolutionRoute(): Route =
    path("recover") {
      post {
        onSuccess(runDBEvolution()) { x => x }
      }
    }

  def runDBEvolution() = {
    logger.debug(s"runDBEvolution:$entityStore")
    implicit val timeout = Timeout(50.seconds)
    entityStore ? CheckDBEvolution map {
      case Success(rev) =>
        logger.debug(s"Successfully check db with revision:$rev")
        context become initializedDB
        complete("")
      case Failure(e) =>
        logger.warn(s"db evolution failed", e)
        systemRouteService.handleError(e)
        complete(StatusCodes.BadRequest, e)
    }
  }
}

// this trait defines our service behavior independently from the service actor
trait DefaultRouteService extends HttpService with ActorReferences with BaseJsonProtocol with StreamSupport with FileStoreComponent with LazyLogging {

  val personId: PersonId
  implicit val timeout = Timeout(5.seconds)

  def create[E <: AnyRef: ClassTag, I <: BaseId](idFactory: Long => I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], persister: Persister[E, _]) = {
    requestInstance { request =>
      entity(as[E]) { entity =>
        created(request)(entity)
      }
    }
  }

  def created[E <: AnyRef: ClassTag, I <: BaseId](request: HttpRequest)(entity: E)(implicit persister: Persister[E, _]) = {
    //create entity
    onSuccess(entityStore ? EntityStore.InsertEntityCommand(personId, entity)) {
      case event: EntityInsertedEvent[_, _] =>
        respondWithHeaders(Location(request.uri.withPath(request.uri.path / event.id.toString))) {
          respondWithStatus(StatusCodes.Created) {
            complete(IdResponse(event.id.id).toJson.compactPrint)
          }
        }
      case x =>
        complete(StatusCodes.BadRequest, s"No id generated:$x")
    }
  }

  def update[E <: AnyRef: ClassTag, I <: BaseId](id: I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _]) = {
    entity(as[E]) { entity => updated(id, entity) }
  }

  def update[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _]) = {
    updated(id, entity)
  }

  def updated[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit idPersister: Persister[I, _], entityPersister: Persister[E, _]) = {
    //update entity
    onSuccess(entityStore ? EntityStore.UpdateEntityCommand(personId, id, entity)) { result =>
      complete(StatusCodes.Accepted, "")
    }
  }

  def list[R](f: => Future[R])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch list of something
    onSuccess(f) { result =>
      complete(result)
    }
  }

  def detail[R](f: => Future[Option[R]])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch detail of something
    onSuccess(f) { result =>
      result.map(complete(_)).getOrElse(complete(StatusCodes.NotFound))
    }
  }

  /**
   * @persister declare format to ensure that format exists for persising purposes
   */
  def remove[I <: BaseId](id: I)(implicit persister: Persister[I, _]) = {
    onSuccess(entityStore ? EntityStore.DeleteEntityCommand(personId, id)) { result =>
      complete("")
    }
  }

  def download(fileType: FileType, id: String) = {
    onSuccess(fileStore.getFile(fileType.bucket, id)) {
      case Left(e) => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found. Error: ${e}")
      case Right(file) => {
        val streamResponse: Stream[ByteString] = Stream.continually(file.file.read).takeWhile(_ != -1).map(ByteString(_))
        streamThenClose(streamResponse, file.file)
      }
    }
  }

  def upload(fileType: FileType, name: Option[String] = None)(onUpload: (InputStream, String) => RequestContext => Unit): RequestContext => Unit = {
    entity(as[MultipartFormData]) { formData =>
      val details = formData.fields.collectFirst {
        case BodyPart(entity, headers) =>
          val content = new ByteArrayInputStream(entity.data.toByteArray)
          val fileName = headers.find(h => h.is("content-disposition")).get.value.split("filename=").last
          (content, fileName)
      }

      val id = name.getOrElse(UUID.randomUUID.toString)
      details.map {
        case (content, fileName) =>
          onUpload(content, fileName)
      } getOrElse {
        complete(StatusCodes.BadRequest, "File has to be submitted using multipart formdata")
      }
    }
  }

  def storeToFileStore(fileType: FileType, name: Option[String] = None, content: InputStream, fileName: String)(onUpload: (String, FileStoreFileMetadata) => RequestContext => Unit, onError: Option[FileStoreError => RequestContext => Unit] = None): RequestContext => Unit = {
    val id = name.getOrElse(UUID.randomUUID.toString)
    onSuccess(fileStore.putFile(fileType.bucket, Some(id), FileStoreFileMetadata(fileName, fileType), content)) {
      case Left(e) => onError.map(_(e)).getOrElse(complete(StatusCodes.BadRequest, s"File of file type ${fileType} with id ${id} could not be stored. Error: ${e}"))
      case Right(metadata) => onUpload(id, metadata)
    }
  }

  def uploadStored(fileType: FileType, name: Option[String] = None)(onUpload: (String, FileStoreFileMetadata) => RequestContext => Unit, onError: Option[FileStoreError => RequestContext => Unit] = None) = {
    upload(fileType, name) { (content, fileName) =>
      storeToFileStore(fileType, name, content, fileName)(onUpload, onError)
    }
  }

}

class DefaultRouteServiceActor(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val mandant: String,
  override val config: Config
)
    extends RouteServiceActor
    with DefaultRouteServiceComponent
