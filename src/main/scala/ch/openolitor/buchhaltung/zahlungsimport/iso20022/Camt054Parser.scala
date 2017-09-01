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

import org.joda.time.DateTime
import ch.openolitor.buchhaltung.zahlungsimport._
import ch.openolitor.buchhaltung.zahlungsimport.esr.ZahlungsImportEsrRecord._
import ch.openolitor.stammdaten.models.Waehrung
import ch.openolitor.stammdaten.models.CHF
import scala.util._
import scala.io.Source
import scala.xml.XML
import java.io.InputStream
import iso.std.iso.n20022.tech.xsd.camt05400106.BankToCustomerDebitCreditNotificationV06

class Camt054Parser extends ZahlungsImportParser {
  def parse(is: InputStream): Try[ZahlungsImportResult] = {
    Try(XML.load(is)) flatMap { node =>
      Try(scalaxb.fromXML[BankToCustomerDebitCreditNotificationV06](node)) flatMap {
        (new Camt054ToZahlungsImportTransformer).transform
      }
    }
  }
}

object Camt054Parser extends ZahlungsImportParser {
  def parse(is: InputStream): Try[ZahlungsImportResult] = {
    new Camt054Parser().parse(is)
  }
}
