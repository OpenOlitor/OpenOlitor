package ch.openolitor.core.reporting

import akka.actor._
import ch.openolitor.core.filestore._
import java.util.UUID
import ch.openolitor.core.filestore.FileStoreActor.StoreFile

object SingleDocumentStoreReportPDFProcessorActor {
  def props(fileStore: FileStore, fileType: FileType, id: Option[String], name: String): Props = Props(classOf[SingleDocumentStoreReportPDFProcessorActor], fileType, id, name)
}

/**
 * This actor generates a report document, converts it to pdf and stored the pdf in the filestore
 */
class SingleDocumentStoreReportPDFProcessorActor(fileStore: FileStore, fileType: FileType, idOpt: Option[String], name: String) extends Actor with ActorLogging {
  import ReportSystem._

  val generatePdfActor = context.actorOf(SingleDocumentReportPDFProcessorActor.props(name), "generate-pdf-" + System.currentTimeMillis)
  val fileStoreActor = context.actorOf(FileStoreActor.props(fileStore), "file-store-" + System.currentTimeMillis)

  var origSender: Option[ActorRef] = None
  var id: String = ""

  val receive: Receive = {
    case cmd: GenerateReport =>
      origSender = Some(sender)
      generatePdfActor ! cmd
      context become waitingForDocumentResult
  }

  val waitingForDocumentResult: Receive = {
    case PdfReportResult(result, name) =>
      id = idOpt.getOrElse(UUID.randomUUID.toString)
      fileStoreActor ! StoreFile(fileType.bucket, Some(id), FileStoreFileMetadata(name, fileType), result)
      context become waitigForStoreCompleted
    case e: ReportError =>
      origSender.map(_ ! e)
      self ! PoisonPill
  }

  val waitigForStoreCompleted: Receive = {
    case FileStoreError(message) =>
      origSender.map(_ ! ReportError(message))
      self ! PoisonPill
    case FileStoreFileMetadata =>
      origSender.map(_ ! StoredPdfReportResult(fileType, FileStoreFileId(id)))
      self ! PoisonPill

  }
}