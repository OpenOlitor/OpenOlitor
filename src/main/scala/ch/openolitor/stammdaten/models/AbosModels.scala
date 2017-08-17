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

import ch.openolitor.stammdaten._
import ch.openolitor.core.models._
import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.core.JSONSerializable
import scala.collection.immutable.TreeMap
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.JSONSerializable
import ch.openolitor.kundenportal.models.BelongsToKunde
import ch.openolitor.core.scalax.Tuple23

case class AboId(id: Long) extends BaseId

object IAbo {
  def calculateAktiv(start: LocalDate, ende: Option[LocalDate]): Boolean = {
    val yesterday = LocalDate.now.minusDays(1)
    start.isBefore(LocalDate.now) && (ende map (_.isAfter(yesterday)) getOrElse true)
  }
}

sealed trait Abo extends BaseEntity[AboId] {
  val id: AboId
  val abotypId: AbotypId
  val vertriebsartId: VertriebsartId
  val vertriebId: VertriebId
  val vertriebBeschrieb: Option[String]
  val abotypName: String
  val kundeId: KundeId
  val kunde: String
  val start: LocalDate
  val ende: Option[LocalDate]
  val guthabenVertraglich: Option[Int]
  val guthaben: Int
  val guthabenInRechnung: Int
  val letzteLieferung: Option[DateTime]
  //calculated fields
  val anzahlAbwesenheiten: TreeMap[String, Int]
  val anzahlLieferungen: TreeMap[String, Int]
  val aktiv: Boolean

  def calculateAktiv: Boolean =
    IAbo.calculateAktiv(start, ende)
}

sealed trait AboReport extends Abo {
  val kundeReport: KundeReport
}

sealed trait AboDetail extends JSONSerializable {
  val vertriebsartId: VertriebsartId
  val vertriebId: VertriebId
  val vertriebBeschrieb: Option[String]
  val abotypId: AbotypId
  val abotypName: String
  val kundeId: KundeId
  val kunde: String
  val start: LocalDate
  val ende: Option[LocalDate]
  val guthabenVertraglich: Option[Int]
  val guthaben: Int
  val guthabenInRechnung: Int
  val letzteLieferung: Option[DateTime]
  //calculated fields
  val anzahlAbwesenheiten: TreeMap[String, Int]
  val anzahlLieferungen: TreeMap[String, Int]
  val abwesenheiten: Seq[Abwesenheit]
  val lieferdaten: Seq[Lieferung]
  val aktiv: Boolean
}

sealed trait AboModify extends JSONSerializable {
  val kundeId: KundeId
  val kunde: String
  val vertriebsartId: VertriebsartId
  val start: LocalDate
  val ende: Option[LocalDate]
}

case class DepotlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

object DepotlieferungAbo {
  def unapply(o: DepotlieferungAbo) = {
    Some(Tuple23(
      o.id,
      o.kundeId,
      o.kunde,
      o.vertriebsartId,
      o.vertriebId,
      o.vertriebBeschrieb,
      o.abotypId,
      o.abotypName,
      o.depotId,
      o.depotName,
      o.start,
      o.ende,
      o.guthabenVertraglich,
      o.guthaben,
      o.guthabenInRechnung,
      o.letzteLieferung,
      o.anzahlAbwesenheiten,
      o.anzahlLieferungen,
      o.aktiv,
      o.erstelldat,
      o.ersteller,
      o.modifidat,
      o.modifikator
    ))
  }
}

case class DepotlieferungAboReport(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  kundeReport: KundeReport,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AboReport with JSONSerializable

case class DepotlieferungAboDetail(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung],
  abotyp: Option[Abotyp],
  vertrieb: Option[Vertrieb]
) extends AboDetail

case class DepotlieferungAboModify(
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  depotId: DepotId,
  start: LocalDate,
  ende: Option[LocalDate]
) extends AboModify

case class HeimlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

object HeimlieferungAbo {
  def unapply(o: HeimlieferungAbo) = {
    Some(Tuple23(
      o.id,
      o.kundeId,
      o.kunde,
      o.vertriebsartId,
      o.vertriebId,
      o.vertriebBeschrieb,
      o.abotypId,
      o.abotypName,
      o.tourId,
      o.tourName,
      o.start,
      o.ende,
      o.guthabenVertraglich,
      o.guthaben,
      o.guthabenInRechnung,
      o.letzteLieferung,
      o.anzahlAbwesenheiten,
      o.anzahlLieferungen,
      o.aktiv,
      o.erstelldat,
      o.ersteller,
      o.modifidat,
      o.modifikator
    ))
  }
}

case class HeimlieferungAboDetail(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung],
  abotyp: Option[Abotyp],
  vertrieb: Option[Vertrieb]
) extends AboDetail

case class HeimlieferungAboModify(
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  tourId: TourId,
  start: LocalDate,
  ende: Option[LocalDate]
) extends AboModify

case class PostlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

case class PostlieferungAboDetail(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  abotypId: AbotypId,
  abotypName: String,
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung],
  abotyp: Option[Abotyp],
  vertrieb: Option[Vertrieb]
) extends AboDetail

case class PostlieferungAboModify(
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  start: LocalDate,
  ende: Option[LocalDate]
) extends AboModify

case class AbwesenheitId(id: Long) extends BaseId

case class Abwesenheit(
  id: AbwesenheitId,
  aboId: AboId,
  lieferungId: LieferungId,
  datum: LocalDate,
  bemerkung: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[AbwesenheitId] with JSONSerializable

case class AbwesenheitModify(
  lieferungId: LieferungId,
  datum: LocalDate,
  bemerkung: Option[String]
) extends JSONSerializable

case class AbwesenheitCreate(
  aboId: AboId,
  lieferungId: LieferungId,
  datum: LocalDate,
  bemerkung: Option[String]
) extends JSONSerializable

case class AboGuthabenModify(
  guthabenAlt: Int,
  guthabenNeu: Int,
  bemerkung: String
) extends JSONSerializable

case class AboVertriebsartModify(
  vertriebsartIdNeu: VertriebsartId,
  bemerkung: String
) extends JSONSerializable

case class AboRechnungsPositionBisGuthabenCreate(
  ids: Seq[AboId],
  titel: String,
  bisGuthaben: Int,
  waehrung: Waehrung
) extends JSONSerializable

case class AboRechnungsPositionBisAnzahlLieferungenCreate(
  ids: Seq[AboId],
  titel: String,
  anzahlLieferungen: Int,
  betrag: Option[BigDecimal],
  waehrung: Waehrung
) extends JSONSerializable

case class Tourlieferung(
  id: AboId,
  tourId: TourId,
  abotypId: AbotypId,
  kundeId: KundeId,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  kundeBezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  abotypName: String,
  sort: Option[Int],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[AboId]

object Tourlieferung {
  def apply(heimlieferungAbo: HeimlieferungAbo, kunde: Kunde, personId: PersonId): Tourlieferung = {
    Tourlieferung(
      heimlieferungAbo.id,
      heimlieferungAbo.tourId,
      heimlieferungAbo.abotypId,
      heimlieferungAbo.kundeId,
      heimlieferungAbo.vertriebsartId,
      heimlieferungAbo.vertriebId,
      kunde.bezeichnungLieferung getOrElse kunde.bezeichnung,
      kunde.strasseLieferung getOrElse kunde.strasse,
      kunde.hausNummerLieferung orElse kunde.hausNummer,
      kunde.adressZusatzLieferung orElse kunde.adressZusatz,
      kunde.plzLieferung getOrElse kunde.plz,
      kunde.ortLieferung getOrElse kunde.ort,
      heimlieferungAbo.abotypName,
      None,
      DateTime.now,
      personId,
      DateTime.now,
      personId
    )
  }
}

case class ZusatzAbo(
  id: AboId,
  hauptAboId: AboId,
  hauptAbotypId: AbotypId,
  abotypId: AbotypId,
  abotypName: String,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  start: LocalDate,
  ende: Option[LocalDate],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

object ZusatzAbo {
  def unapply(o: ZusatzAbo) = {
    Some(Tuple23(
      o.id,
      o.hauptAboId,
      o.hauptAbotypId,
      o.abotypId,
      o.abotypName,
      o.kundeId,
      o.kunde,
      o.vertriebsartId,
      o.vertriebId,
      o.vertriebBeschrieb,
      o.start,
      o.ende,
      o.guthabenVertraglich,
      o.guthaben,
      o.guthabenInRechnung,
      o.letzteLieferung,
      o.anzahlAbwesenheiten,
      o.anzahlLieferungen,
      o.aktiv,
      o.erstelldat,
      o.ersteller,
      o.modifidat,
      o.modifikator
    ))
  }
}