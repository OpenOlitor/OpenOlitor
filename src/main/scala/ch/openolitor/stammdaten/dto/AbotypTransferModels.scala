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
package ch.openolitor.stammdaten.dto

import ch.openolitor.stammdaten._
import org.joda.time.DateTime
import ch.openolitor.core.models._
import scalikejdbc._

case class AbotypCreate(
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime],
  anzahlLieferungen: Option[Int],
  anzahlAbwesenheiten: Option[Int],
  preis: BigDecimal,
  preisEinheit: Preiseinheit,
  vertriebsarten: Seq[Vertriebsartdetail])

case class AbotypDetail(id: Option[AbotypId],
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime],
  anzahlLieferungen: Option[Int],
  anzahlAbwesenheiten: Option[Int],
  preis: BigDecimal,
  preisEinheit: Preiseinheit,
  aktiv: Boolean,
  vertriebsarten: Seq[Vertriebsartdetail],
  anzahlAbonnenten: Int,
  letzteLieferung: Option[DateTime]) extends BaseEntity[AbotypId] with IAbotyp

sealed trait Vertriebsartdetail extends Product
case class DepotlieferungDetail(id: Option[VertriebsartId], depot: Depot, liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsartdetail
case class HeimlieferungDetail(id: Option[VertriebsartId], tour: Tour, liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsartdetail
case class PostlieferungDetail(id: Option[VertriebsartId], liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsartdetail