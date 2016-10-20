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
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.scalax.Tuple24
import scala.collection.immutable.TreeMap
import ch.openolitor.core.scalax.Tuple25

case class KundeId(id: Long) extends BaseId

trait IKunde extends BaseEntity[KundeId] {
  val id: KundeId
  val bezeichnung: String
  val strasse: String
  val hausNummer: Option[String]
  val adressZusatz: Option[String]
  val plz: String
  val ort: String
  val bemerkungen: Option[String]
  val abweichendeLieferadresse: Boolean
  val bezeichnungLieferung: Option[String]
  val strasseLieferung: Option[String]
  val hausNummerLieferung: Option[String]
  val adressZusatzLieferung: Option[String]
  val plzLieferung: Option[String]
  val ortLieferung: Option[String]
  val zusatzinfoLieferung: Option[String]
  val typen: Set[KundentypId]
  //Zusatzinformationen
  val anzahlAbos: Int
  val anzahlAbosAktiv: Int
  val anzahlPendenzen: Int
  val anzahlPersonen: Int
}

case class Kunde(
  id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KundeId]

trait IKundeReport extends IKunde {
  lazy val strasseUndNummer: String = strasse + hausNummer.map(" " + _).getOrElse("")
  lazy val plzOrt: String = plz + " " + ort

  lazy val strasseUndNummerLieferung = strasseLieferung.map(_ + hausNummerLieferung.map(" " + _).getOrElse(""))
  lazy val plzOrtLieferung = plzLieferung.map(_ + ortLieferung.map(" " + _).getOrElse(""))

  lazy val adresszeilen = Seq(
    Some(bezeichnung),
    adressZusatz,
    Some(strasseUndNummer),
    Some(plzOrt)
  ).flatten.padTo(6, "")

  lazy val lieferAdresszeilen = abweichendeLieferadresse match {
    case true =>
      Seq(
        Some(bezeichnungLieferung.getOrElse(bezeichnung)),
        adressZusatzLieferung,
        strasseUndNummerLieferung,
        plzOrtLieferung
      ).flatten.padTo(6, "")
    case false => adresszeilen
  }
}

case class KundeReport(
  id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KundeId] with IKundeReport

case class KundeDetailReport(
  id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int,
  //Report infos
  personen: Seq[PersonDetail],
  abos: Seq[Abo],
  pendenzen: Seq[Pendenz],
  projekt: ProjektReport,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KundeId] with IKundeReport

object Kunde {
  def unapply(k: Kunde) = Some(Tuple25(
    k.id: KundeId,
    k.bezeichnung: String,
    k.strasse: String,
    k.hausNummer: Option[String],
    k.adressZusatz: Option[String],
    k.plz: String,
    k.ort: String,
    k.bemerkungen: Option[String],
    k.abweichendeLieferadresse: Boolean,
    k.bezeichnungLieferung: Option[String],
    k.strasseLieferung: Option[String],
    k.hausNummerLieferung: Option[String],
    k.adressZusatzLieferung: Option[String],
    k.plzLieferung: Option[String],
    k.ortLieferung: Option[String],
    k.zusatzinfoLieferung: Option[String],
    k.typen: Set[KundentypId],
    //Zusatzinformationen
    k.anzahlAbos: Int,
    k.anzahlAbosAktiv: Int,
    k.anzahlPendenzen: Int,
    k.anzahlPersonen: Int,
    //modification flags
    k.erstelldat: DateTime,
    k.ersteller: PersonId,
    k.modifidat: DateTime,
    k.modifikator: PersonId
  ))
}

case class KundeUebersicht(
  id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  ansprechpersonen: Seq[PersonSummary],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KundeDetail(
  id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlAbosAktiv: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int,
  abos: Seq[Abo],
  pendenzen: Seq[Pendenz],
  ansprechpersonen: Seq[PersonDetail],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KundeModify(
  bezeichnung: Option[String],
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  abweichendeLieferadresse: Boolean,
  bezeichnungLieferung: Option[String],
  strasseLieferung: Option[String],
  hausNummerLieferung: Option[String],
  adressZusatzLieferung: Option[String],
  plzLieferung: Option[String],
  ortLieferung: Option[String],
  zusatzinfoLieferung: Option[String],
  typen: Set[KundentypId],
  pendenzen: Seq[PendenzModify],
  ansprechpersonen: Seq[PersonModify]
) extends JSONSerializable

sealed trait Anrede
case object Herr extends Anrede
case object Frau extends Anrede

object Anrede {
  val AlleAnreden = Vector(Herr, Frau)

  def apply(value: String): Option[Anrede] = {
    AlleAnreden.find(_.toString == value)
  }
}

sealed trait Rolle
case object AdministratorZugang extends Rolle
case object KundenZugang extends Rolle

object Rolle {
  val AlleRollen = Vector(AdministratorZugang, KundenZugang)

  def apply(value: String): Option[Rolle] = {
    AlleRollen.find(_.toString == value)
  }
}

case class Person(
  id: PersonId,
  kundeId: KundeId,
  anrede: Option[Anrede],
  name: String,
  vorname: String,
  email: Option[String],
  emailAlternative: Option[String],
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  bemerkungen: Option[String],
  sort: Int,
  // security data
  loginAktiv: Boolean,
  passwort: Option[Array[Char]],
  letzteAnmeldung: Option[DateTime],
  passwortWechselErforderlich: Boolean,
  rolle: Option[Rolle],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[PersonId]

case class PersonDetail(
  id: PersonId,
  kundeId: KundeId,
  anrede: Option[Anrede],
  name: String,
  vorname: String,
  email: Option[String],
  emailAlternative: Option[String],
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  bemerkungen: Option[String],
  sort: Int,
  // security data
  loginAktiv: Boolean,
  letzteAnmeldung: Option[DateTime],
  passwortWechselErforderlich: Boolean,
  rolle: Option[Rolle],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[PersonId]

case class PersonSummary(
  anrede: Option[Anrede],
  name: String,
  vorname: String,
  email: Option[String],
  emailAlternative: Option[String],
  letzteAnmeldung: Option[DateTime]
) extends JSONSerializable

case class PersonUebersicht(
  id: PersonId,
  kundeId: KundeId,
  anrede: Option[Anrede],
  name: String,
  vorname: String,
  email: Option[String],
  emailAlternative: Option[String],
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  bemerkungen: Option[String],
  loginAktiv: Boolean,
  letzteAnmeldung: Option[DateTime],
  rolle: Option[Rolle],
  // kundendaten
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  kundentypen: Set[KundentypId],
  kundenBemerkungen: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends JSONSerializable

case class KundeSummary(id: KundeId, kunde: String) extends Product

case class PersonModify(
    id: Option[PersonId],
    anrede: Option[Anrede],
    name: String,
    vorname: String,
    email: Option[String],
    emailAlternative: Option[String],
    telefonMobil: Option[String],
    telefonFestnetz: Option[String],
    bemerkungen: Option[String]
) extends JSONSerializable {
  def fullName = vorname + ' ' + name
}

case class PersonCreate(
    kundeId: KundeId,
    anrede: Option[Anrede],
    name: String,
    vorname: String,
    email: Option[String],
    emailAlternative: Option[String],
    telefonMobil: Option[String],
    telefonFestnetz: Option[String],
    bemerkungen: Option[String],
    sort: Int
) extends JSONSerializable {
  def fullName = vorname + ' ' + name
}

sealed trait PendenzStatus
case object Ausstehend extends PendenzStatus
case object Erledigt extends PendenzStatus
case object NichtErledigt extends PendenzStatus

object PendenzStatus {
  def apply(value: String): PendenzStatus = {
    Vector(Ausstehend, Erledigt, NichtErledigt).find(_.toString == value).getOrElse(Ausstehend)
  }
}

case class PendenzId(id: Long) extends BaseId

case class Pendenz(
  id: PendenzId,
  kundeId: KundeId,
  kundeBezeichnung: String,
  datum: DateTime,
  bemerkung: Option[String],
  status: PendenzStatus,
  generiert: Boolean,
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[PendenzId]

case class PendenzModify(
  id: Option[PendenzId],
  datum: DateTime,
  bemerkung: Option[String],
  status: PendenzStatus
) extends JSONSerializable

case class PendenzCreate(
  kundeId: KundeId,
  datum: DateTime,
  bemerkung: Option[String],
  status: PendenzStatus,
  generiert: Boolean
) extends JSONSerializable

case class EinladungId(id: Long) extends BaseId

case class Einladung(
  id: EinladungId,
  personId: PersonId,
  uid: String,
  expires: DateTime,
  datumVersendet: Option[DateTime],
  // modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
)
    extends BaseEntity[EinladungId]

case class EinladungCreate(
  id: EinladungId,
  personId: PersonId,
  uid: String,
  expires: DateTime,
  datumVersendet: Option[DateTime]
) extends JSONSerializable
