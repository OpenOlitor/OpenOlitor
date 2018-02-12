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

import org.specs2.mutable._
import java.nio.file.{ Files, Paths }
import java.io.FileInputStream
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class ZahlungsImportParserSpec extends Specification {
  "ZahlungsImportParser" should {

    "parse example esr file" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/esrimport.esr").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 225
    }

    "parse example esr file with blank lines" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/esrimport_with_blank_lines.esr").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 5
    }

    "parse example camt.054 file" in {
      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/camt_054_Beispiel_ZA1_ESR_ZE.xml").toURI()))

      val result = ZahlungsImportParser.parse(bytes)

      beSuccessfulTry(result)

      result.get.records.size === 1
    }
  }
}
