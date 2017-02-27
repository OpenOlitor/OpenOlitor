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

import org.joda.time.DateTime
import ch.openolitor.core.models._
import java.util.UUID
import ch.openolitor.core.JSONSerializable

sealed trait KorbStatus

case object WirdGeliefert extends KorbStatus
case object Geliefert extends KorbStatus
case object FaelltAusAbwesend extends KorbStatus
case object FaelltAusSaldoZuTief extends KorbStatus

object KorbStatus {
  def apply(value: String): KorbStatus = {
    Vector(WirdGeliefert, Geliefert, FaelltAusAbwesend, FaelltAusSaldoZuTief) find (_.toString == value) getOrElse (WirdGeliefert)
  }
}

case class KorbId(id: Long) extends BaseId

case class Korb(
  id: KorbId,
  lieferungId: LieferungId,
  aboId: AboId,
  status: KorbStatus,
  guthabenVorLieferung: Int,
  auslieferungId: Option[AuslieferungId],
  sort: Option[Int],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KorbId]

case class KorbLieferung(
  id: KorbId,
  lieferungId: LieferungId,
  aboId: AboId,
  status: KorbStatus,
  guthabenVorLieferung: Int,
  auslieferungId: Option[AuslieferungId],
  sort: Option[Int],
  lieferung: Lieferung,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KorbDetail(
  id: KorbId,
  lieferungId: LieferungId,
  abo: Abo,
  status: KorbStatus,
  guthabenVorLieferung: Int,
  auslieferungId: Option[AuslieferungId],
  sort: Option[Int],
  kunde: Kunde,
  abotyp: Abotyp,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KorbReport(
  id: KorbId,
  lieferungId: LieferungId,
  abo: Abo,
  status: KorbStatus,
  guthabenVorLieferung: Int,
  auslieferungId: Option[AuslieferungId],
  sort: Option[Int],
  kunde: KundeReport,
  abotyp: Abotyp,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KorbModify(
  id: KorbId
) extends JSONSerializable

case class KorbCreate(
  LieferungId: LieferungId,
  aboId: AboId,
  status: KorbStatus,
  guthabenVorLieferung: Int
) extends JSONSerializable
