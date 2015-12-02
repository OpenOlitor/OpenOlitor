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
import scala.collection.SortedSet

@SerialVersionUID(111111)
case class AbotypCreate(
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime] = None,
  anzahlLieferungen: Option[Int] = None,
  anzahlAbwesenheiten: Option[Int] = None,
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  vertriebsarten: Set[Vertriebsartdetail],
  aktiv: Boolean) extends Product

case class AbotypDetail(id: AbotypId,
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime],
  anzahlLieferungen: Option[Int],
  anzahlAbwesenheiten: Option[Int],
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  aktiv: Boolean,
  vertriebsarten: Set[Vertriebsartdetail],
  anzahlAbonnenten: Int,
  letzteLieferung: Option[DateTime],
  waehrung: Waehrung = CHF) extends BaseEntity[AbotypId] with IAbotyp

@SerialVersionUID(111111)
case class AbotypUpdate(name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime],
  anzahlLieferungen: Option[Int],
  anzahlAbwesenheiten: Option[Int],
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  aktiv: Boolean,
  vertriebsarten: Set[Vertriebsartdetail],
  waehrung: Waehrung = CHF) extends Product

sealed trait Vertriebsartdetail extends Product
case class DepotlieferungDetail(depot: Depot, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail
case class HeimlieferungDetail(tour: Tour, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail
case class PostlieferungDetail(liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail