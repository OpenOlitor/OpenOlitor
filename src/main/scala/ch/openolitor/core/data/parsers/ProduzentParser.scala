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

object ProduzentParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Produzent, ProduzentId]("id", Seq("name", "vorname", "kurzzeichen", "strasse", "haus_nummer", "adress_zusatz",
      "plz", "ort", "bemerkung", "email", "telefon_mobil", "telefon_festnetz", "iban", "bank", "mwst", "mwst_satz", "mwst_nr", "aktiv") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexVorname, indexKurzzeichen, indexStrasse, indexHausNummer, indexAdressZusatz,
        indexPlz, indexOrt, indexBemerkung, indexEmail, indexTelefonMobil, indexTelefonFestnetz, indexIban, indexBank, indexMwst,
        indexMwstSatz, indexMwstNr, indexAktiv) = indexes take (18)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      Produzent(
        id = ProduzentId(id),
        name = row.value[String](indexName),
        vorname = row.value[Option[String]](indexVorname),
        kurzzeichen = row.value[String](indexKurzzeichen),
        strasse = row.value[Option[String]](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkung),
        email = row.value[String](indexEmail),
        telefonMobil = row.value[Option[String]](indexTelefonMobil),
        telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
        iban = row.value[Option[String]](indexIban),
        bank = row.value[Option[String]](indexBank),
        mwst = row.value[Boolean](indexMwst),
        mwstSatz = row.value[Option[BigDecimal]](indexMwstSatz),
        mwstNr = row.value[Option[String]](indexMwstNr),
        aktiv = row.value[Boolean](indexAktiv),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
