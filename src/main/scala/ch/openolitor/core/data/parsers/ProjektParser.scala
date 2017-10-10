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

object ProjektParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Projekt, ProjektId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort",
      "preise_sichtbar", "preise_editierbar", "email_erforderlich", "waehrung", "geschaeftsjahr_monat", "geschaeftsjahr_tag", "two_factor_auth", "sprache", "welcome_message_1", "welcome_message_2", "maintenance_mode") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexPreiseSichtbar,
          indexPreiseEditierbar, indexEmailErforderlich, indexWaehrung, indexGeschaeftsjahrMonat, indexGeschaeftsjahrTag, indexTwoFactorAuth, indexSprache, indexWelcomeMessage1, indexWelcomeMessage2, indexMaintenanceMode) = indexes take (17)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)
        val twoFactorAuth = parseMap(row.value[String](indexTwoFactorAuth))(r => Rolle(r).getOrElse(throw ParseException(s"Unknown Rolle $r while parsing Projekt")), _.toBoolean)

        Projekt(
          id = ProjektId(id),
          bezeichnung = row.value[String](indexBezeichnung),
          strasse = row.value[Option[String]](indexStrasse),
          hausNummer = row.value[Option[String]](indexHausNummer),
          adressZusatz = row.value[Option[String]](indexAdressZusatz),
          plz = row.value[Option[String]](indexPlz),
          ort = row.value[Option[String]](indexOrt),
          preiseSichtbar = row.value[Boolean](indexPreiseSichtbar),
          preiseEditierbar = row.value[Boolean](indexPreiseEditierbar),
          emailErforderlich = row.value[Boolean](indexEmailErforderlich),
          waehrung = Waehrung(row.value[String](indexWaehrung)),
          geschaeftsjahrMonat = row.value[Int](indexGeschaeftsjahrMonat),
          geschaeftsjahrTag = row.value[Int](indexGeschaeftsjahrTag),
          twoFactorAuthentication = twoFactorAuth,
          sprache = new Locale(row.value[String](indexSprache)),
          welcomeMessage1 = row.value[Option[String]](indexWelcomeMessage1),
          welcomeMessage2 = row.value[Option[String]](indexWelcomeMessage2),
          maintenanceMode = row.value[Boolean](indexMaintenanceMode),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}
