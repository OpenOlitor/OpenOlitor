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
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.scalax.Tuple24
import java.text.DecimalFormat
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.scalax.Tuple25
import ch.openolitor.core.scalax.Tuple26

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

// rechnungsdatum sind Zeitstempel um für ISO 20022 gerüstet zu sein.
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
  fileStoreId: Option[String],
  anzahlMahnungen: Int,
  mahnungFileStoreIds: Set[String],
  // rechnungsadresse
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[RechnungId]

object Rechnung {
  def unapply(entity: Rechnung) = {
    Some(Tuple26(
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
      entity.fileStoreId,
      entity.anzahlMahnungen,
      entity.mahnungFileStoreIds,
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
  fileStoreId: Option[String],
  anzahlMahnungen: Int,
  mahnungFileStoreIds: Set[String],
  // rechnungsadresse
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class RechnungDetailReport(
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
    anzahlMahnungen: Int,
    // rechnungsadresse
    strasse: String,
    hausNummer: Option[String],
    adressZusatz: Option[String],
    plz: String,
    ort: String,
    // modification flags
    erstelldat: DateTime,
    ersteller: PersonId,
    modifidat: DateTime,
    modifikator: PersonId,
    projekt: ProjektReport
) extends JSONSerializable {
  lazy val referenzNummerFormatiert: String = referenzNummer.reverse.grouped(5).map(_.reverse).toList.reverse.mkString(" ")
  lazy val betragRappen = (betrag - betrag.toLong) * 100
}

case class RechnungCreate(
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
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String
) extends JSONSerializable

case class RechnungModify(
  titel: String,
  anzahlLieferungen: Int,
  waehrung: Waehrung,
  betrag: BigDecimal,
  einbezahlterBetrag: Option[BigDecimal],
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime,
  eingangsDatum: Option[DateTime],
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String
) extends JSONSerializable

case class RechnungModifyBezahlt(
  einbezahlterBetrag: BigDecimal,
  eingangsDatum: DateTime
) extends JSONSerializable

case class RechnungenContainer(ids: Seq[RechnungId]) extends JSONSerializable
