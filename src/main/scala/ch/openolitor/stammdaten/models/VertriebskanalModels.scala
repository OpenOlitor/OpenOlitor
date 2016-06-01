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
import ch.openolitor.core.scalax.Product27
import ch.openolitor.core.scalax.Tuple27
import ch.openolitor.core.JSONSerializable

sealed trait Vertriebskanal {
  val name: String
}

sealed trait VertriebskanalDetail extends Vertriebskanal with JSONSerializable {
  val beschreibung: Option[String]
}

case class DepotId(id: Long) extends BaseId

case class Depot(
  id: DepotId,
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
  anzahlAbonnentenMax: Option[Int],
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[DepotId] with Vertriebskanal

object Depot {
  def unapply(d: Depot) = {
    Some(Tuple27(
      d.id: DepotId,
      d.name: String,
      d.kurzzeichen: String,
      d.apName: Option[String],
      d.apVorname: Option[String],
      d.apTelefon: Option[String],
      d.apEmail: Option[String],
      d.vName: Option[String],
      d.vVorname: Option[String],
      d.vTelefon: Option[String],
      d.vEmail: Option[String],
      d.strasse: Option[String],
      d.hausNummer: Option[String],
      d.plz: String,
      d.ort: String,
      d.aktiv: Boolean,
      d.oeffnungszeiten: Option[String],
      d.farbCode: Option[String],
      d.iban: Option[String], //maybe use dedicated type
      d.bank: Option[String],
      d.beschreibung: Option[String],
      d.anzahlAbonnentenMax: Option[Int],
      //Zusatzinformationen
      d.anzahlAbonnenten: Int,
      //modification flags
      d.erstelldat: DateTime,
      d.ersteller: PersonId,
      d.modifidat: DateTime,
      d.modifikator: PersonId
    ))
  }
}

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
  anzahlAbonnentenMax: Option[Int]
) extends JSONSerializable

case class DepotSummary(
  id: DepotId,
  name: String
) extends JSONSerializable

case class TourId(id: Long) extends BaseId

case class Tour(
  id: TourId,
  name: String,
  beschreibung: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[TourId] with Vertriebskanal

case class TourModify(
  name: String,
  beschreibung: Option[String]
) extends JSONSerializable
