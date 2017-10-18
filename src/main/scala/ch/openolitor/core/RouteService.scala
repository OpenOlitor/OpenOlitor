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
import ch.openolitor.stammdaten.models.{ AdministratorZugang, KundenZugang }
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.util.InputStreamUtil._
import java.io.InputStream
import java.util.zip.ZipInputStream
import ch.openolitor.core.system.DefaultNonAuthRessourcesRouteService
import ch.openolitor.util.ZipBuilder
import ch.openolitor.kundenportal.KundenportalRoutes
import ch.openolitor.kundenportal.DefaultKundenportalRoutes
import ch.openolitor.reports._
import ch.openolitor.stammdaten.models.ProjektVorlageId
import spray.can.server.Response
import ch.openolitor.core.ws.ExportFormat
import ch.openolitor.core.ws.Json
import ch.openolitor.core.ws.ODS
import org.odftoolkit.simple._
import org.odftoolkit.simple.table._
import org.odftoolkit.simple.SpreadsheetDocument
import java.io.ByteArrayOutputStream
import java.util.Locale
import org.odftoolkit.simple.style.StyleTypeDefinitions
import scala.None
import scala.collection.Iterable
import collection.JavaConverters._
import java.io.File
import java.io.FileInputStream
import ch.openolitor.core.db.evolution.DBEvolutionActor.CheckDBEvolution
import scala.concurrent.ExecutionContext
import ch.openolitor.util.ZipBuilderWithFile

sealed trait ResponseType
case object Download extends ResponseType
case object Fetch extends ResponseType

object RouteServiceActor {
  def props(dbEvolutionActor: ActorRef, entityStore: ActorRef, eventStore: ActorRef, mailService: ActorRef, reportSystem: ActorRef, fileStore: FileStore, airbrakeNotifier: ActorRef, jobQueueService: ActorRef, loginTokenCache: Cache[Subject])(implicit sysConfig: SystemConfig, system: ActorSystem): Props =
    Props(classOf[DefaultRouteServiceActor], dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, fileStore, airbrakeNotifier, jobQueueService, sysConfig, system, loginTokenCache)
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
  val stammdatenRouteOpenService: StammdatenOpenRoutes
  val buchhaltungRouteService: BuchhaltungRoutes
  val reportsRouteService: ReportsRoutes
  val syncReportsRouteService: SyncReportsRoutes
  val kundenportalRouteService: KundenportalRoutes
  val systemRouteService: SystemRouteService
  val loginRouteService: LoginRouteService
  val nonAuthRessourcesRouteService: NonAuthRessourcesRouteService
}

trait DefaultRouteServiceComponent extends RouteServiceComponent with TokenCache {
  override lazy val stammdatenRouteService = new DefaultStammdatenRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val stammdatenRouteOpenService = new DefaultStammdatenOpenRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val buchhaltungRouteService = new DefaultBuchhaltungRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val reportsRouteService = new DefaultReportsRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val syncReportsRouteService = new DefaultSyncReportsRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val kundenportalRouteService = new DefaultKundenportalRoutes(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val systemRouteService = new DefaultSystemRouteService(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
  override lazy val loginRouteService = new DefaultLoginRouteService(dbEvolutionActor, entityStore, eventStore, mailService, reportSystem, sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService, loginTokenCache)
  override lazy val nonAuthRessourcesRouteService = new DefaultNonAuthRessourcesRouteService(sysConfig, system, fileStore, actorRefFactory, airbrakeNotifier, jobQueueService)
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
    with RoleBasedAuthorization
    with AirbrakeNotifierReference
    with LazyLogging {
  self: RouteServiceComponent =>

  //initially run db evolution
  override def preStart() = {
    runDBEvolution()
  }

  implicit val openolitorRejectionHandler: RejectionHandler = OpenOlitorRejectionHandler(this)

  implicit def exceptionHandler: ExceptionHandler = OpenOlitorExceptionHandler(this)

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling

  val receive: Receive = runRoute(cors(dbEvolutionRoutes))

  val initializedDB: Receive = runRoute(cors(
    // unsecured routes
    helloWorldRoute ~
      systemRouteService.statusRoute ~
      nonAuthRessourcesRouteService.ressourcesRoutes ~
      loginRouteService.loginRoute ~
      stammdatenRouteOpenService.stammdatenOpenRoute ~

      // secured routes by XSRF token authenticator
      authenticate(loginRouteService.openOlitorAuthenticator) { implicit subject =>
        systemRouteService.jobQueueRoute ~
          loginRouteService.logoutRoute ~
          authorize(hasRole(AdministratorZugang)) {
            stammdatenRouteService.stammdatenRoute ~
              buchhaltungRouteService.buchhaltungRoute ~
              reportsRouteService.reportsRoute ~
              syncReportsRouteService.syncReportsRoute ~
              fileStoreRoute
          } ~
          authorize(hasRole(KundenZugang) || hasRole(AdministratorZugang)) {
            kundenportalRouteService.kundenportalRoute
          }
      } ~

      // routes secured by basicauth mainly used for service accounts
      authenticate(BasicAuth(loginRouteService.basicAuthValidation _, realm = "OpenOlitor")) { implicit subject =>
        authorize(hasRole(AdministratorZugang)) {
          systemRouteService.adminRoutes
        }
      } ~
      authenticate(loginRouteService.openOlitorAuthenticator) { implicit subject =>
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
    dbEvolutionActor ? CheckDBEvolution map {
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
    with SprayDeserializers
    with ReportJsonProtocol
    with DateFormats {

  implicit val timeout = Timeout(5.seconds)
  implicit lazy val executionContext: ExecutionContext = system.dispatcher
  lazy val DefaultChunkSize = ConfigLoader.loadConfig.getIntBytes("spray.can.parsing.max-chunk-size")

  implicit val exportFormatPath = enumPathMatcher(path =>
    path.head match {
      case '.' => ExportFormat.apply(path) match {
        case x => Some(x)
      }
      case _ => None
    })

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
        complete(StatusCodes.BadRequest, s"No id generated or CommandHandler not triggered:$x")
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

  protected def list[R](f: => Future[R], exportFormat: Option[ExportFormat])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch list of something
    onSuccess(f) { result =>
      exportFormat match {
        case Some(ODS) => {
          val dataDocument = SpreadsheetDocument.newSpreadsheetDocument()
          val sheet = dataDocument.getSheetByIndex(0)
          sheet.setCellStyleInheritance(false)

          def writeToRow(row: Row, element: Any, cellIndex: Int): Unit = {
            element match {
              case null =>
              case some: Some[Any] => writeToRow(row, some.x, cellIndex)
              case None =>
              case ite: Iterable[Any] => ite map { item => writeToRow(row, item, cellIndex) }
              case id: BaseId => row.getCellByIndex(cellIndex).setDoubleValue(id.id)
              case stringId: BaseStringId => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + stringId.id).trim)
              case str: String => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + str).trim)
              case dat: org.joda.time.DateTime => row.getCellByIndex(cellIndex).setDateTimeValue(dat.toCalendar(Locale.GERMAN))
              case nbr: Number => row.getCellByIndex(cellIndex).setDoubleValue(nbr.doubleValue())
              case x => row.getCellByIndex(cellIndex).setStringValue((row.getCellByIndex(cellIndex).getStringValue + " " + x.toString).trim)
            }
          }

          result match {
            case genericList: List[Any] =>
              if (genericList.nonEmpty) {
                genericList.head match {
                  case firstMapEntry: Map[_, _] =>
                    val listOfMaps = genericList.asInstanceOf[List[Map[String, Any]]]
                    val row = sheet.getRowByIndex(0);

                    listOfMaps.head.zipWithIndex foreach {
                      case ((fieldName, value), index) =>
                        row.getCellByIndex(index).setStringValue(fieldName)
                        val font = row.getCellByIndex(index).getFont
                        font.setFontStyle(StyleTypeDefinitions.FontStyle.BOLD)
                        font.setSize(10)
                        row.getCellByIndex(index).setFont(font)
                    }

                    listOfMaps.zipWithIndex foreach {
                      case (entry, index) =>
                        val row = sheet.getRowByIndex(index + 1);

                        entry.zipWithIndex foreach {
                          case ((fieldName, value), colIndex) =>
                            fieldName match {
                              case "passwort" => writeToRow(row, "Not available", colIndex)
                              case _ => writeToRow(row, value, colIndex)
                            }
                        }
                    }

                  case firstProductEntry: Product =>
                    val listOfProducts = genericList.asInstanceOf[List[Product]]
                    val row = sheet.getRowByIndex(0);

                    def getCCParams(cc: Product) = cc.getClass.getDeclaredFields.map(_.getName) // all field names
                      .zip(cc.productIterator.to).toMap // zipped with all values

                    getCCParams(listOfProducts.head).zipWithIndex foreach {
                      case ((fieldName, value), index) =>
                        row.getCellByIndex(index).setStringValue(fieldName)
                        val font = row.getCellByIndex(index).getFont
                        font.setFontStyle(StyleTypeDefinitions.FontStyle.BOLD)
                        font.setSize(10)
                        row.getCellByIndex(index).setFont(font)
                    }

                    listOfProducts.zipWithIndex foreach {
                      case (entry, index) =>
                        val row = sheet.getRowByIndex(index + 1);

                        getCCParams(entry).zipWithIndex foreach {
                          case ((fieldName, value), colIndex) =>
                            writeToRow(row, value, colIndex)
                        }
                    }
                }
              }
            case x => sheet.getRowByIndex(0).getCellByIndex(0).setStringValue("Data of type" + x.toString + " could not be transfered to ODS file.")
          }

          sheet.getColumnList.asScala map { _.setUseOptimalWidth(true) }

          val outputStream = new ByteArrayOutputStream
          dataDocument.save(outputStream)
          streamOds("Daten_" + System.currentTimeMillis + ".ods", outputStream.toByteArray())
        }
        //matches "None" and "Some(Json)"
        case None => complete(result)
        case Some(x) => complete(result)
      }
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

  protected def downloadAll(zipFileName: String, fileType: FileType, ids: Seq[FileStoreFileId]) = {
    ids match {
      case Seq() => complete(StatusCodes.BadRequest)
      case Seq(fileStoreId) => download(fileType, fileStoreId.id)
      case list =>
        val refs = ids.map(id => FileStoreFileReference(fileType, id))
        downloadAsZip(zipFileName, refs)
    }
  }

  protected def fetch(fileType: FileType, id: String) = {
    tryDownload(fileType, id, Fetch)(e => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found."))
  }

  protected def download(fileType: FileType, id: String) = {
    tryDownload(fileType, id, Download)(e => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found."))
  }

  protected def tryDownload(fileType: FileType, id: String, responseType: ResponseType = Download)(errorFunction: FileStoreError => RequestContext => Unit) = {
    onSuccess(fileStore.getFile(fileType.bucket, id)) {
      case Left(e) => errorFunction(e)
      case Right(file) =>
        val name = if (file.metaData.name.isEmpty) id else file.metaData.name
        responseType match {
          case Download => respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", name)))) {
            stream(file.file)
          }
          case Fetch => respondWithMediaType(MediaTypes.`text/css`) {
            stream(file.file)
          }
        }

    }
  }

  protected def downloadAsZip(zipFileName: String, fileReferences: Seq[FileStoreFileReference]) = {
    val builder = new ZipBuilderWithFile()
    fileReferences map { ref =>
      fileStore.getFile(ref.fileType.bucket, ref.id.id) map {
        case Left(e) =>
          logger.warn(s"Couldn't download file from fileStore '${ref.fileType.bucket}-${ref.id.id}':$e")
        case Right(file) =>
          val name = if (file.metaData.name.isEmpty) ref.id.id else file.metaData.name
          logger.debug(s"Add zip entry:${ref.id.id} => $name")
          builder.addZipEntry(name, file.file)
      }
    }
    builder.close().map(result => streamZip(zipFileName, result)) getOrElse complete(StatusCodes.NotFound)
  }

  protected def stream(input: File, deleteAfterStreaming: Boolean = false) = {
    val streamResponse = input.toByteArrayStream(DefaultChunkSize).map(ByteString(_))
    streamThen(streamResponse, { () =>
      if (deleteAfterStreaming) {
        input.delete()
      }
    })
  }

  protected def stream(input: InputStream) = {
    val streamResponse = input.toByteArrayStream(DefaultChunkSize).map(ByteString(_))
    streamThen(streamResponse, () => input.close())
  }

  protected def stream(input: Array[Byte]) = {
    val streamResponse: Stream[ByteString] = Stream(ByteString(input))
    streamIt(streamResponse)
  }

  protected def stream(input: ByteString) = {
    logger.debug(s"Stream result. Length:${input.size}")
    val streamResponse: Stream[ByteString] = Stream(input)
    streamIt(streamResponse)
  }

  protected def streamFile(fileName: String, mediaType: MediaType, file: File, deleteAfterStreaming: Boolean = false) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(mediaType) {
        stream(file, deleteAfterStreaming)
      }
    }
  }

  protected def streamZip(fileName: String, result: File, deleteAfterStreaming: Boolean = false) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/zip`) {
        stream(result, deleteAfterStreaming)
      }
    }
  }

  protected def streamPdf(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/pdf`) {
        complete(HttpData(result))
      }
    }
  }

  protected def streamOdt(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/vnd.oasis.opendocument.text`) {
        complete(HttpData(result))
      }
    }
  }

  protected def streamOds(fileName: String, result: Array[Byte]) = {
    respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", fileName)))) {
      respondWithMediaType(MediaTypes.`application/vnd.oasis.opendocument.spreadsheet`) {
        complete(HttpData(result))
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
        vorlageId <- Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("projektVorlageId") =>
            Some(ProjektVorlageId(entity.asString.toLong))
        }.getOrElse(None))
        datenExtrakt <- Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("datenExtrakt") =>
            entity.asString.toBoolean
        }.getOrElse(false))
        vorlage <- loadVorlage(datenExtrakt, file, vorlageId)
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
        }.getOrElse(false))
        ids <- id.map(id => Success(Seq(id))).getOrElse(Try(formData.fields.collectFirst {
          case b @ BodyPart(entity, headers) if b.name == Some("ids") =>
            entity.asString.split(",").map(id => idFactory(id.toLong))
        }.getOrElse(Seq())))
      } yield {
        logger.debug(s"generateReport: ids: $ids, pdfGenerieren: $pdfGenerieren, pdfAblegen: $pdfAblegen, downloadFile: $downloadFile")
        val config = ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen, downloadFile)

        onSuccess(reportFunction(config)) {
          case Left(serviceError) =>
            complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:$serviceError")
          case Right(result) =>
            result.result match {
              case ReportDataResult(id, json) =>
                respondWithHeader(HttpHeaders.`Content-Disposition`("attachment", Map(("filename", s"${id}.json")))) {
                  complete(json)
                }
              case AsyncReportResult(jobId) =>
                // async result, return jobId
                val ayncResult = AsyncReportServiceResult(jobId, result.validationErrors.map(_.asJson))
                complete(ayncResult)
              case x if result.hasErrors =>
                val errorString = result.validationErrors.map(_.message).mkString(",")
                complete(StatusCodes.BadRequest, s"Der Bericht konnte nicht erzeugt werden:${errorString}")
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

  private def loadVorlage(datenExtrakt: Boolean, file: Option[(InputStream, String)], vorlageId: Option[ProjektVorlageId]): Try[BerichtsVorlage] = {
    (datenExtrakt, file, vorlageId) match {
      case (true, _, _) => Success(DatenExtrakt)
      case (false, Some((is, name)), _) => is.toByteArray.map(result => EinzelBerichtsVorlage(result))
      case (false, None, Some(vorlageId)) => Success(ProjektBerichtsVorlage(vorlageId))
      case _ => Success(StandardBerichtsVorlage)
    }
  }
}

class DefaultRouteServiceActor(
  override val dbEvolutionActor: ActorRef,
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val mailService: ActorRef,
  override val reportSystem: ActorRef,
  override val fileStore: FileStore,
  override val airbrakeNotifier: ActorRef,
  override val jobQueueService: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val loginTokenCache: Cache[Subject]
) extends RouteServiceActor
    with DefaultRouteServiceComponent
