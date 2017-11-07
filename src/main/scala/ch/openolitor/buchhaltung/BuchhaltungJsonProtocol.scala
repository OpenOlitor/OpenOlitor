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
package ch.openolitor.buchhaltung

import spray.json._
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.buchhaltung.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.JSONSerializable
import zangelo.spray.json.AutoProductFormats

/**
 * JSON Format deklarationen für das Modul Buchhaltung
 */
trait BuchhaltungJsonProtocol extends BaseJsonProtocol with LazyLogging with AutoProductFormats[JSONSerializable] with StammdatenJsonProtocol {

  implicit val rechnungStatusFormat = enumFormat(RechnungStatus.apply)
  implicit val rechnungsPositionStatusFormat = enumFormat(RechnungsPositionStatus.apply)
  implicit val rechnungsPositionTypFormat = enumFormat(RechnungsPositionTyp.apply)
  implicit val zahlungsEingangStatusFormat = enumFormat(ZahlungsEingangStatus.apply)

  //id formats
  implicit val rechnungIdFormat = baseIdFormat(RechnungId)
  implicit val rechnungsPositionIdFormat = baseIdFormat(RechnungsPositionId)
  implicit val zahlungsImportIdFormat = baseIdFormat(ZahlungsImportId)
  implicit val zahlungsEingangIdFormat = baseIdFormat(ZahlungsEingangId)

  // special report formats
  def enhancedRechnungDetailFormatDef(implicit defaultFormat: JsonFormat[RechnungDetailReport]): RootJsonFormat[RechnungDetailReport] = new RootJsonFormat[RechnungDetailReport] {
    def write(obj: RechnungDetailReport): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        ("referenzNummerFormatiert" -> JsString(obj.referenzNummerFormatiert),
          "betragRappen" -> JsNumber(obj.betragRappen)))
    }

    def read(json: JsValue): RechnungDetailReport = defaultFormat.read(json)
  }

  implicit val enhancedRechnungDetailFormat = enhancedRechnungDetailFormatDef
}
