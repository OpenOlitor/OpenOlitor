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

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.scalax.Tuple28

sealed trait Vertriebskanal {
  val name: String
}

sealed trait VertriebskanalDetail extends Vertriebskanal with JSONSerializable {
  val beschreibung: Option[String]
}

case class DepotId(id: Long) extends BaseId

trait IDepot extends BaseEntity[DepotId] {
  val id: DepotId
  val name: String
  val kurzzeichen: String
  val apName: Option[String]
  val apVorname: Option[String]
  val apTelefon: Option[String]
  val apEmail: Option[String]
  val vName: Option[String]
  val vVorname: Option[String]
  val vTelefon: Option[String]
  val vEmail: Option[String]
  val strasse: Option[String]
  val hausNummer: Option[String]
  val plz: String
  val ort: String
  val aktiv: Boolean
  val oeffnungszeiten: Option[String]
  val farbCode: Option[String]
  val iban: Option[String]
  val bank: Option[String]
  val beschreibung: Option[String]
  val anzahlAbonnentenMax: Option[Int]
  //Zusatzinformationen
  val anzahlAbonnenten: Int
  val anzahlAbonnentenAktiv: Int
}

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
  anzahlAbonnentenAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends IDepot with Vertriebskanal

trait IDepotReport extends IDepot {
  lazy val strasseUndNummer = strasse.map(_ + hausNummer.map(" " + _).getOrElse(""))
  lazy val plzOrt = plz + " " + ort

  lazy val adresszeilen = Seq(
    Some(name),
    strasseUndNummer,
    Some(plzOrt)
  ).flatten.padTo(6, "")
}

case class DepotReport(
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
  anzahlAbonnentenAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[DepotId] with IDepotReport with JSONSerializable

case class DepotDetailReport(
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
  anzahlAbonnentenAktiv: Int,
  abos: Seq[DepotlieferungAboReport],
  projekt: ProjektReport,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[DepotId] with IDepotReport with JSONSerializable

object Depot {
  def unapply(d: Depot) = {
    Some(Tuple28(
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
      d.anzahlAbonnentenAktiv: Int,
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
  name: String,
  kurzzeichen: String
) extends JSONSerializable

case class TourId(id: Long) extends BaseId

case class Tour(
  id: TourId,
  name: String,
  beschreibung: Option[String],
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  anzahlAbonnentenAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[TourId] with Vertriebskanal

case class TourDetail(
  id: TourId,
  name: String,
  beschreibung: Option[String],
  tourlieferungen: Seq[TourlieferungDetail],
  anzahlAbonnenten: Int,
  anzahlAbonnentenAktiv: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class TourCreate(
  name: String,
  beschreibung: Option[String]
) extends JSONSerializable

case class TourModify(
  name: String,
  beschreibung: Option[String],
  tourlieferungen: Seq[Tourlieferung]
) extends JSONSerializable
