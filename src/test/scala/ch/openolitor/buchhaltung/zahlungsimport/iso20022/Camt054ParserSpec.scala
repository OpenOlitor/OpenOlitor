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
package ch.openolitor.buchhaltung.zahlungsimport.iso20022

import org.specs2.mutable._
import org.joda.time.DateTime
import java.nio.file.{ Files, Paths }
import scala.io.Source
import ch.openolitor.stammdaten.models.CHF
import ch.openolitor.buchhaltung.zahlungsimport.Gutschrift
import org.joda.time.format.ISODateTimeFormat

class Camt054ParserSpec extends Specification {
  "Camt054Parser" should {
    "parse camt.054 XML file" in {
      val is = getClass.getResourceAsStream("/camt_054_Beispiel_ZA1_ESR_ZE.xml")

      val result = Camt054Parser.parse(is)

      beSuccessfulTry(result)

      result.get.records.head === Camt054Record(
        Some("010391391"),
        Some("CH160077401231234567"),
        Some("Pia Rutschmann"),
        "210000000003139471430009017",
        3949.75,
        CHF,
        Gutschrift,
        "",
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-15T09:30:47Z"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        ISODateTimeFormat.dateOptionalTimeParser.parseDateTime("2015-01-07"),
        "",
        0.0
      )
    }
  }
}
