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

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.scalax.Tuple24
import ch.openolitor.core.JSONSerializable

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

object RechnungsPositionStatus {
  sealed trait RechnungsPositionStatus
  case object Offen extends RechnungsPositionStatus
  case object Zugewiesen extends RechnungsPositionStatus
  case object Bezahlt extends RechnungsPositionStatus
  case object Storniert extends RechnungsPositionStatus

  def apply(value: String): RechnungsPositionStatus = {
    Vector(Offen, Zugewiesen, Bezahlt, Storniert).find(_.toString == value).getOrElse(Offen)
  }
}

object RechnungsPositionTyp {
  sealed trait RechnungsPositionTyp
  case object Abo extends RechnungsPositionTyp

  def apply(value: String): RechnungsPositionTyp = {
    Vector(Abo).find(_.toString == value).getOrElse(Abo)
  }
}

case class RechnungId(id: Long) extends BaseId
case class RechnungsPositionId(id: Long) extends BaseId

case class RechnungsPosition(
  id: RechnungsPositionId,
  rechnungId: Option[RechnungId],
  parentRechnungsPositionId: Option[RechnungsPositionId],
  aboId: Option[AboId],
  kundeId: KundeId,
  betrag: BigDecimal,
  waehrung: Waehrung,
  anzahlLieferungen: Option[Int],
  beschrieb: String,
  status: RechnungsPositionStatus.RechnungsPositionStatus,
  typ: RechnungsPositionTyp.RechnungsPositionTyp,
  sort: Option[Int],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[RechnungsPositionId]

// rechnungsdatum sind Zeitstempel um für ISO 20022 gerüstet zu sein.
case class Rechnung(
  id: RechnungId,
  kundeId: KundeId,
  titel: String,
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
    Some(Tuple24(
      entity.id,
      entity.kundeId,
      entity.titel,
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

case class RechnungsPositionDetail(
  id: RechnungsPositionId,
  abo: Abo,
  betrag: BigDecimal,
  waehrung: Waehrung,
  anzahlLieferungen: Option[Int],
  beschrieb: String,
  status: RechnungsPositionStatus.RechnungsPositionStatus,
  typ: RechnungsPositionTyp.RechnungsPositionTyp,
  sort: Option[Int],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class RechnungDetail(
  id: RechnungId,
  kunde: Kunde,
  titel: String,
  waehrung: Waehrung,
  betrag: BigDecimal,
  rechnungsPositionen: Seq[RechnungsPositionDetail],
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
    kontoDaten: KontoDaten,
    titel: String,
    waehrung: Waehrung,
    betrag: BigDecimal,
    rechnungsPositionen: Seq[RechnungsPositionDetail],
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

case class RechnungCreateFromRechnungsPositionen(
  kundeId: KundeId,
  titel: String,
  waehrung: Waehrung,
  betrag: BigDecimal,
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

case class RechnungsPositionCreate(
  kundeId: KundeId,
  aboId: Option[AboId],
  beschrieb: String,
  anzahlLieferungen: Option[Int],
  betrag: BigDecimal,
  waehrung: Waehrung,
  status: RechnungsPositionStatus.RechnungsPositionStatus,
  typ: RechnungsPositionTyp.RechnungsPositionTyp
) extends JSONSerializable

case class RechnungsPositionModify(
  beschrieb: String,
  anzahlLieferungen: Option[Int],
  betrag: BigDecimal,
  status: RechnungsPositionStatus.RechnungsPositionStatus
) extends JSONSerializable

case class RechnungModifyBezahlt(
  einbezahlterBetrag: BigDecimal,
  eingangsDatum: DateTime
) extends JSONSerializable

case class RechnungenContainer(ids: Seq[RechnungId]) extends JSONSerializable

case class RechnungsPositionenCreateRechnungen(
  ids: Seq[RechnungsPositionId],
  titel: String,
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime
) extends JSONSerializable

case class RechnungsPositionAssignToRechnung(
  rechnungId: RechnungId,
  sort: Int
) extends JSONSerializable
