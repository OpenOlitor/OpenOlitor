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
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging

/**
 * JSON Format deklarationen für das Modul Stammdaten
 */
object StammdatenJsonProtocol extends DefaultJsonProtocol with LazyLogging {
  import BaseJsonProtocol._

  //enum formats
  implicit val wochentagFormat = enumFormat(x => Wochentag.apply(x).getOrElse(Montag))
  implicit val rhythmusFormat = enumFormat(Rhythmus.apply)
  implicit val preiseinheitFormat = enumFormat(Preiseinheit.apply)
  implicit val waehrungFormat = enumFormat(Waehrung.apply)

  //id formats
  implicit val vertriebsartIdFormat = baseIdFormat(VertriebsartId.apply)
  implicit val abotypIdFormat = baseIdFormat(AbotypId.apply)
  implicit val depotIdFormat = baseIdFormat(DepotId.apply)
  implicit val tourIdFormat = baseIdFormat(TourId.apply)
  implicit val personIdFormat = baseIdFormat(PersonId.apply)
  implicit val aboIdFormat = baseIdFormat(AboId.apply)

  implicit val lieferzeitpunktFormat = new RootJsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      obj match {
        case w: Wochentag => w.toJson
        case _            => JsObject()
      }

    def read(json: JsValue): Lieferzeitpunkt =
      json.convertTo[Wochentag]
  }

  implicit val depotlieferungFormat = jsonFormat4(Depotlieferung.apply)
  implicit val heimlieferungFormat = jsonFormat4(Heimlieferung.apply)
  implicit val postlieferungFormat = jsonFormat3(Postlieferung.apply)

  implicit val depot = jsonFormat21(Depot.apply)
  implicit val depotModify = jsonFormat18(DepotModify.apply)
  implicit val depotSummary = jsonFormat2(DepotSummary.apply)
  implicit val tour = jsonFormat3(Tour.apply)

  implicit val postlieferungDetailFormat = jsonFormat1(PostlieferungDetail.apply)
  implicit val depotlieferungDetailFormat = jsonFormat2(DepotlieferungDetail.apply)
  implicit val heimlieferungDetailFormat = jsonFormat2(HeimlieferungDetail.apply)

  implicit val vertriebsartDetailFormat = new JsonFormat[Vertriebsartdetail] {
    def write(obj: Vertriebsartdetail): JsValue =
      JsObject((obj match {
        case p: PostlieferungDetail   => p.toJson
        case hl: HeimlieferungDetail  => hl.toJson
        case dl: DepotlieferungDetail => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): Vertriebsartdetail =
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung"))  => json.convertTo[PostlieferungDetail]
        case Seq(JsString("Heimlieferung"))  => json.convertTo[HeimlieferungDetail]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungDetail]
      }
  }

  implicit val abotypFormat = jsonFormat13(Abotyp.apply)
  implicit val abotypDetailFormat = jsonFormat14(AbotypDetail.apply)
  implicit val abotypUpdateFormat = jsonFormat11(AbotypModify.apply)

  implicit val personentypFormat = new JsonFormat[Personentyp] {
    def write(obj: Personentyp): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): Personentyp =
      json match {
        case JsString("Vereinsmitglied")    => Vereinsmitglied
        case JsString("Goenner")            => Goenner
        case JsString("Genossenschafterin") => Genossenschafterin
        case pt                             => sys.error(s"Unknown personentyp:$pt")
      }
  }

  implicit val person = jsonFormat14(Person.apply)
  implicit val personUpdateOrCreate = jsonFormat13(PersonModify.apply)

  implicit val depotaboFormat = jsonFormat9(DepotlieferungAbo.apply)
  implicit val depotaboModifyFormat = jsonFormat4(DepotlieferungAboModify.apply)
  implicit val heimlieferungAboFormat = jsonFormat9(HeimlieferungAbo.apply)
  implicit val postlieferungAboFormat = jsonFormat7(PostlieferungAbo.apply)

  implicit val aboFormat = new JsonFormat[Abo] {
    def write(obj: Abo): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): Abo = ???
  }
}
