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
package ch.openolitor.stammdaten

import spray.json._
import ch.openolitor.core.models._
import java.util.UUID
import org.joda.time._
import org.joda.time.format._
import ch.openolitor.core.BaseJsonProtocol

/**
 * JSON Format deklarationen für das Modul Stammdaten
 */
object StammdatenJsonProtocol extends DefaultJsonProtocol {
  import BaseJsonProtocol._

  //enum formats
  implicit val wochentagFormat = enumFormat(x => Wochentag.apply(x).getOrElse(Montag))
  implicit val rhythmusFormat = enumFormat(Rhythmus.apply)
  implicit val preiseinheitFormat = enumFormat(Preiseinheit.apply)
  implicit val waehrungFormat = enumFormat(Waehrung.apply)

  //id formats
  implicit val vertriebsartIdFormat = baseIdFormat(VertriebsartId.apply)
  implicit val abortypIdFormat = baseIdFormat(AbotypId.apply)
  implicit val depotIdFormat = baseIdFormat(DepotId.apply)
  implicit val tourIdFormat = baseIdFormat(TourId.apply)

  implicit val lieferzeitpunktFormat = new RootJsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      JsObject((obj match {
        case w: Wochentag => w.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))

    def read(json: JsValue): Lieferzeitpunkt =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("Wochentag")) => json.convertTo[Wochentag]
      }
  }

  implicit val depotlieferungFormat = jsonFormat4(Depotlieferung.apply)
  implicit val heimlieferungFormat = jsonFormat4(Heimlieferung.apply)
  implicit val postlieferungFormat = jsonFormat3(Postlieferung.apply)

  implicit val vertriebsartFormat = new JsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      JsObject((obj match {
        case w: Wochentag => w.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))

    def read(json: JsValue): Lieferzeitpunkt =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("Wochentag")) => json.convertTo[Wochentag]
      }
  }

  implicit val abottypFormat = jsonFormat10(Abotyp.apply)
}
