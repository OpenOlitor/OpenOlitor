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

sealed trait LieferungStatus extends Product

case object Offen extends LieferungStatus
case object InBearbeitung extends LieferungStatus
case object Bearbeitet extends LieferungStatus

object LieferungStatus {
  def apply(value: String): LieferungStatus = {
    Vector(Offen, InBearbeitung, Bearbeitet).find(_.toString == value).getOrElse(Offen)
  }
}

case class LieferungId(id: UUID) extends BaseId

case class Lieferung(id: LieferungId,
  abotypId: AbotypId,
  vertriebsartId: VertriebsartId,
  datum: DateTime,
  anzahlAbwesenheiten: Int,
  status: LieferungStatus) extends BaseEntity[LieferungId]

case class LieferungModify(datum: DateTime)

case class LieferungAbotypCreate(datum: DateTime, abotypId: AbotypId, vertriebsartId: VertriebsartId)