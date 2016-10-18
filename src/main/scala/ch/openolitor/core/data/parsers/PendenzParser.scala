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

object PendenzParser extends EntityParser {
  import EntityParser._

  def parse(kunden: List[Kunde])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Pendenz, PendenzId]("id", Seq("kunde_id", "datum", "bemerkung", "status", "generiert") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexDatum, indexBemerkung, indexStatus, indexGeneriert) = indexes take (5)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val kundeId = KundeId(row.value[Long](indexKundeId))
        val kunde = kunden.find(_.id == kundeId).headOption getOrElse (throw ParseException(s"Kunde not found with id $kundeId"))
        Pendenz(
          id = PendenzId(id),
          kundeId = kundeId,
          kundeBezeichnung = kunde.bezeichnung,
          datum = row.value[DateTime](indexDatum),
          bemerkung = row.value[Option[String]](indexBemerkung),
          generiert = row.value[Boolean](indexGeneriert),
          status = PendenzStatus(row.value[String](indexStatus)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}
