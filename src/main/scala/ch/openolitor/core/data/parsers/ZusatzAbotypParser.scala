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
import ch.openolitor.util.DateTimeUtil
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ZusatzAbotypParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[ZusatzAbotyp, AbotypId]("id", Seq("name", "beschreibung", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten", "guthaben_mindestbestand", "admin_prozente", "wird_geplant",
      "kuendigungsfrist", "vertragslaufzeit", "anzahl_abonnenten", "anzahl_abonnenten_aktiv", "letzte_lieferung", "waehrung") ++ modifyColumns) { id => indexes => row =>
      import DateTimeUtil._

      //match column indexes
      val Seq(indexName, indexBeschreibung, indexPreis, indexPreiseinheit, indexAktivVon,
        indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten,
        indexGuthabenMindestbestand, indexAdminProzente, indexWirdGeplant, indexKuendigungsfrist, indexVertrag,
        indexAnzahlAbonnenten, indexAnzahlAbonnentenAktiv, indexLetzteLieferung, indexWaehrung) = indexes take (20)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val fristeinheitPattern = """(\d+)(M|W)""".r

      ZusatzAbotyp(
        id = AbotypId(id),
        name = row.value[String](indexName),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        aktivVon = row.value[Option[DateTime]](indexAktivVon),
        aktivBis = row.value[Option[DateTime]](indexAktivBis),
        preis = row.value[BigDecimal](indexPreis),
        preiseinheit = Preiseinheit(row.value[String](indexPreiseinheit)),
        laufzeit = row.value[Option[Int]](indexLaufzeit),
        laufzeiteinheit = Laufzeiteinheit(row.value[String](indexLaufzeiteinheit)),
        vertragslaufzeit = row.value[Option[String]](indexVertrag) map {
          case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
          case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
        },
        kuendigungsfrist = row.value[Option[String]](indexKuendigungsfrist) map {
          case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
          case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
        },
        anzahlAbwesenheiten = row.value[Option[Int]](indexAnzahlAbwesenheiten),
        farbCode = row.value[String](indexFarbCode),
        zielpreis = row.value[Option[BigDecimal]](indexZielpreis),
        guthabenMindestbestand = row.value[Int](indexGuthabenMindestbestand),
        adminProzente = row.value[BigDecimal](indexAdminProzente),
        wirdGeplant = row.value[Boolean](indexWirdGeplant),
        //Zusatzinformationen
        anzahlAbonnenten = row.value[Int](indexAnzahlAbonnenten),
        anzahlAbonnentenAktiv = row.value[Int](indexAnzahlAbonnentenAktiv),
        letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung),
        waehrung = Waehrung(row.value[String](indexWaehrung)),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}
