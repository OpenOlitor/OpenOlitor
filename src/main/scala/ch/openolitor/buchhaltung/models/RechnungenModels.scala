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
package ch.openolitor.buchhaltung.models

import ch.openolitor.buchhaltung._
import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.scalax.Tuple23

/**
 *        +
 *        |
 *   +----v-----+
 *   | ERSTELLT |
 *   +----+-----+
 *        |
 *   +----v-------+
 *   | VERSCHICKT +-----------+
 *   +----+-------+           |
 *        |                   |
 *   +----v----+              |
 *   | BEZAHLT |              |
 *   +---------+              |
 *                            |
 *   +--------------------+   |
 *   | MAHNUNG_VERSCHICKT <---+
 *   +--------------------+   |
 *                            |
 *   +-----------+            |
 *   | STORNIERT <------------+
 *   +-----------+
 */
sealed trait RechnungStatus
case object Erstellt extends RechnungStatus
case object Verschickt extends RechnungStatus
case object Bezahlt extends RechnungStatus
case object MahnungVerschickt extends RechnungStatus
case object Storniert extends RechnungStatus

object RechnungStatus {
  def apply(value: String): RechnungStatus = {
    Vector(Erstellt, Verschickt, Bezahlt, MahnungVerschickt, Storniert).find(_.toString == value).getOrElse(Erstellt)
  }
}

case class RechnungId(id: Long) extends BaseId

case class Rechnung(
  id: RechnungId,
  kundeId: KundeId,
  aboId: AboId,
  titel: String,
  anzahlLieferungen: Int,
  waehrung: Waehrung,
  betrag: BigDecimal,
  einbezahlterBetrag: Option[BigDecimal],
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime,
  eingangsDatum: Option[DateTime],
  status: RechnungStatus,
  referenzNummer: String,
  esrNummer: String,
  // rechnungsadresse
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[RechnungId]

object Rechnung {
  def unapply(entity: Rechnung) = {
    Some(Tuple23(
      entity.id,
      entity.kundeId,
      entity.aboId,
      entity.titel,
      entity.anzahlLieferungen,
      entity.waehrung,
      entity.betrag,
      entity.einbezahlterBetrag,
      entity.rechnungsDatum,
      entity.faelligkeitsDatum,
      entity.eingangsDatum,
      entity.status,
      entity.referenzNummer,
      entity.esrNummer,
      entity.strasse,
      entity.hausNummer,
      entity.adressZusatz,
      entity.plz,
      entity.ort,
      entity.erstelldat,
      entity.ersteller,
      entity.modifidat,
      entity.modifikator
    ))
  }
}

case class RechnungDetail(
  id: RechnungId,
  kunde: Kunde,
  abo: Abo,
  titel: String,
  anzahlLieferungen: Int,
  waehrung: Waehrung,
  betrag: BigDecimal,
  einbezahlterBetrag: Option[BigDecimal],
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime,
  eingangsDatum: Option[DateTime],
  status: RechnungStatus,
  referenzNummer: String,
  esrNummer: String,
  // rechnungsadresse
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends JSONSerializable

case class RechnungModify(
  kundeId: KundeId,
  aboId: AboId,
  titel: String,
  anzahlLieferungen: Int,
  waehrung: Waehrung,
  betrag: BigDecimal,
  einbezahlterBetrag: Option[BigDecimal],
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime,
  eingangsDatum: Option[DateTime],
  status: RechnungStatus,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String
) extends JSONSerializable
