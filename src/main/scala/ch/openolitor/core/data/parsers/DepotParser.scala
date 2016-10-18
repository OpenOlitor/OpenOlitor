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

object DepotParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Depot, DepotId]("id", Seq("name", "kurzzeichen", "ap_name", "ap_vorname", "ap_telefon", "ap_email", "v_name", "v_vorname",
      "v_telefon", "v_email", "strasse", "haus_nummer",
      "plz", "ort", "aktiv", "oeffnungszeiten", "farb_code", "iban", "bank", "beschreibung", "max_abonnenten", "anzahl_abonnenten", "anzahl_abonnenten_aktiv") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexKurzzeichen, indexApName, indexApVorname, indexApTelefon, indexApEmail,
        indexVName, indexVVorname, indexVTelefon, indexVEmail, indexStrasse, indexHausNummer, indexPLZ, indexOrt,
        indexAktiv, indexOeffnungszeiten, indexFarbCode, indexIBAN, indexBank, indexBeschreibung, indexMaxAbonnenten,
        indexAnzahlAbonnenten) = indexes take (22)
      val indexAnzahlAbonnentenAktiv = indexes(22)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      Depot(
        id = DepotId(id),
        name = row.value[String](indexName),
        kurzzeichen = row.value[String](indexKurzzeichen),
        apName = row.value[Option[String]](indexApName),
        apVorname = row.value[Option[String]](indexApVorname),
        apTelefon = row.value[Option[String]](indexApTelefon),
        apEmail = row.value[Option[String]](indexApEmail),
        vName = row.value[Option[String]](indexVName),
        vVorname = row.value[Option[String]](indexVVorname),
        vTelefon = row.value[Option[String]](indexVTelefon),
        vEmail = row.value[Option[String]](indexVEmail),
        strasse = row.value[Option[String]](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        plz = row.value[String](indexPLZ),
        ort = row.value[String](indexOrt),
        aktiv = row.value[Boolean](indexAktiv),
        oeffnungszeiten = row.value[Option[String]](indexOeffnungszeiten),
        farbCode = row.value[Option[String]](indexFarbCode),
        iban = row.value[Option[String]](indexIBAN),
        bank = row.value[Option[String]](indexBank),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        anzahlAbonnentenMax = row.value[Option[Int]](indexMaxAbonnenten),
        //Zusatzinformationen
        anzahlAbonnenten = row.value[Int](indexAnzahlAbonnenten),
        anzahlAbonnentenAktiv = row.value[Int](indexAnzahlAbonnentenAktiv),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
