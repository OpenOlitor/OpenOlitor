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

import scala.util.Try

import ch.openolitor.buchhaltung.zahlungsimport.{ Gutschrift, Transaktionsart, ZahlungsImportParseException, ZahlungsImportResult }
import ch.openolitor.generated.xsd.{ BankToCustomerDebitCreditNotificationV06, Document }
import ch.openolitor.stammdaten.models.Waehrung

import org.joda.time.format.ISODateTimeFormat

import javax.xml.datatype.XMLGregorianCalendar

object Camt054v06Transaktionsart {
  def apply(c: String): Transaktionsart = c match {
    case "CRDT" => Gutschrift
    case _ => throw new ZahlungsImportParseException(s"unable to match $c")
  }
}

class Camt054v06ToZahlungsImportTransformer {
  def transform(input: Document): Try[ZahlungsImportResult] = {
    transform(input.BkToCstmrDbtCdtNtfctn)
  }

  def transform(input: BankToCustomerDebitCreditNotificationV06): Try[ZahlungsImportResult] = {
    val groupHeader = input.GrpHdr // Level A

    Try(ZahlungsImportResult(input.Ntfctn flatMap { notification => // Level B
      notification.Ntry flatMap { entry => // Level C
        entry.NtryDtls flatMap { entryDetail => // Level D.1
          entryDetail.TxDtls map { transactionDetail => // Level D.2
            Camt054Record(
              entry.NtryRef,
              Some(notification.Acct.Id.accountidentification4choiceoption.as[String]),
              (transactionDetail.RltdPties flatMap (_.Dbtr flatMap (_.Nm))),
              transactionDetail.RmtInf map (_.Strd match {
                case Nil => ""
                case structures => (structures map (_.CdtrRefInf flatMap (_.Ref))).flatten.mkString(",")
              }) getOrElse "", // Referenznummer
              (transactionDetail.AmtDtls flatMap (_.TxAmt map (_.Amt.value))) getOrElse (throw new ZahlungsImportParseException("Missing Betrag")),
              (transactionDetail.AmtDtls flatMap (_.TxAmt map (txAmt => Waehrung.applyUnsafe(txAmt.Amt.Ccy)))).getOrElse(throw new ZahlungsImportParseException("Missing Waehrung")),
              Camt054v06Transaktionsart(transactionDetail.CdtDbtInd.toString),
              "",
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(groupHeader.CreDtTm.toGregorianCalendar.toString),
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(entry.BookgDt.get.dateanddatetimechoiceoption.as[XMLGregorianCalendar].toGregorianCalendar.toString),
              ISODateTimeFormat.dateOptionalTimeParser.parseDateTime(entry.ValDt.get.dateanddatetimechoiceoption.as[XMLGregorianCalendar].toGregorianCalendar.toString),
              "",
              0.0
            )
          }
        }
      }
    }))
  }
}
