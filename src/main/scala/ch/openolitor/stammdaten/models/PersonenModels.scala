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

sealed trait Personentyp extends Product
case object Vereinsmitglied extends Personentyp
case object Goenner extends Personentyp
case object Genossenschafterin extends Personentyp

object Personentyp {
  def apply(value: String): Option[Personentyp] = {
    Vector(Vereinsmitglied, Goenner, Genossenschafterin).find(_.toString == value)
  }
}

case class PersonId(id: UUID) extends BaseId
case class Person(id: PersonId,
                  name: String,
                  vorname: String,
                  strasse: String,
                  hausNummer: Option[String],
                  adressZusatz: Option[String],
                  plz: String,
                  ort: String,
                  email: String,
                  emailAlternative: Option[String],
                  telefon: Option[String],
                  telefonAlternative: Option[String],
                  bemerkungen: Option[String],
                  typen: Set[Personentyp]) extends BaseEntity[PersonId]

case class PersonSummary(id: PersonId, name: String, vorname: String) extends Product

case class PersonModify(
  name: String,
  vorname: String,
  strasse: String,
  hausNummer: Option[String],
  adressZusatz: Option[String],
  plz: String,
  ort: String,
  email: String,
  emailAlternative: Option[String],
  telefon: Option[String],
  telefonAlternative: Option[String],
  bemerkungen: Option[String],
  typen: Set[Personentyp]) extends Product
