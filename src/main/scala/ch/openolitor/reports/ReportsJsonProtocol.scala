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
package ch.openolitor.reports

import spray.json._
import ch.openolitor.core.models._
import java.util.UUID
import org.joda.time._
import org.joda.time.format._
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.reports.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.JSONSerializable
import zangelo.spray.json.AutoProductFormats

/**
 * JSON Format deklarationen für das Modul Reports
 */
trait ReportsJsonProtocol extends BaseJsonProtocol with LazyLogging with AutoProductFormats[JSONSerializable] {

  //id formats
  implicit val reportId = baseIdFormat(ReportId)

  implicit def dbResultMapFormat = new RootJsonFormat[Map[String, Any]] {
    def write(m: Map[String, Any]) = JsObject {
      m.map { field =>
        field._1 -> {
          field._2 match {
            case null => JsNull
            case _ => {
              field._1 match {
                case "passwort" => JsString("Not available")
                case _ => JsString(field._2.toString)
              }
            }
          }
        }
      }
    }

    def read(value: JsValue) = throw new UnsupportedOperationException("Map[String, Any] can not be created out of JSON-Object at the moment")
  }

}
