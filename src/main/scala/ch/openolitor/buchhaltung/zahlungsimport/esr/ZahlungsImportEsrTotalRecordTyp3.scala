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
package ch.openolitor.buchhaltung.zahlungsimport.esr

import org.joda.time.DateTime
import ch.openolitor.buchhaltung.zahlungsimport._
import ch.openolitor.buchhaltung.zahlungsimport.esr.ZahlungsImportEsrRecord._

object EsrTotalRecordTyp3Transaktionsartcode {
  def apply(c: String): Transaktionsart = c match {
    case "999" => Gutschrift // Gutschrift/Korrektur
    case "995" => Storno
  }
}

case class EsrTotalRecordTyp3(
  transaktionsart: Transaktionsart,
  teilnehmerNummer: String,
  sortierSchluessel: String,
  betrag: BigDecimal,
  anzahlTransaktionen: Int,
  erstellungsDatumMedium: DateTime,
  preiseFuerEinzahlungen: BigDecimal,
  nachbearbeitungEsrPlus: BigDecimal,
  reserve: String
) extends ZahlungsImportTotalRecord

object EsrTotalRecordTyp3 {
  private val R = """(\w{3})(\d{9})(\d{27})(\d{12})(\d{12})(\d{6})(\d{9})(\d{9})([\w\s]{0,13})""".r

  def unapply(line: String): Option[EsrTotalRecordTyp3] = line match {
    case R(transaktionsartcode, teilnehmernummer, sortierSchluessel, betrag, anzahlTransaktionen, erstellungsDatumMedium, preiseFuerEinzahlungen, nachbearbeitungEsrPlus, reserve) =>
      Some(EsrTotalRecordTyp3(
        EsrTotalRecordTyp3Transaktionsartcode(transaktionsartcode),
        teilnehmernummer,
        sortierSchluessel,
        BigDecimal(betrag.toInt, Scale),
        anzahlTransaktionen.toInt,
        DateTime.parse(erstellungsDatumMedium, Format),
        BigDecimal(preiseFuerEinzahlungen.toInt, Scale),
        BigDecimal(nachbearbeitungEsrPlus.toInt, Scale),
        reserve
      ))
    case _ =>
      None
  }
}

