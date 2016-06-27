package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.filestore._
import ch.openolitor.core.filestore.FileStoreActor.StoreFile
import java.util.UUID
import java.util.Locale

object SingleDocumentStoreReportPDFProcessorActor {
  def props(fileStore: FileStore, sysConfig: SystemConfig, fileType: FileType, id: Option[String], name: String, locale: Locale): Props = Props(classOf[SingleDocumentStoreReportPDFProcessorActor], fileStore, sysConfig, fileType, id, name, locale)
}

/**
 * This actor generates a report document, converts it to pdf and stores the pdf in the filestore
 */
class SingleDocumentStoreReportPDFProcessorActor(fileStore: FileStore, sysConfig: SystemConfig, fileType: FileType, idOpt: Option[String], name: String, locale: Locale) extends Actor with ActorLogging {
  import ReportSystem._

  val generatePdfActor = context.actorOf(SingleDocumentReportPDFProcessorActor.props(sysConfig, name, locale), "generate-pdf-" + System.currentTimeMillis)
  val fileStoreActor = context.actorOf(FileStoreActor.props(fileStore), "file-store-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None

  val receive: Receive = {
    case cmd: GenerateReport =>
      origSender = Some(sender)
      generatePdfActor ! cmd
      context become waitingForDocumentResult
  }

  val waitingForDocumentResult: Receive = {
    case PdfReportResult(result, name) =>
      fileStoreActor ! StoreFile(fileType.bucket, Some(name), FileStoreFileMetadata(name, fileType), result)
      context become waitigForStoreCompleted
    case e: ReportError =>
      origSender map (_ ! e)
      self ! PoisonPill
  }

  val waitigForStoreCompleted: Receive = {
    case FileStoreError(message) =>
      origSender map (_ ! ReportError(message))
      self ! PoisonPill
    case FileStoreFileMetadata(name, _) =>
      origSender map (_ ! StoredPdfReportResult(fileType, FileStoreFileId(name)))
      self ! PoisonPill

  }
}