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


sealed trait Liefereinheit extends Product
case object Stueck extends Liefereinheit
case object Bund extends Liefereinheit
case object Gramm extends Liefereinheit
case object Kilogramm extends Liefereinheit

object Liefereinheit {
  def apply(value: String): Liefereinheit = {
    Vector(Stueck, Bund, Gramm, Kilogramm).find(_.toString == value).getOrElse(Kilogramm)
  }
}

case class BaseProduktekategorieId(id: String)

case class ProduktekategorieId(id: UUID) extends BaseId

case class Produktekategorie(id: ProduktekategorieId,
  val beschreibung: Option[String]) extends BaseEntity[ProduktekategorieId]  {
}

case class ProduktekategorieModify(beschreibung: Option[String]) extends Product 

case class ProduktId(id: UUID) extends BaseId

case class Produkt(
  id: ProduktId,
  name: String,
  verfuegbarVon: Int,
  verfuegbarBis: Int,
  kategorien: Set[BaseProduktekategorieId],
  einheit: Liefereinheit,
  preis: BigDecimal,
  produzenten: Set[BaseProduzentId]
) extends BaseEntity[ProduktId]

case class ProduktModify(
  name: String,
  verfuegbarVon: Int,
  verfuegbarBis: Int,
  kategorien: Set[ProduktekategorieId],
  einheit: Liefereinheit,
  preis: BigDecimal,
  produzenten: Set[ProduzentId]
) extends Product 

