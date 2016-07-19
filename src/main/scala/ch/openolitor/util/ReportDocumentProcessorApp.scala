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
package ch.openolitor.util

import ch.openolitor.core.reporting.DocumentProcessor
import java.io.File
import org.odftoolkit.simple._
import scala.io.Source
import spray.json._
import DefaultJsonProtocol._
import scala.util._
import java.util.Locale

/**
 * This helper app reads a template filename and a json data filename from the args and tries
 * to generate a report with those informations. This app may be used to verify the report engine
 * as well as verify custom report templates within a console
 *
 */
object ReportDocumentProcessorApp extends App with DocumentProcessor {

  if (args.length < 2) {
    sys.error(s"Missing argument <template file> <json data file> <output file>")
  }
  val template = new File(args(0))
  if (!template.exists) {
    sys.error(s"Template file $template does not exist!")
  }

  val jsonDataFile = new File(args(1))
  if (!jsonDataFile.exists) {
    sys.error(s"Json data file $jsonDataFile does not exist!")
  }

  // load template
  println(s"Load template document...")
  val doc = TextDocument.loadDocument(template)

  // load json data
  println(s"Load json data...")
  val jsonData = Source.fromFile(jsonDataFile).getLines.mkString.parseJson

  // process report
  println(s"Process report...")
  processDocument(doc, jsonData, Locale.GERMAN) match {
    case Success(true) =>
      val outFile = if (args.length > 2) {
        new File(args(2))
      } else {
        File.createTempFile("report", ".odt")
      }

      // save result
      println(s"Save result document...")
      doc.save(outFile)
      println(s"Report successful processed. Output file: $outFile")
    case Failure(error) =>
      println(s"Failed processing report")
      error.printStackTrace
  }

}