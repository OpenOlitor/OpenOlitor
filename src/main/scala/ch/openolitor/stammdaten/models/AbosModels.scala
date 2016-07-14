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
import ch.openolitor.core.JSONSerializable
import scala.collection.immutable.TreeMap
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.JSONSerializable

case class AboId(id: Long) extends BaseId

sealed trait Abo extends BaseEntity[AboId] {
  val id: AboId
  val vertriebsartId: VertriebsartId
  val vertriebId: VertriebId
  val abotypId: AbotypId
  val abotypName: String
  val kundeId: KundeId
  val kunde: String
  val start: DateTime
  val ende: Option[DateTime]
  val guthabenVertraglich: Option[Int]
  val guthaben: Int
  val guthabenInRechnung: Int
  val letzteLieferung: Option[DateTime]
  //calculated fields
  val anzahlAbwesenheiten: TreeMap[String, Int]
  val anzahlLieferungen: TreeMap[String, Int]

  val erstelldat: DateTime
  val ersteller: PersonId
  val modifidat: DateTime
  val modifikator: PersonId
}

sealed trait AboDetail extends JSONSerializable {
  val vertriebsartId: VertriebsartId
  val vertriebId: VertriebId
  val abotypId: AbotypId
  val abotypName: String
  val kundeId: KundeId
  val kunde: String
  val start: DateTime
  val ende: Option[DateTime]
  val guthabenVertraglich: Option[Int]
  val guthaben: Int
  val guthabenInRechnung: Int
  val letzteLieferung: Option[DateTime]
  //calculated fields
  val anzahlAbwesenheiten: TreeMap[String, Int]
  val anzahlLieferungen: TreeMap[String, Int]
  val abwesenheiten: Seq[Abwesenheit]
  val lieferdaten: Seq[Lieferung]
}

sealed trait AboModify extends JSONSerializable {
  val kundeId: KundeId
  val kunde: String
  val vertriebsartId: VertriebsartId
  val start: DateTime
  val ende: Option[DateTime]
}

case class DepotlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

case class DepotlieferungAboDetail(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
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
  start: DateTime,
  ende: Option[DateTime]
) extends AboModify

case class HeimlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Abo

case class HeimlieferungAboDetail(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
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
  start: DateTime,
  ende: Option[DateTime]
) extends AboModify

case class PostlieferungAbo(
  id: AboId,
  kundeId: KundeId,
  kunde: String,
  vertriebsartId: VertriebsartId,
  vertriebId: VertriebId,
  abotypId: AbotypId,
  abotypName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
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
  abotypId: AbotypId,
  abotypName: String,
  start: DateTime,
  ende: Option[DateTime],
  guthabenVertraglich: Option[Int],
  guthaben: Int,
  guthabenInRechnung: Int,
  letzteLieferung: Option[DateTime],
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int],
  anzahlLieferungen: TreeMap[String, Int],
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
  start: DateTime,
  ende: Option[DateTime]
) extends AboModify

case class AbwesenheitId(id: Long) extends BaseId

case class Abwesenheit(
  id: AbwesenheitId,
  aboId: AboId,
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[AbwesenheitId] with JSONSerializable

case class AbwesenheitModify(
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: Option[String]
) extends JSONSerializable

case class AbwesenheitCreate(
  aboId: AboId,
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: Option[String]
) extends JSONSerializable

case class AboGuthabenModify(
  guthabenNeu: Int,
  bemerkung: String
) extends JSONSerializable

case class AboVertriebsartModify(
  vertriebsartIdNeu: VertriebsartId,
  bemerkung: String
) extends JSONSerializable

case class AboRechnungCreate(
  ids: Seq[AboId],
  titel: String,
  anzahlLieferungen: Option[Int],
  bisGuthaben: Option[Int],
  waehrung: Waehrung,
  betrag: Option[BigDecimal],
  rechnungsDatum: DateTime,
  faelligkeitsDatum: DateTime
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
