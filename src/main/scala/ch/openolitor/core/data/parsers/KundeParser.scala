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

object KundeParser extends EntityParser {
  import EntityParser._

  def parse(personen: List[Person])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Kunde, KundeId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort", "bemerkungen",
      "abweichende_lieferadresse", "bezeichnung_lieferung", "strasse_lieferung", "haus_nummer_lieferung",
      "adress_zusatz_lieferung", "plz_lieferung", "ort_lieferung", "zusatzinfo_lieferung", "typen",
      "anzahl_abos", "anzahl_abos_aktiv", "anzahl_pendenzen", "anzahl_personen") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexBemerkungen,
        indexAbweichendeLieferadresse, indexBezeichnungLieferung, indexStrasseLieferung, indexHausNummerLieferung,
        indexAdresseZusatzLieferung, indexPlzLieferung, indexOrtLieferung, indexZusatzinfoLieferung, indexKundentyp,
        indexAnzahlAbos, indexAnzahlAbosAktiv, indexAnzahlPendenzen, indexAnzahlPersonen) = indexes take (20)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val kundeId = KundeId(id)
      val personenByKundeId = personen filter (_.kundeId == kundeId)

      if (personenByKundeId.isEmpty) {
        throw ParseException(s"Kunde id $kundeId does not reference any person. At least one person is required")
      }

      val bez = row.value[Option[String]](indexBezeichnung) getOrElse (s"${personenByKundeId.head.vorname}  ${personenByKundeId.head.name}")

      Kunde(
        kundeId,
        bezeichnung = bez,
        strasse = row.value[String](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkungen),
        abweichendeLieferadresse = row.value[Boolean](indexAbweichendeLieferadresse),
        bezeichnungLieferung = row.value[Option[String]](indexBezeichnungLieferung),
        strasseLieferung = row.value[Option[String]](indexStrasseLieferung),
        hausNummerLieferung = row.value[Option[String]](indexHausNummerLieferung),
        adressZusatzLieferung = row.value[Option[String]](indexAdresseZusatzLieferung),
        plzLieferung = row.value[Option[String]](indexPlzLieferung),
        ortLieferung = row.value[Option[String]](indexOrtLieferung),
        zusatzinfoLieferung = row.value[Option[String]](indexZusatzinfoLieferung),
        typen = (row.value[String](indexKundentyp).split(",") map (KundentypId)).toSet,
        //Zusatzinformationen
        anzahlAbos = row.value[Int](indexAnzahlAbos),
        anzahlAbosAktiv = row.value[Int](indexAnzahlAbosAktiv),
        anzahlPendenzen = row.value[Int](indexAnzahlPendenzen),
        anzahlPersonen = row.value[Int](indexAnzahlPersonen),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
