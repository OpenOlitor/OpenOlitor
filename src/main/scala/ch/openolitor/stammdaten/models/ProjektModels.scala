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
package ch.openolitor.stammdaten.models

import java.util.UUID
import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable

case class ProjektId(id: UUID) extends BaseId

case class Projekt(id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean = true,
  preiseEditierbar: Boolean = false,
  waehrung: Waehrung = CHF,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[ProjektId]

case class ProjektModify(
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean = true,
  preiseEditierbar: Boolean = false,
  waehrung: Waehrung = CHF) extends JSONSerializable

case class KundentypId(id: String)

case class CustomKundentypId(id: UUID) extends BaseId

trait Kundentyp {
  val kundentyp: KundentypId
  val beschreibung: Option[String] = None
  def system: Boolean
}

case class CustomKundentyp(id: CustomKundentypId,
  override val kundentyp: KundentypId,
  override val beschreibung: Option[String],
  anzahlVerknuepfungen: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[CustomKundentypId] with Kundentyp {
  override def system = false
}

case class CustomKundentypModify(beschreibung: Option[String]) extends JSONSerializable
case class CustomKundentypCreate(kundentyp: KundentypId, beschreibung: Option[String]) extends JSONSerializable

sealed trait SystemKundentyp extends Kundentyp with Product {
  override def system = true
}

object SystemKundentyp {

  val ALL = Vector(Vereinsmitglied, Goenner, Genossenschafterin)

  def parse(value: String): Option[SystemKundentyp] = {
    ALL.find(_.toString == value)
  }
}

case object Vereinsmitglied extends SystemKundentyp {
  override val kundentyp = KundentypId("Vereinsmitglied")
}

case object Goenner extends SystemKundentyp {
  override val kundentyp = KundentypId("Goenner")
}

case object Genossenschafterin extends SystemKundentyp {
  override val kundentyp = KundentypId("Genossenschafterin")
}
