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
import ch.openolitor.core.JSONSerializable
import scala.collection.immutable.TreeMap

case class VertriebId(id: Long) extends BaseId

case class Vertrieb(id: VertriebId, abotypId: AbotypId, liefertag: Lieferzeitpunkt, beschrieb: Option[String],
  anzahlAbos: Int,
  durchschnittspreis: TreeMap[String, BigDecimal],
  anzahlLieferungen: TreeMap[String, Int],
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends BaseEntity[VertriebId]

case class VertriebVertriebsarten(id: VertriebId, abotypId: AbotypId, liefertag: Lieferzeitpunkt, beschrieb: Option[String],
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  depotlieferungen: Seq[DepotlieferungDetail],
  heimlieferungen: Seq[HeimlieferungDetail],
  postlieferungen: Seq[Postlieferung],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends BaseEntity[VertriebId]

case class VertriebModify(abotypId: AbotypId, liefertag: Lieferzeitpunkt, beschrieb: Option[String]) extends JSONSerializable

case class VertriebsartId(id: Long) extends BaseId

sealed trait Vertriebsart extends BaseEntity[VertriebsartId] {
  val vertriebId: VertriebId
  val anzahlAbos: Int
  val anzahlAbosAktiv: Int
}

case class Depotlieferung(id: VertriebsartId, vertriebId: VertriebId, depotId: DepotId,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart

case class Heimlieferung(id: VertriebsartId, vertriebId: VertriebId, tourId: TourId,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart

case class Postlieferung(id: VertriebsartId, vertriebId: VertriebId,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart

sealed trait VertriebsartDetail extends JSONSerializable {
  val id: VertriebsartId
  val vertriebId: VertriebId
  val anzahlAbos: Int
  val anzahlAbosAktiv: Int
}
case class DepotlieferungDetail(id: VertriebsartId, vertriebId: VertriebId, depotId: DepotId, depot: DepotSummary,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail

case class HeimlieferungDetail(id: VertriebsartId, vertriebId: VertriebId, tourId: TourId, tour: Tour,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail

case class PostlieferungDetail(id: VertriebsartId, vertriebId: VertriebId,
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail

sealed trait VertriebsartModify extends JSONSerializable
case class DepotlieferungModify(depotId: DepotId) extends VertriebsartModify
case class HeimlieferungModify(tourId: TourId) extends VertriebsartModify
case class PostlieferungModify() extends VertriebsartModify

sealed trait VertriebsartAbotypModify extends JSONSerializable
case class DepotlieferungAbotypModify(vertriebId: VertriebId, depotId: DepotId) extends VertriebsartAbotypModify
case class HeimlieferungAbotypModify(vertriebId: VertriebId, tourId: TourId) extends VertriebsartAbotypModify
case class PostlieferungAbotypModify(vertriebId: VertriebId) extends VertriebsartAbotypModify
