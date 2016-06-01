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
import akka.util.ByteString
import scalaz._
import Scalaz._
import ch.openolitor.core.filestore._
import ch.openolitor.core.reporting.ReportSystem._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.util.UUID
import ch.openolitor.util.ByteBufferBackedInputStream
import org.reactivestreams.Publisher
import akka.stream.scaladsl.Source
import akka.NotUsed
import spray.json.JsonFormat
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.JSONSerializable
import akka.stream.scaladsl.Sink
import akka.stream.ActorMaterializer

sealed trait BerichtsVorlage extends Product
case object StandardBerichtsVorlage extends BerichtsVorlage
//case class Berichtsvorlage(id: BerichtsVorlageId) extends BerichtsVorlage
case class EinzelBerichtsVorlage(file: ByteString) extends BerichtsVorlage
case class ServiceFailed(msg: String, e: Throwable) extends Exception(msg, e)

case class ReportForm[I](ids: Seq[I], pdfGenerieren: Boolean, pdfAblegen: Boolean) extends JSONSerializable {
  def toConfig(vorlage: BerichtsVorlage): ReportConfig[I] = {
    ReportConfig[I](ids, vorlage, pdfGenerieren, pdfAblegen)
  }
}
case class ReportConfig[I](ids: Seq[I], vorlage: BerichtsVorlage, pdfGenerieren: Boolean, pdfAblegen: Boolean)
case class ValidationError[I](id: I, message: String)
case class ReportServiceResult[I](jobId: JobId, validationErrors: Seq[ValidationError[I]], results: Source[ReportResult, _]) {
  val hasErrors = !validationErrors.isEmpty
  val singleReportResultSink: Sink[ReportResult, Future[ReportResult]] = Sink.head

  def single(implicit materializer: ActorMaterializer): Future[ReportResult] = {
    results.take(1).runWith(singleReportResultSink)
  }
}

object ServiceFailed {
  def apply(msg: String) = new ServiceFailed(msg, null)
}

trait ReportService {
  self: ActorReferences with FileStoreComponent =>

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
    ablageType: FileType,
    ablageIdFactory: E => Option[String],
    ablageNameFactory: E => String,
    jobId: JobId = JobId()
  ): Future[Either[ServiceFailed, ReportServiceResult[I]]] = {
    validationFunction(config.ids) flatMap {
      case (errors, result) =>
        val ablageParams = config.pdfAblegen match {
          case false => None
          case true => Some(FileStoreParameters[E](ablageType, ablageIdFactory, ablageNameFactory))
        }
        generateDocument(config.vorlage, vorlageType, vorlageId, ReportData(jobId, result), config.pdfGenerieren, ablageParams).run map {
          case -\/(e) => Left(e)
          case \/-(result) => Right(ReportServiceResult(jobId, errors, result))
        }
    }
  }

  def generateDocument[E](vorlage: BerichtsVorlage, fileType: FileType, id: Option[String], data: ReportData[E],
    pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]]): ServiceResult[Source[ReportResult, _]] = {
    for {
      temp <- loadBerichtsvorlage(vorlage, fileType, id)
      source <- generateReport(temp, data, pdfGenerieren, pdfAblage)
    } yield source
  }

  def generateReport[E](vorlage: ByteString, data: ReportData[E], pdfGenerieren: Boolean, pdfAblage: Option[FileStoreParameters[E]]): ServiceResult[Source[ReportResult, _]] = EitherT {
    Future {
      val publisher = Source.actorPublisher[ReportResult](ReportResultPublisher.props(reportSystem))
      reportSystem ! GenerateReports(vorlage, data, pdfGenerieren, pdfAblage)
      publisher.right
    }
  }

  def loadBerichtsvorlage(vorlage: BerichtsVorlage, fileType: FileType, id: Option[String]): ServiceResult[ByteString] = {
    vorlage match {
      case EinzelBerichtsVorlage(file) => EitherT { Future { file.right } }
      case StandardBerichtsVorlage => resolveBerichtsVorlageFromFileStore(fileType, id)
    }
  }

  /**
   * Resolve from S3 or local as a local resource
   */
  def resolveStandardBerichtsVorlageFromFileStore(fileType: FileType, id: Option[String] = None): ServiceResult[ByteString] = {
    resolveBerichtsVorlageFromFileStore(fileType, id) orElse resolveBerichtsVorlageFromResources(fileType, id)
  }

  def resolveBerichtsVorlageFromFileStore(fileType: FileType, id: Option[String]): ServiceResult[ByteString] = EitherT {
    fileStore.getFile(fileType.bucket, id.getOrElse(defaultFileTypeId(fileType))) map {
      case Left(e) => ServiceFailed(s"Vorlage konnte im FileStore nicht gefunden werden: $fileType, $id").left
      case Right(file) => ByteString(scala.io.Source.fromInputStream(file.file).mkString).right
    }
  }

  def defaultFileTypeId(fileType: FileType) = {
    fileType match {
      case VorlageRechnung => "Rechnung.odt"
      case VorlageEtikette => "Etiketten.odt"
      case VorlageMahnung => "Mahnung.odt"
      case VorlageBestellung => "Bestellung.odt"
      case _ => "undefined.odt"
    }
  }

  def resolveBerichtsVorlageFromResources(fileType: FileType, id: Option[String]): ServiceResult[ByteString] = EitherT {
    Future {
      val resourcePath = "vorlagen" + defaultFileTypeId(fileType)
      val idString = id.map(i => s"/$i").getOrElse("")
      val resource = s"/$resourcePath$idString"
      val is = getClass.getResourceAsStream(resource)
      is match {
        case null => ServiceFailed(s"Vorlage konnte im folgenden Pfad nicht gefunden werden: $resource").left
        case is => ByteString(scala.io.Source.fromInputStream(is).mkString).right
      }
    }
  }
}
