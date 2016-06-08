package ch.openolitor.core.reporting

import ch.openolitor.core.BaseJsonProtocol
import zangelo.spray.json.AutoProductFormats
import spray.json._
import ch.openolitor.core.reporting._
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.core.filestore.FileStoreJsonProtocol

trait ReportJsonProtocol extends BaseJsonProtocol with FileStoreJsonProtocol
    with AutoProductFormats[JSONSerializable] {
  implicit val jobIdFormat = jsonFormat1(JobId)
}