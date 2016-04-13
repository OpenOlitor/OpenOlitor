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
package ch.openolitor.stammdaten.models

import java.util.UUID

import ch.openolitor.core.models._
import java.util.Date
import org.joda.time.DateTime
import ch.openolitor.core.scalax.Product23

case class BaseProduzentId(id: String)

case class ProduzentId(id: UUID) extends BaseId

case class Produzent(id: ProduzentId,
  name: String,
  vorname: Option[String],
  kurzzeichen: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  email: String,
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  mwst: Boolean,
  mwstSatz: Option[BigDecimal],
  mwstNr: Option[String],
  aktiv: Boolean,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[ProduzentId] with Product23[ProduzentId, String, Option[String], String, Option[String], Option[String], Option[String], String, String, Option[String], String, Option[String], Option[String], Option[String], //maybe use dedicated type
  Option[String], Boolean, Option[BigDecimal], Option[String], Boolean, DateTime, UserId, DateTime, UserId]

case class ProduzentModify(
  name: String,
  vorname: Option[String],
  kurzzeichen: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  email: String,
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  mwst: Boolean,
  mwstSatz: Option[BigDecimal],
  mwstNr: Option[String],
  aktiv: Boolean) extends Product
