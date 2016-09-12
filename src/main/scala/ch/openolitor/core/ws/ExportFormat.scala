package ch.openolitor.core.ws

import com.typesafe.scalalogging.LazyLogging

trait ExportFormat

case object Json extends ExportFormat
case object ODS extends ExportFormat

object ExportFormat extends LazyLogging {
  val AlleExportFormat = List(
    Json,
    ODS
  )

  def apply(value: String): ExportFormat = {
    logger.debug(s"ExportFormat.apply:$value")
    AlleExportFormat.find(_.toString.toLowerCase == value.toLowerCase).getOrElse(Json)
  }
}