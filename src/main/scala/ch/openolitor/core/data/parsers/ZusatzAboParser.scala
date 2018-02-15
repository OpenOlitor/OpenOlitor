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
import org.joda.time.LocalDate
import akka.event.LoggingAdapter

object ZusatzAboParser extends EntityParser {
  import EntityParser._

  def parse(kundeIdMapping: Map[Long, KundeId], kunden: List[Kunde], vertriebsartIdMapping: Map[Long, VertriebsartId], vertriebsarten: List[Vertriebsart], vertriebe: List[Vertrieb],
    abotypen: List[ZusatzAbotyp], abos: List[Abo], abwesenheiten: List[Abwesenheit])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[ZusatzAbo, AboId]("id", Seq("haupt_abo_id", "abotyp_id", "kunde_id", "vertriebsart_id", "start", "ende",
      "letzte_lieferung", "anzahl_abwesenheiten", "anzahl_lieferungen") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(haupAboIdIndex, zusatzAbotypIdIndex, kundeIdIndex, vertriebsartIdIndex, startIndex, endeIndex,
          guthabenVertraglichIndex, guthabenIndex, guthabenInRechnungIndex, indexLetzteLieferung, indexAnzahlAbwesenheiten, lieferungenIndex) = indexes take (12)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val hauptAboIdInt = row.value[Long](haupAboIdIndex)
        val zusatzAbotypIdInt = row.value[Long](zusatzAbotypIdIndex)
        val kundeIdInt = row.value[Long](kundeIdIndex)
        val vertriebsartIdInt = row.value[Long](vertriebsartIdIndex)
        val start = row.value[LocalDate](startIndex)
        val ende = row.value[Option[LocalDate]](endeIndex)
        val aboId = AboId(id)

        val letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung)
        //calculate count
        val anzahlAbwesenheiten = parseTreeMap(row.value[String](indexAnzahlAbwesenheiten))(identity, _.toInt)
        val anzahlLieferungen = parseTreeMap(row.value[String](lieferungenIndex))(identity, _.toInt)

        val erstelldat = row.value[DateTime](indexErstelldat)
        val ersteller = PersonId(row.value[Long](indexErsteller))
        val modifidat = row.value[DateTime](indexModifidat)
        val modifikator = PersonId(row.value[Long](indexModifikator))

        val hauptAbo = abos find (_.id == AboId(hauptAboIdInt)) getOrElse (throw ParseException(s"Abo id $hauptAboIdInt referenced from zusatzabo not found"))
        val zusatzAbotyp = abotypen find (_.id == AbotypId(zusatzAbotypIdInt)) getOrElse (throw ParseException(s"ZusatzAbotyp id $zusatzAbotypIdInt referenced from zusatzabo not found"))
        val kundeId = kundeIdMapping getOrElse (kundeIdInt, throw ParseException(s"Kunde id $kundeIdInt referenced from abo not found"))
        val kunde = kunden find (_.id == kundeId) map (_.bezeichnung) getOrElse (throw ParseException(s"Kunde not found for id:$kundeId"))

        val aktiv = IAbo.calculateAktiv(start, ende)

        ZusatzAbo(
          id = aboId,
          hauptAboId = hauptAbo.id,
          hauptAbotypId = hauptAbo.abotypId,
          abotypId = zusatzAbotyp.id,
          abotypName = zusatzAbotyp.name,
          kundeId = kundeId,
          kunde = kunde,
          vertriebsartId = hauptAbo.vertriebsartId,
          vertriebId = hauptAbo.vertriebId,
          vertriebBeschrieb = hauptAbo.vertriebBeschrieb,
          start = start,
          ende = ende,
          letzteLieferung = letzteLieferung,
          //calculated fields
          anzahlAbwesenheiten = anzahlAbwesenheiten,
          anzahlLieferungen = anzahlLieferungen,
          aktiv = aktiv,
          //modification flags
          erstelldat = erstelldat,
          ersteller = ersteller,
          modifidat = modifidat,
          modifikator = modifikator
        )

    }
  }
}
