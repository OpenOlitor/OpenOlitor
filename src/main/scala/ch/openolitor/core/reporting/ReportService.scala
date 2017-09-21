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
package ch.openolitor.core.reporting

import ch.openolitor.core.ActorReferences
import scalaz._
import Scalaz._
import ch.openolitor.core.filestore._
import ch.openolitor.core.reporting.ReportSystem._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.util.UUID
import ch.openolitor.util.ByteBufferBackedInputStream
import akka.pattern.ask
import spray.json.JsonFormat
import ch.openolitor.core.JSONSerializable
import com.typesafe.scalalogging.LazyLogging
import scala.util.{ Try, Success => TrySuccess, Failure => TryFailure }
import ch.openolitor.util.ByteStringUtil
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.duration._
import ch.openolitor.util.InputStreamUtil._
import java.util.Locale
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.models.ProjektVorlageId
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsync
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import spray.json._
import ch.openolitor.core.DateFormats
import ch.openolitor.core.jobs.JobQueueService.JobId
import ch.openolitor.core.JSONSerializable

sealed trait BerichtsVorlage extends Product
case object DatenExtrakt extends BerichtsVorlage
case object StandardBerichtsVorlage extends BerichtsVorlage
case class ProjektBerichtsVorlage(id: ProjektVorlageId) extends BerichtsVorlage
case class EinzelBerichtsVorlage(file: Array[Byte]) extends BerichtsVorlage
case class ServiceFailed(msg: String, e: Throwable = null) extends Exception(msg, e)

case class ReportForm[I](ids: Seq[I], pdfGenerieren: Boolean, pdfAblegen: Boolean, fileDownload: Boolean) extends JSONSerializable {
  def toConfig(vorlage: BerichtsVorlage): ReportConfig[I] = {
    ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen, fileDownload)
  }
}
case class ReportConfig[I](ids: Seq[I], vorlage: BerichtsVorlage, pdfGenerieren: Boolean, pdfAblegen: Boolean, downloadFile: Boolean)
case class ValidationError[I](id: I, message: String)(implicit format: JsonFormat[I]) {
  val jsonId = id.toJson

  val asJson = JsObject(
    "message" -> JsString(message),
    "id" -> jsonId
  )
}
case class ReportServiceResult[I](jobId: JobId, validationErrors: Seq[ValidationError[I]], result: ReportResult) {
  val hasErrors = !validationErrors.isEmpty
}
case class AsyncReportServiceResult(jobId: JobId, validationErrors: Seq[JsValue]) extends JSONSerializable {
  val hasErrors = !validationErrors.isEmpty
}

trait ReportService extends LazyLogging with AsyncConnectionPoolContextAware with FileTypeFilenameMapping with DateFormats {
  self: ActorReferences with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  implicit val actorSystem = system

  import ReportSystem._
  type ServiceResult[T] = EitherT[Future, ServiceFailed, T]

  /**
   *
   */
  def generateReports[I, E](
    config: ReportConfig[I],
    validationFunction: Seq[I] => Future[(Seq[ValidationError[I]], Seq[E])],
    vorlageType: FileType,
    vorlageId: Option[String],
    idFactory: E => Any,
    ablageType: FileType,
    ablageIdFactory: E => Option[String],
    nameFactory: E => String,
    localeFactory: E => Locale,
    jobId: JobId
  )(implicit personId: PersonId, jsonFormat: JsonFormat[E]): Future[Either[ServiceFailed, ReportServiceResult[I]]] = {
    logger.debug(s"Validate ids:${config.ids}")
    validationFunction(config.ids) flatMap {
      case (errors, Seq()) =>
        Future { Right(ReportServiceResult(jobId, errors, ReportError(None, errors.mkString(",")))) }
      case (errors, result) if (config.vorlage == DatenExtrakt) =>
        Future {
          val jsonData = JsArray(result.map(jsonFormat.write(_).asJsObject).toVector)
          Right(ReportServiceResult(jobId, errors, ReportDataResult(jobId.id, jsonData)))
        }
      case (errors, result) =>
        logger.debug(s"Validation errors:$errors, process result records:${result.length}")
        val ablageParams = if (config.pdfAblegen) Some(FileStoreParameters[E](ablageType)) else None
        generateDocument(config.vorlage, vorlageType, vorlageId, ReportData(result, idFactory, ablageIdFactory, nameFactory, localeFactory), config.pdfGenerieren, ablageParams, config.downloadFile, jobId).run map {
          case -\/(e) =>
            logger.warn(s"Failed generating report {}", e.getMessage)
            Left(e)
          case \/-(result) => Right(ReportServiceResult(jobId, errors, AsyncReportResult(jobId)))
        }
    }
  }

  def generateDocument[I, E](vorlage: BerichtsVorlage, fileType: FileType, id: Option[String], data: ReportData[E],
    pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]], downloadFile: Boolean, jobId: JobId)(implicit personId: PersonId): ServiceResult[JobId] = {
    for {
      temp <- loadBerichtsvorlage(vorlage, fileType, id)
      source <- generateReport(temp, data, pdfGenerieren, pdfAblage, downloadFile, jobId)
    } yield source
  }

  def generateReport[I, E](vorlage: Array[Byte], data: ReportData[E], pdfGenerieren: Boolean,
    pdfAblage: Option[FileStoreParameters[E]], downloadFile: Boolean, jobId: JobId)(implicit personId: PersonId): ServiceResult[JobId] = EitherT {
    logger.debug(s"generateReport: vorlage: $vorlage, pdfGenerieren: $pdfGenerieren, pdfAblage: $pdfAblage, downloadFile: $downloadFile, jobId: $jobId")
    val collector =
      if (pdfAblage.isDefined) FileStoreReportResultCollector.props(reportSystem, jobQueueService, downloadFile)
      else if (data.rows.size == 1) HeadReportResultCollector.props(reportSystem, jobQueueService)
      else ChunkedReportResultCollector.props(fileStore, "Report_" + filenameDateFormat.print(System.currentTimeMillis()) + ".zip", reportSystem, jobQueueService)

    val ref = actorSystem.actorOf(collector)

    (ref ! GenerateReports(personId, jobId, vorlage, data, pdfGenerieren, pdfAblage))
    Future {
      jobId.right
    }
  }

  def loadBerichtsvorlage(vorlage: BerichtsVorlage, fileType: FileType, id: Option[String]): ServiceResult[Array[Byte]] = {
    vorlage match {
      case EinzelBerichtsVorlage(file) => EitherT { Future { file.right } }
      case StandardBerichtsVorlage => resolveStandardBerichtsVorlage(fileType, id)
      case ProjektBerichtsVorlage(vorlageId) => resolveProjektBerichtsVorlage(fileType, vorlageId)
      case _ => EitherT { Future { ServiceFailed(s"Berichtsvorlage nicht unterstützt").left } }
    }
  }

  /**
   * Resolve from S3 or as a local resource
   */
  def resolveStandardBerichtsVorlage(fileType: FileType, id: Option[String] = None): ServiceResult[Array[Byte]] = {
    resolveBerichtsVorlageFromFileStore(fileType, id) ||| resolveBerichtsVorlageFromResources(fileType, id)
  }

  def resolveProjektBerichtsVorlage(fileType: FileType, id: ProjektVorlageId): ServiceResult[Array[Byte]] = {
    for {
      fileStoreId <- resolveBerichtsVorlageFileStoreId(fileType, id)
      vorlage <- resolveBerichtsVorlageFromFileStore(fileType, Some(fileStoreId))
    } yield vorlage
  }

  def resolveBerichtsVorlageFileStoreId(fileType: FileType, id: ProjektVorlageId): ServiceResult[String] = EitherT {
    stammdatenReadRepository.getProjektVorlage(id) map {
      case Some(vorlage) if (vorlage.typ != fileType) =>
        ServiceFailed(s"Projekt-Vorlage kann für diesen Bericht nicht verwendet werden").left
      case Some(vorlage) if (vorlage.fileStoreId.isDefined) =>
        vorlage.fileStoreId.get.right
      case Some(vorlage) =>
        ServiceFailed(s"Bei dieser Projekt-Vorlage ist kein Dokument hinterlegt").left
      case None =>
        ServiceFailed(s"Projekt-Vorlage konnte nicht gefunden werden:$id").left
    }
  }

  def resolveBerichtsVorlageFromFileStore(fileType: FileType, id: Option[String]): ServiceResult[Array[Byte]] = EitherT {
    fileStore.getFile(fileType.bucket, id.getOrElse(defaultFileTypeId(fileType))) map {
      case Left(e) => ServiceFailed(s"Vorlage konnte im FileStore nicht gefunden werden: $fileType, $id").left
      case Right(file) => file.file.toByteArray match {
        case TrySuccess(result) => result.right
        case TryFailure(error) => ServiceFailed(s"Vorlage konnte im FileStore nicht geladen: $error").left
      }
    }
  }

  def resolveBerichtsVorlageFromResources(fileType: FileType, id: Option[String]): ServiceResult[Array[Byte]] = EitherT {
    logger.debug(s"Resolve template from resources:$fileType:$id")
    Future {
      fileTypeResourceAsStream(fileType, id) match {
        case Left(resource) => ServiceFailed(s"Vorlage konnte im folgenden Pfad nicht gefunden werden: $resource").left
        case Right(is) =>
          is.toByteArray match {
            case TrySuccess(result) => result.right
            case TryFailure(error) =>
              error.printStackTrace()
              ServiceFailed(s"Vorlage konnte im folgenden Pfad nicht gefunden werden: $error").left
          }
      }
    }
  }
}
