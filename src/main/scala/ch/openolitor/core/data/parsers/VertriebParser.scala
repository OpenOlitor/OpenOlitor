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

object VertriebParser extends EntityParser {
  import EntityParser._

  def parse(vertriebsarten: List[Vertriebsart])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Vertrieb, VertriebId]("id", Seq("abotyp_id", "beschrieb", "liefertag", "anzahl_abos", "anzahl_abos_aktiv", "durchschnittspreis", "anzahl_lieferungen") ++ modifyColumns) { id => indexes => row =>
      val Seq(indexAbotypId, indexBeschrieb, indexLiefertag, indexAnzahlAbos, indexAnzahlAbosAktiv, indexDurchschnittspreis, indexLieferungen) = indexes take (7)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val vertriebId = VertriebId(id)
      val abotypId = AbotypId(row.value[Long](indexAbotypId))
      val beschrieb = row.value[Option[String]](indexBeschrieb)
      val liefertag = Lieferzeitpunkt(row.value[String](indexLiefertag))
      val anzahlAbos = row.value[Int](indexAnzahlAbos)
      val anzahlAbosAktiv = row.value[Int](indexAnzahlAbosAktiv)

      val anzahlLieferungen = parseTreeMap(row.value[String](indexLieferungen))(identity, _.toInt)
      val durchschnittspreis = parseTreeMap(row.value[String](indexDurchschnittspreis))(identity, BigDecimal(_))

      Vertrieb(vertriebId, abotypId, liefertag, beschrieb,
        anzahlAbos, durchschnittspreis, anzahlLieferungen, anzahlAbosAktiv,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator)))
    }

  }
}
