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

case class KundeId(id: UUID) extends BaseId

case class Kunde(id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int) extends BaseEntity[KundeId]

case class KundeDetail(id: KundeId,
  bezeichnung: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  typen: Set[KundentypId],
  //Zusatzinformationen
  anzahlAbos: Int,
  anzahlPendenzen: Int,
  anzahlPersonen: Int,
  abos: Seq[Abo],
  pendenzen: Seq[Pendenz],
  ansprechpersonen: Seq[Person])

case class KundeModify(
  bezeichnung: Option[String],
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  bemerkungen: Option[String],
  typen: Set[KundentypId],
  pendenzen: Seq[PendenzModify],
  ansprechpersonen: Seq[PersonModify]) extends Product

case class PersonId(id: UUID) extends BaseId
case class Person(id: PersonId,
  kundeId: KundeId,
  name: String,
  vorname: String,
  email: String,
  emailAlternative: Option[String],
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  bemerkungen: Option[String],
  sort: Int) extends BaseEntity[PersonId]

case class KundeSummary(id: KundeId, kunde: String) extends Product

case class PersonModify(
  id: Option[PersonId],
  name: String,
  vorname: String,
  email: String,
  emailAlternative: Option[String],
  telefonMobil: Option[String],
  telefonFestnetz: Option[String],
  bemerkungen: Option[String]) extends Product {
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

case class PendenzId(id: UUID) extends BaseId

case class Pendenz(id: PendenzId,
    kundeId: KundeId,
    kundeBezeichnung: String,
    datum: DateTime,
    bemerkung: Option[String],
    status: PendenzStatus) extends BaseEntity[PendenzId]

case class PendenzModify(id: Option[PendenzId],
    datum: DateTime,
    bemerkung: Option[String],
    status: PendenzStatus) extends Product
