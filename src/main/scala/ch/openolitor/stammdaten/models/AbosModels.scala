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

case class AboId(id: Long) extends BaseId

sealed trait Abo extends BaseEntity[AboId] {
  val abotypId: AbotypId
  val kundeId: KundeId
  val kunde: String
  val start: DateTime
  val ende: Option[DateTime]
  val saldo: Int
  val saldoInRechnung: Int
  val letzteLieferung: Option[DateTime]
  //calculated fields
  val anzahlAbwesenheiten: TreeMap[String, Int]
  val anzahlLieferungen: TreeMap[String, Int]
}

sealed trait AboDetail {
  val abotypId: AbotypId
  val kundeId: KundeId
  val kunde: String
  val start: DateTime
  val ende: Option[DateTime]
  val saldo: Int
  val saldoInRechnung: Int
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
  val abotypId: AbotypId
  val abotypName: String
  val start: DateTime
  val ende: Option[DateTime]
}

case class DepotlieferungAbo(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends Abo

case class DepotlieferungAboDetail(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung]) extends AboDetail with JSONSerializable

case class DepotlieferungAboModify(kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  depotId: DepotId,
  depotName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None) extends AboModify

case class HeimlieferungAbo(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends Abo

case class HeimlieferungAboDetail(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung]) extends AboDetail with JSONSerializable

case class HeimlieferungAboModify(kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  tourId: TourId,
  tourName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None) extends AboModify

case class PostlieferungAbo(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends Abo

case class PostlieferungAboDetail(id: AboId,
  kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None,
  saldo: Int = 0,
  saldoInRechnung: Int = 0,
  letzteLieferung: Option[DateTime] = None,
  //calculated fields
  anzahlAbwesenheiten: TreeMap[String, Int] = TreeMap(),
  anzahlLieferungen: TreeMap[String, Int] = TreeMap(),
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId,
  abwesenheiten: Seq[Abwesenheit],
  lieferdaten: Seq[Lieferung]) extends AboDetail with JSONSerializable

case class PostlieferungAboModify(kundeId: KundeId,
  kunde: String,
  abotypId: AbotypId,
  abotypName: String,
  liefertag: Lieferzeitpunkt,
  start: DateTime,
  ende: Option[DateTime] = None) extends AboModify

case class AbwesenheitId(id: Long) extends BaseId

case class Abwesenheit(id: AbwesenheitId,
  aboId: AboId,
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: String,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[AbwesenheitId] with JSONSerializable

case class AbwesenheitModify(
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: String) extends JSONSerializable

case class AbwesenheitCreate(
  aboId: AboId,
  lieferungId: LieferungId,
  datum: DateTime,
  bemerkung: String) extends JSONSerializable
