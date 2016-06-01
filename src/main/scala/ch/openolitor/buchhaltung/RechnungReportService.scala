package ch.openolitor.buchhaltung

import ch.openolitor.buchhaltung.models.RechnungId
import scalaz._
import Scalaz._
import scala.concurrent.Future
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.ByteString
import ch.openolitor.core.filestore._
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.ReportSystem._
import akka.stream.scaladsl.Source

trait RechnungReportService extends AsyncConnectionPoolContextAware with ReportService with BuchhaltungJsonProtocol {
  self: BuchhaltungReadRepositoryComponent with ActorReferences with FileStoreComponent =>

  def generateRechnungReports(config: ReportConfig[RechnungId]): Future[ReportServiceResult[RechnungId]] = {
    generateReports[RechnungId, RechnungDetail](config, rechungenById, VorlageRechnung, None, GeneriertRechnung, x => Some(x.id.id.toString), pdfName)
  }

  def pdfName(rechnung: RechnungDetail) = {
    s"Rechnung Nr. ${rechnung.id.id}";
  }

  def rechungenById(rechnungIds: Seq[RechnungId]): Future[(Seq[ValidationError[RechnungId]], Seq[RechnungDetail])] = {
    val results = Future.sequence(rechnungIds.map { rechnungId =>
      buchhaltungReadRepository.getRechnungDetail(rechnungId).map(_.map { rechnung =>
        rechnung.status match {
          case Storniert =>
            Left(ValidationError[RechnungId](rechnungId, s"Für stornierte Rechnungen können keine Berichte mehr erzeugt werden"))
          case Bezahlt =>
            Left(ValidationError[RechnungId](rechnungId, s"Für bezahlte Rechnungen können keine Berichte mehr erzeugt werden"))
          case _ =>
            Right(rechnung)
        }

      }.getOrElse(Left(ValidationError[RechnungId](rechnungId, s"Rechnung konnte nicht gefunden werden"))))
    })
    results.map(_.partition(_.isLeft) match {
      case (a, b) => (a.map(_.left.get), b.map(_.right.get))
    })
  }
}