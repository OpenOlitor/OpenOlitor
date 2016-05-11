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

case class VertriebsartId(id: Long) extends BaseId
sealed trait Vertriebsart extends BaseEntity[VertriebsartId]
case class Depotlieferung(id: VertriebsartId, abotypId: AbotypId, depotId: DepotId, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart
case class Heimlieferung(id: VertriebsartId, abotypId: AbotypId, tourId: TourId, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart
case class Postlieferung(id: VertriebsartId, abotypId: AbotypId, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends Vertriebsart

sealed trait VertriebsartDetail extends JSONSerializable
case class DepotlieferungDetail(id: VertriebsartId, abotypId: AbotypId, depotId: DepotId, depot: DepotSummary, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail
case class HeimlieferungDetail(id: VertriebsartId, abotypId: AbotypId, tourId: TourId, tour: Tour, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail
case class PostlieferungDetail(id: VertriebsartId, abotypId: AbotypId, liefertag: Lieferzeitpunkt,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId) extends VertriebsartDetail

sealed trait VertriebsartModify extends JSONSerializable
case class DepotlieferungModify(depotId: DepotId, liefertag: Lieferzeitpunkt) extends VertriebsartModify
case class HeimlieferungModify(tourId: TourId, liefertag: Lieferzeitpunkt) extends VertriebsartModify
case class PostlieferungModify(liefertag: Lieferzeitpunkt) extends VertriebsartModify

sealed trait VertriebsartAbotypModify extends JSONSerializable
case class DepotlieferungAbotypModify(abotypId: AbotypId, depotId: DepotId, liefertag: Lieferzeitpunkt) extends VertriebsartAbotypModify
case class HeimlieferungAbotypModify(abotypId: AbotypId, tourId: TourId, liefertag: Lieferzeitpunkt) extends VertriebsartAbotypModify
case class PostlieferungAbotypModify(abotypId: AbotypId, liefertag: Lieferzeitpunkt) extends VertriebsartAbotypModify
