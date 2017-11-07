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
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object TourlieferungParser extends EntityParser {
  import EntityParser._

  def parse(abos: List[Abo], kunden: List[Kunde])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Tourlieferung, AboId]("id", Seq("sort") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexSort) = indexes take 1
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight 4

      val aboId = AboId(id)
      val abo = abos collect { case a: HeimlieferungAbo => a } find (_.id == aboId) getOrElse (throw ParseException(s"No abo found with id $aboId"))
      val kunde = kunden find (_.id == abo.kundeId) getOrElse (throw ParseException(s"No abo found with id $aboId"))

      val sort = row.value[Option[Int]](indexSort)

      Tourlieferung(
        aboId,
        abo.tourId,
        abo.abotypId,
        abo.kundeId,
        abo.vertriebsartId,
        abo.vertriebId,
        kunde.bezeichnung,
        kunde.strasse,
        kunde.hausNummer,
        kunde.adressZusatz,
        kunde.plz,
        kunde.ort,
        abo.abotypName,
        sort,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

}
