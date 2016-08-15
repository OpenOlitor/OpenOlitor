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

sealed trait BerichtsVorlage extends Product
case object StandardBerichtsVorlage extends BerichtsVorlage
//case class Berichtsvorlage(id: BerichtsVorlageId) extends BerichtsVorlage
case class EinzelBerichtsVorlage(file: Array[Byte]) extends BerichtsVorlage
case class ServiceFailed(msg: String, e: Throwable = null) extends Exception(msg, e)

case class ReportForm[I](ids: Seq[I], pdfGenerieren: Boolean, pdfAblegen: Boolean) extends JSONSerializable {
  def toConfig(vorlage: BerichtsVorlage): ReportConfig[I] = {
    ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen)
  }
}
case class ReportConfig[I](ids: Seq[I], vorlage: BerichtsVorlage, pdfGenerieren: Boolean, pdfAblegen: Boolean)
case class ValidationError[I](id: I, message: String)
case class ReportServiceResult[I](jobId: JobId, validationErrors: Seq[ValidationError[I]], result: ReportResult) {
  val hasErrors = !validationErrors.isEmpty
}

trait ReportService extends LazyLogging {
  self: ActorReferences with FileStoreComponent =>

  implicit val actorSystem = system

  import ReportSystem._
  type ServiceResult[T] = EitherT[Future, ServiceFailed, T]

  /**
   *
   */
  def generateReports[I, E: JsonFormat](
    config: ReportConfig[I],
    validationFunction: Seq[I] => Future[(Seq[ValidationError[I]], Seq[E])],
    vorlageType: FileType,
    vorlageId: Option[String],
    idFactory: E => Any,
    ablageType: FileType,
    ablageIdFactory: E => Option[String],
    nameFactory: E => String,
    localeFactory: E => Locale,
    jobId: JobId = JobId()
  )(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[I]]] = {
    logger.debug(s"Validate ids:${config.ids}")
    validationFunction(config.ids) flatMap {
      case (errors, Seq()) =>
        Future { Right(ReportServiceResult(jobId, errors, ReportError(None, errors.mkString(",")))) }
      case (errors, result) =>
        logger.debug(s"Validation errors:$errors, process result records:${result.length}")
        val ablageParams = if (config.pdfAblegen) Some(FileStoreParameters[E](ablageType)) else None
        generateDocument(config.vorlage, vorlageType, vorlageId, ReportData(jobId, result, idFactory, ablageIdFactory, nameFactory, localeFactory), config.pdfGenerieren, ablageParams).run map {
          case -\/(e) =>
            logger.warn(s"Failed generating report {}", e.getMessage)
            Left(e)
          case \/-(result) => Right(ReportServiceResult(jobId, errors, result))
        }
    }
  }

  def generateDocument[I, E](vorlage: BerichtsVorlage, fileType: FileType, id: Option[String], data: ReportData[E],
    pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]])(implicit personId: PersonId): ServiceResult[ReportResult] = {
    for {
      temp <- loadBerichtsvorlage(vorlage, fileType, id)
      source <- generateReport(temp, data, pdfGenerieren, pdfAblage)
    } yield source
  }

  def generateReport[I, E](vorlage: Array[Byte], data: ReportData[E], pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]])(implicit personId: PersonId): ServiceResult[ReportResult] = EitherT {
    implicit val timeout = Timeout(600 seconds)
    val collector =
      if (data.rows.size == 1) HeadReportResultCollector.props(reportSystem)
      else if (pdfAblage.isDefined) FileStoreReportResultCollector.props(reportSystem)
      else ZipReportResultCollector.props(reportSystem)

    val ref = actorSystem.actorOf(collector)
    (ref ? GenerateReports(personId, vorlage, data, pdfGenerieren, pdfAblage)).map(_.asInstanceOf[ReportResult].right)
  }

  def loadBerichtsvorlage(vorlage: BerichtsVorlage, fileType: FileType, id: Option[String]): ServiceResult[Array[Byte]] = {
    vorlage match {
      case EinzelBerichtsVorlage(file) => EitherT { Future { file.right } }
      case StandardBerichtsVorlage => resolveStandardBerichtsVorlage(fileType, id)
    }
  }

  /**
   * Resolve from S3 or as a local resource
   */
  def resolveStandardBerichtsVorlage(fileType: FileType, id: Option[String] = None): ServiceResult[Array[Byte]] = {
    resolveBerichtsVorlageFromFileStore(fileType, id) ||| resolveBerichtsVorlageFromResources(fileType, id)
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

  def defaultFileTypeId(fileType: FileType) = {
    fileType match {
      case VorlageRechnung => "Rechnung.odt"
      case VorlageDepotLieferschein => "DepotLieferschein.odt"
      case VorlageTourLieferschein => "TourLieferschein.odt"
      case VorlagePostLieferschein => "PostLieferschein.odt"
      case VorlageDepotLieferetiketten => "DepotLieferetiketten.odt"
      case VorlageTourLieferetiketten => "TourLieferetiketten.odt"
      case VorlagePostLieferetiketten => "PostLieferetiketten.odt"
      case VorlageMahnung => "Mahnung.odt"
      case VorlageBestellung => "Bestellung.odt"
      case _ => "undefined.odt"
    }
  }

  def resolveBerichtsVorlageFromResources(fileType: FileType, id: Option[String]): ServiceResult[Array[Byte]] = EitherT {
    logger.debug(s"Resolve template from resources:$fileType:$id")
    Future {
      val resourcePath = "/vorlagen/" + defaultFileTypeId(fileType)
      val idString = id.map(i => s"/$i").getOrElse("")
      val resource = s"$resourcePath$idString"
      logger.debug(s"Resolve template from resources:$resource")
      val is = getClass.getResourceAsStream(resource)
      is match {
        case null => ServiceFailed(s"Vorlage konnte im folgenden Pfad nicht gefunden werden: $resource").left
        case is =>
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
