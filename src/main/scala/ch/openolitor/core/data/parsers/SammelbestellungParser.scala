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

object SammelbestellungParser extends EntityParser {
  import EntityParser._

  def parse(produzenten: List[Produzent], lieferplanungen: List[Lieferplanung])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Sammelbestellung, SammelbestellungId]("id", Seq("produzent_id", "lieferplanung_id", "datum", "datum_abrechnung", "preis_total", "steuer_satz", "steuer", "total_steuer", "datum_versendet") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduzentId, indexLieferplanungId, indexDatum, indexDatumAbrechnung, indexPreisTotal, indexSteuerSatz, indexSteuer, indexTotalSteuer, indexDatumVersendet) = indexes take (6)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produzentId = ProduzentId(row.value[Long](indexProduzentId))
      val produzent = produzenten.find(_.id == produzentId) getOrElse (throw ParseException(s"No produzent found with id $produzentId"))

      val lieferplanungId = LieferplanungId(row.value[Long](indexLieferplanungId))

      Sammelbestellung(
        id = SammelbestellungId(id),
        produzentId = produzentId,
        produzentKurzzeichen = produzent.kurzzeichen,
        lieferplanungId,
        status = Offen,
        datum = row.value[DateTime](indexDatum),
        datumAbrechnung = row.value[Option[DateTime]](indexDatumAbrechnung),
        preisTotal = row.value[BigDecimal](indexPreisTotal),
        steuerSatz = row.value[Option[BigDecimal]](indexSteuerSatz),
        steuer = row.value[BigDecimal](indexSteuer),
        totalSteuer = row.value[BigDecimal](indexTotalSteuer),
        datumVersendet = row.value[Option[DateTime]](indexDatumVersendet),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
