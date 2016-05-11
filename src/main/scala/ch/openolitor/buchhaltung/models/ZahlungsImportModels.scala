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
package ch.openolitor.buchhaltung.models

import ch.openolitor.buchhaltung._
import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.stammdaten.models._

sealed trait ZahlungsImportStatus
case object Ok extends ZahlungsImportStatus

object ZahlungsImportStatus {
  def apply(value: String): ZahlungsImportStatus = {
    Vector(Ok).find(_.toString == value).getOrElse(Ok)
  }
}

case class ZahlungsEingangId(id: Long) extends BaseId

case class ZahlungsImportId(id: Long) extends BaseId

case class ZahlungsImport(
  id: ZahlungsImportId,
  file: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ZahlungsImportId]

case class ZahlungsEingang(
  id: ZahlungsEingangId,
  rechnungId: Option[RechnungId],
  transaktionsart: String,
  teilnehmerNummer: String,
  referenzNummer: String,
  waehrung: Waehrung,
  betrag: BigDecimal,
  aufgabeReferenzen: String,
  aufgabeDatum: DateTime,
  verarbeitungsDatum: DateTime,
  gutschriftsDatum: DateTime,
  status: ZahlungsImportStatus,
  esrNummer: String,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ZahlungsEingangId]
