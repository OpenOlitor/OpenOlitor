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

sealed trait Vertriebskanal {
  val name: String
}

sealed trait VertriebskanalDetail extends Vertriebskanal {
  val beschreibung: Option[String]
}

case class DepotId(id: UUID) extends BaseId

case class Depot(id: DepotId,
  name: String,
  apName: Option[String],
  apVorname: Option[String],
  apTelefon: Option[String],
  apEmail: Option[String],
  vName: Option[String],
  vVorname: Option[String],
  vTelefon: Option[String],
  vEmail: Option[String],
  strasse: Option[String],
  hausNummer: Option[String],
  plz: String,
  ort: String,
  aktiv: Boolean,
  oeffnungszeiten: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  beschreibung: Option[String],
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  anzahlAbonnentenMax: Int) extends BaseEntity[DepotId] with Vertriebskanal

case class DepotModify(
  name: String,
  apName: Option[String],
  apVorname: Option[String],
  apTelefon: Option[String],
  apEmail: Option[String],
  vName: Option[String],
  vVorname: Option[String],
  vTelefon: Option[String],
  vEmail: Option[String],
  strasse: Option[String],
  hausNummer: Option[String],
  plz: String,
  ort: String,
  aktiv: Boolean,
  oeffnungszeiten: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  beschreibung: Option[String]) extends Product

case class TourId(id: UUID) extends BaseId
case class Tour(id: TourId, name: String, beschreibung: Option[String]) extends BaseEntity[TourId] with Vertriebskanal
