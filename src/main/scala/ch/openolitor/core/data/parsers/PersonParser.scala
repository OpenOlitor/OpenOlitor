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

object PersonParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Person, PersonId]("id", Seq("kunde_id", "anrede", "name", "vorname", "email", "email_alternative",
      "telefon_mobil", "telefon_festnetz", "bemerkungen", "sort", "login_aktiv", "passwort", "letzte_anmeldung", "passwort_wechsel", "rolle") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexAnrede, indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil,
          indexTelefonFestnetz, indexBemerkungen, indexSort, indexLoginAktiv, indexPasswort, indexLetzteAnmeldung, indexPasswortWechselErforderlich, indexRolle) = indexes take (15)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val kundeId = KundeId(row.value[Long](indexKundeId))

        Person(
          id = PersonId(id),
          kundeId = kundeId,
          anrede = row.value[Option[String]](indexAnrede) map (r => Anrede(r) getOrElse (throw ParseException(s"Unbekannte Anrede $r bei Person Nr. $id"))),
          name = row.value[String](indexName),
          vorname = row.value[String](indexVorname),
          email = row.value[Option[String]](indexEmail),
          emailAlternative = row.value[Option[String]](indexEmailAlternative),
          telefonMobil = row.value[Option[String]](indexTelefonMobil),
          telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
          bemerkungen = row.value[Option[String]](indexBemerkungen),
          sort = row.value[Int](indexSort),
          // security daten
          loginAktiv = row.value[Boolean](indexLoginAktiv),
          passwort = row.value[Option[String]](indexPasswort) map (_.toCharArray),
          letzteAnmeldung = row.value[Option[DateTime]](indexLetzteAnmeldung),
          passwortWechselErforderlich = row.value[Boolean](indexPasswortWechselErforderlich),
          rolle = row.value[Option[String]](indexRolle) map (r => Rolle(r) getOrElse (throw ParseException(s"Unbekannte Rolle $r bei Person Nr. $id"))),
          // modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}
