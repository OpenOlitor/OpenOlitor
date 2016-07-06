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
import ch.openolitor.stammdaten._
import ch.openolitor.core._
import ch.openolitor.core.models._
import spray.routing._
import spray.routing.authentication._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.http.HttpHeaders._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import spray.util._
import spray.caching._
import HttpCharsets._
import MediaTypes._
import java.util.UUID
import ch.openolitor.core.domain._
import ch.openolitor.core.domain.EntityStore._
import akka.pattern.ask
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._
import ch.openolitor.core.BaseJsonProtocol._
import com.typesafe.config.Config
import scala.util._
import stamina.Persister
import stamina.json.JsonPersister
import ch.openolitor.core.system._
import java.io.ByteArrayInputStream
import ch.openolitor.core.filestore._
import spray.routing.StandardRoute
import akka.util.ByteString
import scala.reflect.ClassTag
import ch.openolitor.buchhaltung._
import com.typesafe.scalalogging.LazyLogging
import spray.routing.RequestContext
import java.io.InputStream
import ch.openolitor.core.security._
import ch.openolitor.stammdaten.models.AdministratorZugang
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.util.InputStreamUtil._
import java.io.InputStream
import java.util.zip.ZipInputStream
import ch.openolitor.util.ZipBuilder

object RouteServiceActor {
  def props(entityStore: ActorRef, eventStore: ActorRef, mailService: ActorRef, reportSystem: ActorRef, fileStore: FileStore, loginTokenCache: Cache[Subject])(implicit sysConfig: SystemConfig, system: ActorSystem): Props =
    Props(classOf[DefaultRouteServiceActor], entityStore, eventStore, mailService, reportSystem, fileStore, sysConfig, system, loginTokenCache)
}

trait RouteServiceComponent extends ActorReferences {
  val entityStore: ActorRef
  val eventStore: ActorRef
  val mailService: ActorRef
  val sysConfig: SystemConfig
  val system: ActorSystem
  val fileStore: FileStore
  val actorRefFactory: ActorRefFactory

  val stammdatenRouteService: StammdatenRoutes
  val buchhaltungRouteService: BuchhaltungRoutes
  val systemRouteService: SystemRouteService
  val loginRouteService: LoginRouteService
}

trait DefaultRouteServiceComponent extends RouteServiceComponent with TokenCache {
  override lazy val stammdatenRouteService = new DefaultStammdatenRoutes(entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory)
  override lazy val buchhaltungRouteService = new DefaultBuchhaltungRoutes(entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory)
  override lazy val systemRouteService = new DefaultSystemRouteService(entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory)
  override lazy val loginRouteService = new DefaultLoginRouteService(entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, loginTokenCache)
}

// we don't implement our route structure directly in the service actor because(entityStore, sysConfig, system, fileStore, actorRefFactory)
// we want to be able to test it independently, without having to spin up an actor
trait RouteServiceActor
    extends Actor with ActorReferences
    with DefaultRouteService
    with HelloWorldRoutes
    with StatusRoutes
    with FileStoreRoutes
    with FileStoreComponent
    with CORSSupport
    with BaseJsonProtocol
    with RoleBasedAuthorization {
  self: RouteServiceComponent =>

  //initially run db evolution  
  override def preStart() = {
    runDBEvolution()
  }

  implicit val openolitorRejectionHandler: RejectionHandler = OpenOlitorRejectionHandler()

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  val receive = runRoute(cors(dbEvolutionRoutes))

  val initializedDB = runRoute(cors(
    // unsecured routes
    helloWorldRoute ~
      systemRouteService.statusRoute ~
      loginRouteService.loginRoute ~

      // secured routes by XSRF token authenticator
      authenticate(loginRouteService.openOlitorAuthenticator) { implicit subject =>
        loginRouteService.logoutRoute ~
          authorize(hasRole(AdministratorZugang)) {
            stammdatenRouteService.stammdatenRoute ~
              buchhaltungRouteService.buchhaltungRoute ~
              fileStoreRoute
          }
      } ~

      // routes secured by basicauth mainly used for service accounts
      authenticate(BasicAuth(loginRouteService.basicAuthValidation _, realm = "OpenOlitor")) { implicit subject =>
        authorize(hasRole(AdministratorZugang)) {
          systemRouteService.adminRoutes
        }
      }
  ))

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
trait DefaultRouteService extends HttpService with ActorReferences with BaseJsonProtocol with StreamSupport
    with FileStoreComponent
    with LazyLogging
    with ReportJsonProtocol {

  implicit val timeout = Timeout(5.seconds)

  protected def create[E <: AnyRef: ClassTag, I <: BaseId](idFactory: Long => I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], persister: Persister[E, _], subject: Subject) = {
    requestInstance { request =>
      entity(as[E]) { entity =>
        created(request)(entity)
      }
    }
  }

  protected def created[E <: AnyRef: ClassTag, I <: BaseId](request: HttpRequest)(entity: E)(implicit persister: Persister[E, _], subject: Subject) = {
    //create entity
    onSuccess(entityStore ? EntityStore.InsertEntityCommand(subject.personId, entity)) {
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

  protected def update[E <: AnyRef: ClassTag, I <: BaseId](id: I)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject) = {
    entity(as[E]) { entity => updated(id, entity) }
  }

  protected def update[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit
    um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject) = {
    updated(id, entity)
  }

  protected def updated[E <: AnyRef: ClassTag, I <: BaseId](id: I, entity: E)(implicit idPersister: Persister[I, _], entityPersister: Persister[E, _], subject: Subject) = {
    //update entity
    onSuccess(entityStore ? EntityStore.UpdateEntityCommand(subject.personId, id, entity)) { result =>
      complete(StatusCodes.Accepted, "")
    }
  }

  protected def list[R](f: => Future[R])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch list of something
    onSuccess(f) { result =>
      complete(result)
    }
  }

  protected def detail[R](f: => Future[Option[R]])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch detail of something
    onSuccess(f) { result =>
      result.map(complete(_)).getOrElse(complete(StatusCodes.NotFound))
    }
  }

  /**
   * @persister declare format to ensure that format exists for persising purposes
   */
  protected def remove[I <: BaseId](id: I)(implicit persister: Persister[I, _], subject: Subject) = {
    onSuccess(entityStore ? EntityStore.DeleteEntityCommand(subject.personId, id)) { result =>
      complete("")
    }
  }

  protected def download(fileType: FileType, id: String) = {
    onSuccess(fileStore.getFile(fileType.bucket, id)) {
      case Left(e) => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found.")
      case Right(file) =>
        val name = if (file.metaData.name.isEmpty) id else file.metaData.name
        respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", name)))) {
          stream(file.file)
        }
    }
  }

  protected def downloadAsZip(zipFileName: String, fileReferences: Seq[FileStoreFileReference]) = {
    val builder = new ZipBuilder()
    fileReferences map { ref =>
      fileStore.getFile(ref.fileType.bucket, ref.id.id) map {
        case Left(e) =>
          logger.warn(s"Couldn't download file from fileStore '${ref.fileType.bucket}-${ref.id.id}':$e")
        case Right(file) =>
          builder.addZipEntry(file.metaData.name, file.file)
      }
    }
    builder.close().map(result => streamZip(zipFileName, result)) getOrElse complete(StatusCodes.NotFound)
  }

  protected def stream(input: InputStream) = {
    val streamResponse: Stream[ByteString] = Stream.continually(input.read).takeWhile(_ != -1).map(ByteString(_))
    streamThenClose(streamResponse, Some(input))
  }

  protected def stream(input: Array[Byte]) = {
    val streamResponse: Stream[ByteString] = Stream(ByteString(input))
    streamThenClose(streamResponse, None)
  }

  protected def stream(input: ByteString) = {
    logger.debug(s"Stream result. Length:${input.size}")
    val streamResponse: Stream[ByteString] = Stream(input)
    streamThenClose(streamResponse, None)
  }

  protected def streamZip(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/zip`) {
        stream(result)
      }
    }
  }

  protected def streamPdf(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/pdf`) {
        stream(result)
      }
    }
  }

  protected def streamOdt(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/vnd.oasis.opendocument.text`) {
        stream(result)
      }
    }
  }

  protected def uploadOpt(fileProperty: String = "file")(onUpload: MultipartFormData => Option[(InputStream, String)] => RequestContext => Unit): RequestContext => Unit = {
    entity(as[MultipartFormData]) { formData =>
      val details = formData.fields.collectFirst {
        case b @ BodyPart(entity, headers) if b.name == Some(fileProperty) =>
          val content = new ByteArrayInputStream(entity.data.toByteArray)
          val fileName = headers.find(h => h.is("content-disposition")).get.value.split("filename=").last
          (content, fileName)
      }
      onUpload(formData)(details)
    }
  }

  protected def upload(onUpload: (MultipartFormData, InputStream, String) => RequestContext => Unit): RequestContext => Unit = {
    uploadOpt() { formData => details =>
      details.map {
        case (content, fileName) =>
          onUpload(formData, content, fileName)
      } getOrElse {
        complete(StatusCodes.BadRequest, "File has to be submitted using multipart formdata")
      }
    }
  }

  protected def storeToFileStore(fileType: FileType, name: Option[String] = None, content: InputStream, fileName: String)(onUpload: (String, FileStoreFileMetadata) => RequestContext => Unit, onError: Option[FileStoreError => RequestContext => Unit] = None): RequestContext => Unit = {
    val id = name.getOrElse(UUID.randomUUID.toString)
    onSuccess(fileStore.putFile(fileType.bucket, Some(id), FileStoreFileMetadata(fileName, fileType), content)) {
      case Left(e) => onError.map(_(e)).getOrElse(complete(StatusCodes.BadRequest, s"File of file type ${fileType} with id ${id} could not be stored. Error: ${e}"))
      case Right(metadata) => onUpload(id, metadata)
    }
  }

  protected def uploadStored(fileType: FileType, name: Option[String] = None)(onUpload: (String, FileStoreFileMetadata) => RequestContext => Unit, onError: Option[FileStoreError => RequestContext => Unit] = None) = {
    upload { (formData, content, fileName) =>
      storeToFileStore(fileType, name, content, fileName)(onUpload, onError)
    }
  }

  protected def generateReport[I](
    id: Option[I],
    reportFunction: ReportConfig[I] => Future[Either[ServiceFailed, ReportServiceResult[I]]]
  )(idFactory: Long => I)(implicit subject: Subject) = {
    uploadOpt("vorlage") { formData => file =>
      //use custom or default template whether content was delivered or not
      (for {
        vorlage <- loadVorlage(file)
        pdfGenerieren <- Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfGenerieren") =>
            entity.asString.toBoolean
        }.getOrElse(false))
        pdfAblegen <- Try(pdfGenerieren && formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfAblegen") =>
            entity.asString.toBoolean
        }.getOrElse(false))
        downloadFile <- Try(!pdfAblegen || formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("pdfDownloaden") =>
            entity.asString.toBoolean
        }.getOrElse(true))
        ids <- id.map(id => Success(Seq(id))).getOrElse(Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("ids") =>
            entity.asString.split(",").map(id => idFactory(id.toLong))
        }.getOrElse(Seq())))
      } yield {
        val config = ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen)

        onSuccess(reportFunction(config)) {
          case Left(serviceError) =>
            complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$serviceError")
          case Right(result) if result.hasErrors =>
            val errorString = result.validationErrors.map(_.message).mkString(",")
            complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:${errorString}")
          case Right(result) =>
            result.result match {
              case SingleReportResult(_, _, Left(ReportError(_, error))) => complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$error")
              case SingleReportResult(_, _, Right(DocumentReportResult(_, result, name))) => streamOdt(name, result)
              case SingleReportResult(_, _, Right(PdfReportResult(_, result, name))) => streamPdf(name, result)
              case SingleReportResult(_, _, Right(StoredPdfReportResult(_, fileType, fileStoreId))) if downloadFile => download(fileType, fileStoreId.id)
              case SingleReportResult(_, _, Right(result: StoredPdfReportResult)) =>
                //complete(result)
                complete("")
              case ZipReportResult(_, errors, zip) if !zip.isDefined =>
                val errorString: String = errors.map(_.error).mkString("\n")
                complete(StatusCodes.BadRequest, errorString)
              case ZipReportResult(_, errors, zip) if zip.isDefined =>
                //TODO: send error to client as well
                errors.map(error => logger.warn(s"Coulnd't generate report document: $error"))
                zip.map(result => streamZip("Report_" + System.currentTimeMillis + ".zip", result)) getOrElse (complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden, es wurden keine Dateien erzeugt"))
              case BatchStoredPdfReportResult(_, errors, results) if downloadFile =>
                downloadAsZip("Report_" + System.currentTimeMillis + ".zip", results)
              case result: BatchStoredPdfReportResult =>
                //complete(result)
                complete("")
              case x =>
                logger.error(s"Received unexpected result:$x")
                complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden")
            }
        }
      }) match {
        case Success(result) => result
        case Failure(error) => complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:${error}")
      }
    }
  }

  private def loadVorlage(file: Option[(InputStream, String)]): Try[BerichtsVorlage] = {
    file map {
      case (is, name) => is.toByteArray.map(result => EinzelBerichtsVorlage(result))
    } getOrElse Success(StandardBerichtsVorlage)
  }
}

class DefaultRouteServiceActor(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val fileStore: FileStore,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val loginTokenCache: Cache[Subject]
) extends RouteServiceActor
    with DefaultRouteServiceComponent
