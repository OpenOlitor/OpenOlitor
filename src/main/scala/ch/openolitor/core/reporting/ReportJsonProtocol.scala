package ch.openolitor.core.reporting

import ch.openolitor.core.BaseJsonProtocol
import zangelo.spray.json.AutoProductFormats
import spray.json._
import ch.openolitor.core.reporting._
import ch.openolitor.core.JSONSerializable

trait ReportJsonProtocol extends BaseJsonProtocol with AutoProductFormats[JSONSerializable] {

}