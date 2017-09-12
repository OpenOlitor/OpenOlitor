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

import org.specs2.mutable._
import org.joda.time.DateTime
import ch.openolitor.buchhaltung.zahlungsimport.Gutschrift

class ZahlungsImportEsrRecordTyp3Spec extends Specification {
  "ZahlungsImportEsrRecordTyp3" should {

    "extract esr line correctly" in {
      val EsrRecordTyp3(result) = "0020000000000000003078431000700031995000000038400112528627013112513112513112500000000000000000000000"

      result === EsrRecordTyp3(
        EsrRecordTyp3Transaktionsartcode(Esr, Beleglos, Gutschrift),
        Some("000000000"),
        None,
        None,
        "000000307843100070003199500",
        BigDecimal("384.00"),
        "1125286270",
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        "000000000",
        Kein,
        "000000000",
        BigDecimal("0")
      )
    }

    "extract esr line with spaces" in {
      val EsrRecordTyp3(result) = "00200000000000000030784310007000319950000000384001125  627013112513112513112500000000000000000000000"

      result === EsrRecordTyp3(
        EsrRecordTyp3Transaktionsartcode(Esr, Beleglos, Gutschrift),
        Some("000000000"),
        None,
        None,
        "000000307843100070003199500",
        BigDecimal("384.00"),
        "1125  6270",
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        new DateTime(2013, 11, 25, 0, 0, 0, 0),
        "000000000",
        Kein,
        "000000000",
        BigDecimal("0")
      )
    }
  }

  "EsrTotalRecordTyp3" should {

    "extract esr line correctly" in {
      val EsrTotalRecordTyp3(result) = "999000000000999999999999999999999999999000005721600000000000224140126000000000000000000  "

      result === EsrTotalRecordTyp3(
        Gutschrift,
        "000000000",
        "999999999999999999999999999",
        BigDecimal("57216.00"),
        224,
        new DateTime(2014, 1, 26, 0, 0, 0, 0),
        BigDecimal("0.00"),
        BigDecimal("0.00"),
        "  "
      )
    }
  }
}