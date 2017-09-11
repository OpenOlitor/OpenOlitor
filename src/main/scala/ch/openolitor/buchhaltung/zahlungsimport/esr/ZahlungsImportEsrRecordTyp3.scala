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
import ch.openolitor.stammdaten.models.Waehrung
import ch.openolitor.stammdaten.models.CHF

object EsrRecordTyp3EsrTyp {
  def apply(c: String): EsrTyp = c match {
    case "0" => Esr
    case "1" => EsrPlus
  }
}

object EsrRecordTyp3Transaktionsart {
  def apply(c: String): Transaktionsart = c match {
    case "2" => Gutschrift
    case "5" => Storno
    case "8" => Korrektur
  }
}

sealed trait EsrRecordTyp3Transaktionscode
case object Beleglos extends EsrRecordTyp3Transaktionscode
case object Postschalter extends EsrRecordTyp3Transaktionscode
case object Nachnahme extends EsrRecordTyp3Transaktionscode
case object EigenesKonto extends EsrRecordTyp3Transaktionscode

object EsrRecordTyp3Transaktionscode {
  def apply(c: String): EsrRecordTyp3Transaktionscode = c match {
    case "0" => Beleglos
    case "1" => Postschalter
    case "2" => Nachnahme
    case "3" => EigenesKonto
  }
}

sealed trait EsrRecordTyp3RejectCode
case object Kein extends EsrRecordTyp3RejectCode
case object Reject extends EsrRecordTyp3RejectCode
case object MassenReject extends EsrRecordTyp3RejectCode

case class EsrRecordTyp3Transaktionsartcode(esrTyp: EsrTyp, transaktionsCode: EsrRecordTyp3Transaktionscode, transaktionsart: Transaktionsart)

object EsrRecordTyp3Transaktionsartcode {
  private val R = """(\w{1})(\w{1})(\w{1})""".r

  def unapply(code: String): Option[EsrRecordTyp3Transaktionsartcode] = code match {
    case R(esrTyp, transaktionscode, transaktionsart) =>
      Some(EsrRecordTyp3Transaktionsartcode(EsrRecordTyp3EsrTyp(esrTyp), EsrRecordTyp3Transaktionscode(transaktionscode), EsrRecordTyp3Transaktionsart(transaktionsart)))
    case _ =>
      None
  }
}

object EsrRecordTyp3RejectCode {
  def apply(c: String): EsrRecordTyp3RejectCode = c match {
    case "0" => Kein
    case "1" => Reject
    case "5" => MassenReject
  }
}

case class EsrRecordTyp3(
    transaktionsartCode: EsrRecordTyp3Transaktionsartcode,
    teilnehmerNummer: Option[String],
    iban: Option[String],
    debitor: Option[String],
    referenzNummer: String,
    betrag: BigDecimal,
    aufgabereferenzen: String,
    aufgabeDatum: DateTime,
    verarbeitungsDatum: DateTime,
    gutschriftsDatum: DateTime,
    mikrofilmNummer: String,
    rejectCode: EsrRecordTyp3RejectCode,
    reserve: String,
    preiseFuerEinzahlungen: BigDecimal
) extends ZahlungsImportRecord {
  override val transaktionsart: Transaktionsart = transaktionsartCode.transaktionsart
  override val waehrung: Waehrung = CHF
}

object EsrRecordTyp3 {
  // Aufgabereferenzen ist im Dokument Recordstrukturen definiert als X(10) mit der Bemerkung 4 Zahlen, 2 SPACES und nochmals 4 Zahlen.
  // Der String ist aber abhängig von der Bank mit 2 Spaces oder anderen Zeichen vorzufinden. Deshalb ([\w\s]{10})
  private val R = """(\w{3})(\d{9})(\d{27})(\d{10})([\w\s]{10})(\d{6})(\d{6})(\d{6})(\d{9})(\d{1})(\w{9})(\d{4})""".r

  def unapply(line: String): Option[EsrRecordTyp3] = line match {
    case R(transaktionsartcode, teilnehmernummer, referenznummer, betrag, aufgabereferenzen, aufgabeDatum, verarbeitungsDatum, gutschriftsDatum, mikrofilmNummer, rejectCode, reserve, preiseFuerEinzahlungen) =>
      val EsrRecordTyp3Transaktionsartcode(code) = transaktionsartcode

      Some(EsrRecordTyp3(
        code,
        Some(teilnehmernummer),
        None,
        None,
        referenznummer,
        BigDecimal(betrag.toInt, Scale),
        aufgabereferenzen,
        DateTime.parse(aufgabeDatum, Format),
        DateTime.parse(verarbeitungsDatum, Format),
        DateTime.parse(gutschriftsDatum, Format),
        mikrofilmNummer,
        EsrRecordTyp3RejectCode(rejectCode),
        reserve,
        BigDecimal(preiseFuerEinzahlungen.toInt, Scale)
      ))
    case _ =>
      None
  }
}

