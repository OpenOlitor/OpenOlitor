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
import ch.openolitor.buchhaltung.zahlungsimport.esr.EsrRecordTyp3
import ch.openolitor.buchhaltung.zahlungsimport.esr.EsrTotalRecordTyp3
import scala.util._
import scala.io.Source
import java.io.InputStream
import ch.openolitor.buchhaltung.zahlungsimport.esr.EsrParser
import ch.openolitor.buchhaltung.zahlungsimport.iso20022.Camt054Parser
import java.io.ByteArrayInputStream

class ZahlungsImportParseException(message: String) extends Exception(message)

case class ZahlungsImportParseError(message: String)

trait ZahlungsImportParser {
  def parse(is: InputStream): Try[ZahlungsImportResult]
}

object ZahlungsImportParser {
  val importParsers: List[ZahlungsImportParser] =
    Camt054Parser :: EsrParser :: Nil

  /**
   * Try parsing the given bytes using all available import parsers until the first succeeds.
   *
   * @param bytes the data of the given file.
   * @return either a ZahlungsImportResult or Failure
   */
  def parse(bytes: Array[Byte]): Try[ZahlungsImportResult] = {
    importParsers map (_.parse(new ByteArrayInputStream(bytes))) find (_.isSuccess) getOrElse
      Failure(new IllegalArgumentException(s"Could not parse the input stream using the following parsers: $importParsers"))
  }
}