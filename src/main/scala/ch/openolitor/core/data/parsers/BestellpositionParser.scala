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

object BestellpositionParser extends EntityParser {
  import EntityParser._

  def parse(produkte: List[Produkt])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Bestellposition, BestellpositionId]("id", Seq("bestellung_id", "produkt_id", "preis_einheit", "einheit", "menge", "preis",
      "anzahl") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBestellungId, indexProduktId, indexPreisEinheit, indexEinheit, indexMenge, indexPreis, indexAnzahl) = indexes take (7)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produktId = Some(ProduktId(row.value[Long](indexProduktId)))
      val produkt = produkte.find(_.id == produktId) getOrElse (throw ParseException(s"No produkt found for id $produktId"))

      Bestellposition(
        BestellpositionId(id),
        bestellungId = BestellungId(row.value[Long](indexBestellungId)),
        produktId,
        //TODO: verify
        produktBeschrieb = produkt.name,
        preisEinheit = row.value[Option[BigDecimal]](indexPreisEinheit),
        einheit = Liefereinheit(row.value[String](indexEinheit)),
        menge = row.value[BigDecimal](indexMenge),
        preis = row.value[Option[BigDecimal]](indexPreis),
        anzahl = row.value[Int](indexAnzahl),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
