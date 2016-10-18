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

object VertriebsartParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Vertriebsart, VertriebsartId]("id", Seq("vertrieb_id", "depot_id", "tour_id", "anzahl_abos", "anzahl_abos_aktiv") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexVertriebId, indexDepotId, indexTourId, indexAnzahlAbos, indexAnzahlAbosAktiv) = indexes take (5)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val vertriebsartId = VertriebsartId(id)
      val vertriebId = VertriebId(row.value[Long](indexVertriebId))
      val depotIdOpt = row.value[Option[Long]](indexDepotId) map (DepotId)
      val tourIdOpt = row.value[Option[Long]](indexTourId) map (TourId)
      val anzahlAbos = row.value[Int](indexAnzahlAbos)
      val anzahlAbosAktiv = row.value[Int](indexAnzahlAbosAktiv)

      depotIdOpt map { depotId =>
        Depotlieferung(
          vertriebsartId,
          vertriebId, depotId, anzahlAbos, anzahlAbosAktiv,
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
      } getOrElse {
        tourIdOpt map { tourId =>
          Heimlieferung(vertriebsartId, vertriebId, tourId, anzahlAbos, anzahlAbosAktiv,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        } getOrElse {
          Postlieferung(vertriebsartId, vertriebId, anzahlAbos, anzahlAbosAktiv,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        }
      }
    }
  }
}
