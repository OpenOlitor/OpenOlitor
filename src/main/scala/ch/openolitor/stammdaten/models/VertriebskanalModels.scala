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
import org.joda.time.DateTime
import ch.openolitor.core.scalax.Product26

sealed trait Vertriebskanal {
  val name: String
}

sealed trait VertriebskanalDetail extends Vertriebskanal {
  val beschreibung: Option[String]
}

case class DepotId(id: UUID) extends BaseId

case class Depot(id: DepotId,
  name: String,
  kurzzeichen: String,
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
  //farbCode: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  beschreibung: Option[String],
  anzahlAbonnentenMax: Option[Int],
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[DepotId] with Vertriebskanal with Product26[DepotId,
  String,String,Option[String],Option[String],Option[String],Option[String],Option[String],Option[String],Option[String],Option[String],Option[String],Option[String],String,String,Boolean,Option[String],Option[String],  Option[String], Option[String], Option[Int],Int,DateTime,UserId,DateTime,UserId]

case class DepotModify(
  name: String,
  kurzzeichen: String,
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
  farbCode: Option[String],
  iban: Option[String], //maybe use dedicated type
  bank: Option[String],
  beschreibung: Option[String],
  anzahlAbonnentenMax: Option[Int]) extends Product

case class DepotSummary(
  id: DepotId,
  name: String) extends Product

case class TourId(id: UUID) extends BaseId

case class Tour(
  id: TourId,
  name: String,
  beschreibung: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[TourId] with Vertriebskanal

case class TourModify(
  name: String,
  beschreibung: Option[String]) extends Product
