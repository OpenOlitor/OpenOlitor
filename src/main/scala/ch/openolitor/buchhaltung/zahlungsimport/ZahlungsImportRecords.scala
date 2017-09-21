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
package ch.openolitor.buchhaltung.zahlungsimport

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ch.openolitor.stammdaten.models.Waehrung

sealed trait Transaktionsart
case object Gutschrift extends Transaktionsart
case object Storno extends Transaktionsart
case object Korrektur extends Transaktionsart

trait ZahlungsImportRecordResult {
  val betrag: BigDecimal
  val transaktionsart: Transaktionsart
}

trait ZahlungsImportRecord extends ZahlungsImportRecordResult {
  val teilnehmerNummer: Option[String]
  val iban: Option[String]
  val debitor: Option[String]
  val referenzNummer: String
  val waehrung: Waehrung
  val aufgabeDatum: DateTime
  val verarbeitungsDatum: DateTime
  val gutschriftsDatum: DateTime
}

trait ZahlungsImportTotalRecord extends ZahlungsImportRecordResult

case class ZahlungsImportResult(records: Seq[ZahlungsImportRecordResult])