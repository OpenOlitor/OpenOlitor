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
  implicit val laufzeiteinheitFormat = enumFormat(Laufzeiteinheit.apply)

  //id formats
  implicit val vertriebsartIdFormat = baseIdFormat(VertriebsartId.apply)
  implicit val abotypIdFormat = baseIdFormat(AbotypId.apply)
  implicit val depotIdFormat = baseIdFormat(DepotId.apply)
  implicit val tourIdFormat = baseIdFormat(TourId.apply)
  implicit val kundeIdFormat = baseIdFormat(KundeId.apply)
  implicit val personIdFormat = baseIdFormat(PersonId.apply)
  implicit val aboIdFormat = baseIdFormat(AboId.apply)
  implicit val customKundentypIdFormat = baseIdFormat(CustomKundentypId.apply)
  implicit val kundentypIdFormat = new JsonFormat[KundentypId] {
    def write(obj: KundentypId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): KundentypId =
      json match {
        case JsString(id) => KundentypId(id)
        case kt => sys.error(s"Unknown KundentypId:$kt")
      }
  }

  implicit val lieferzeitpunktFormat = new RootJsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      obj match {
        case w: Wochentag => w.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): Lieferzeitpunkt =
      json.convertTo[Wochentag]
  }

  implicit val depotlieferungFormat = jsonFormat4(Depotlieferung.apply)
  implicit val heimlieferungFormat = jsonFormat4(Heimlieferung.apply)
  implicit val postlieferungFormat = jsonFormat3(Postlieferung.apply)

  implicit val depot = jsonFormat21(Depot.apply)
  implicit val depotModify = jsonFormat19(DepotModify.apply)
  implicit val depotSummary = jsonFormat2(DepotSummary.apply)
  implicit val tour = jsonFormat3(Tour.apply)

  implicit val postlieferungDetailFormat = jsonFormat1(PostlieferungDetail.apply)
  implicit val depotlieferungDetailFormat = jsonFormat2(DepotlieferungDetail.apply)
  implicit val heimlieferungDetailFormat = jsonFormat2(HeimlieferungDetail.apply)

  implicit val vertriebsartDetailFormat = new JsonFormat[Vertriebsartdetail] {
    def write(obj: Vertriebsartdetail): JsValue =
      JsObject((obj match {
        case p: PostlieferungDetail => p.toJson
        case hl: HeimlieferungDetail => hl.toJson
        case dl: DepotlieferungDetail => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): Vertriebsartdetail =
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung")) => json.convertTo[PostlieferungDetail]
        case Seq(JsString("Heimlieferung")) => json.convertTo[HeimlieferungDetail]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungDetail]
      }
  }

  implicit val abotypFormat = jsonFormat16(Abotyp.apply)
  implicit val abotypDetailFormat = jsonFormat17(AbotypDetail.apply)
  implicit val abotypUpdateFormat = jsonFormat13(AbotypModify.apply)
  implicit val abotypSummaryFormat = jsonFormat2(AbotypSummary.apply)

  implicit val systemKundentypFormat = new JsonFormat[SystemKundentyp] {
    def write(obj: SystemKundentyp): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): SystemKundentyp =
      json match {
        case JsString(kundentyp) => SystemKundentyp.apply(kundentyp).getOrElse(sys.error(s"Unknown System-Kundentyp:$kundentyp"))
        case pt => sys.error(s"Unknown personentyp:$pt")
      }
  }

  implicit val customKundentypFormat = jsonFormat4(CustomKundentyp.apply)

  implicit val kundentypFormat = new JsonFormat[Kundentyp] {
    def write(obj: Kundentyp): JsValue =
      obj match {
        case s: SystemKundentyp =>
          systemKundentypFormat.write(s)
        case c: CustomKundentyp =>
          customKundentypFormat.write(c)
      }

    def read(json: JsValue): Kundentyp =
      json match {
        case system: JsString => systemKundentypFormat.read(json)
        case custom: JsObject => customKundentypFormat.read(json)
        case pt => sys.error(s"Unknown personentyp:$pt")
      }
  }

  implicit val depotaboFormat = jsonFormat8(DepotlieferungAbo.apply)
  implicit val depotaboModifyFormat = jsonFormat7(DepotlieferungAboModify.apply)
  implicit val heimlieferungAboFormat = jsonFormat8(HeimlieferungAbo.apply)
  implicit val postlieferungAboFormat = jsonFormat6(PostlieferungAbo.apply)

  implicit val aboFormat = new RootJsonFormat[Abo] {
    def write(obj: Abo): JsValue =
      obj match {
        case d: DepotlieferungAbo => d.toJson
        case h: HeimlieferungAbo => h.toJson
        case p: PostlieferungAbo => p.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): Abo = ???
  }

  implicit val aboModifyFormat = new RootJsonFormat[AboModify] {
    def write(obj: AboModify): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): AboModify = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboModify]
      } else {
        sys.error(s"Unknown AboModify for: $json")
      }
    }
  }

  implicit val personFormat = jsonFormat10(Person.apply)
  implicit val personModifyFormat = jsonFormat8(PersonModify.apply)
  implicit val kundeFormat = jsonFormat11(Kunde.apply)
  implicit val kundeDetailFormat = jsonFormat13(KundeDetail.apply)
  implicit val kundeModifyFormat = jsonFormat9(KundeModify.apply)
  implicit val kundeSummaryFormat = jsonFormat2(KundeSummary.apply)
  implicit val kundentypCreateFormat = jsonFormat2(CustomKundentypCreate.apply)
  implicit val kundentypModifyFormat = jsonFormat1(CustomKundentypModify.apply)
}
