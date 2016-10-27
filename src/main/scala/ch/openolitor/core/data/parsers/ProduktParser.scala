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
package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProduktParser extends EntityParser {
  import EntityParser._

  def parse(produzenten: List[Produzent], produktProduzenten: List[ProduktProduzent], produktkategorien: List[Produktekategorie], produktProduktekategorien: List[ProduktProduktekategorie])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Produkt, ProduktId]("id", Seq("name", "verfuegbar_von", "verfuegbar_bis", "standard_menge", "einheit",
      "preis") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexVerfuegbarVon, indexVerfuegbarBis, indexStandardMenge, indexEinheit,
        indexPreis) = indexes take (6)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produktId = ProduktId(id)
      val produzentenIds = produktProduzenten filter (_.produktId == produktId) map (_.produzentId)
      val produzentenName = produzenten filter (p => produzentenIds.contains(p.id)) map (_.kurzzeichen)

      val kategorienIds = produktProduktekategorien filter (_.produktId == produktId) map (_.produktekategorieId)
      val kategorien = produktkategorien filter (p => kategorienIds.contains(p.id)) map (_.beschreibung)

      Produkt(
        produktId,
        name = row.value[String](indexName),
        verfuegbarVon = Liefersaison(row.value[String](indexVerfuegbarVon)),
        verfuegbarBis = Liefersaison(row.value[String](indexVerfuegbarBis)),
        kategorien,
        standardmenge = row.value[Option[BigDecimal]](indexStandardMenge),
        einheit = Liefereinheit(row.value[String](indexEinheit)),
        preis = row.value[BigDecimal](indexPreis),
        produzentenName,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

}
