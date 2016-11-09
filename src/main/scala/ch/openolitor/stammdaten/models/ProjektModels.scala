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
import org.joda.time.LocalDate
import ch.openolitor.core.JSONSerializable
import scala.collection.immutable.TreeMap
import java.util.Locale
import ch.openolitor.core.JSONSerializable

case class ProjektId(id: Long) extends BaseId

case class Geschaeftsjahr(monat: Int, tag: Int) {

  /**
   * Errechnet den Start des Geschäftsjahres aufgrund eines Datums
   */
  def start(date: LocalDate = LocalDate.now): LocalDate = {
    val geschaftsjahrInJahr = new LocalDate(date.year.get, monat, tag)
    date match {
      case d if d.isBefore(geschaftsjahrInJahr) =>
        //Wir sind noch im "alten" Geschäftsjahr
        geschaftsjahrInJahr.minusYears(1)
      case d =>
        //Wir sind bereits im neuen Geschäftsjahr
        geschaftsjahrInJahr
    }
  }

  /**
   * Errechnet der Key für ein Geschäftsjahr aufgrund eines Datum. Der Key des Geschäftsjahres leitet sich aus dem Startdatum
   * des Geschäftsjahres ab. Wird der Start des Geschäftsjahres auf den Start des Kalenderjahres gesetzt, wird das Kalenderjahr als
   * key benutzt, ansonsten setzt sich der Key aus Monat/Jahr zusammen
   */
  def key(date: LocalDate = LocalDate.now): String = {
    val startDate = start(date)
    if (monat == 1 && tag == 1) {
      startDate.year.getAsText
    } else {
      s"${startDate.getMonthOfYear}/${startDate.getYear}"
    }
  }
}

case class Projekt(
    id: ProjektId,
    bezeichnung: String,
    strasse: Option[String],
    hausNummer: Option[String],
    adressZusatz: Option[String],
    plz: Option[String],
    ort: Option[String],
    preiseSichtbar: Boolean,
    preiseEditierbar: Boolean,
    emailErforderlich: Boolean,
    waehrung: Waehrung,
    geschaeftsjahrMonat: Int,
    geschaeftsjahrTag: Int,
    twoFactorAuthentication: Map[Rolle, Boolean],
    sprache: Locale,
    //modification flags
    erstelldat: DateTime,
    ersteller: PersonId,
    modifidat: DateTime,
    modifikator: PersonId
) extends BaseEntity[ProjektId] {
  lazy val geschaftsjahr = Geschaeftsjahr(geschaeftsjahrMonat, geschaeftsjahrTag)
}

case class ProjektPublik(
  id: ProjektId,
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int
) extends JSONSerializable

case class ProjektReport(
    id: ProjektId,
    bezeichnung: String,
    strasse: Option[String],
    hausNummer: Option[String],
    adressZusatz: Option[String],
    plz: Option[String],
    ort: Option[String],
    preiseSichtbar: Boolean,
    preiseEditierbar: Boolean,
    emailErforderlich: Boolean,
    waehrung: Waehrung,
    geschaeftsjahrMonat: Int,
    geschaeftsjahrTag: Int,
    twoFactorAuthentication: Map[Rolle, Boolean],
    sprache: Locale,
    //modification flags
    erstelldat: DateTime,
    ersteller: PersonId,
    modifidat: DateTime,
    modifikator: PersonId
) extends BaseEntity[ProjektId] {
  lazy val geschaftsjahr = Geschaeftsjahr(geschaeftsjahrMonat, geschaeftsjahrTag)
  lazy val strasseUndNummer = strasse.map(_ + hausNummer.map(" " + _).getOrElse(""))
  lazy val plzOrt = plz.map(_ + ort.map(" " + _).getOrElse(""))

  lazy val adresszeilen = Seq(
    Some(bezeichnung),
    adressZusatz,
    strasseUndNummer,
    plzOrt
  ).flatten.padTo(6, "")
}

case class ProjektModify(
  bezeichnung: String,
  strasse: Option[String],
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: Option[String],
  ort: Option[String],
  preiseSichtbar: Boolean,
  preiseEditierbar: Boolean,
  emailErforderlich: Boolean,
  waehrung: Waehrung,
  geschaeftsjahrMonat: Int,
  geschaeftsjahrTag: Int,
  twoFactorAuthentication: Map[Rolle, Boolean],
  sprache: Locale
) extends JSONSerializable

case class KundentypId(id: String) extends BaseStringId

case class CustomKundentypId(id: Long) extends BaseId

trait Kundentyp {
  val kundentyp: KundentypId
  val beschreibung: Option[String] = None
  def system: Boolean
}

case class CustomKundentyp(
    id: CustomKundentypId,
    override val kundentyp: KundentypId,
    override val beschreibung: Option[String],
    anzahlVerknuepfungen: Int,
    //modification flags
    erstelldat: DateTime,
    ersteller: PersonId,
    modifidat: DateTime,
    modifikator: PersonId
) extends BaseEntity[CustomKundentypId] with Kundentyp {
  override def system = false
}

case class CustomKundentypModify(beschreibung: Option[String]) extends JSONSerializable
case class CustomKundentypCreate(kundentyp: KundentypId, beschreibung: Option[String]) extends JSONSerializable

sealed trait SystemKundentyp extends Kundentyp with Product {
  override def system = true
}

object SystemKundentyp {
  val ALL = Vector(Vereinsmitglied, Goenner, Genossenschafterin)

  def parse(value: String): Option[SystemKundentyp] = {
    ALL find (_.toString == value)
  }
}

case object Vereinsmitglied extends SystemKundentyp {
  override val kundentyp = KundentypId("Vereinsmitglied")
}

case object Goenner extends SystemKundentyp {
  override val kundentyp = KundentypId("Goenner")
}

case object Genossenschafterin extends SystemKundentyp {
  override val kundentyp = KundentypId("Genossenschafterin")
}
