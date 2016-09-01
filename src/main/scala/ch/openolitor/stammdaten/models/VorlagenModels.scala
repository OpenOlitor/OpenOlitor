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
import ch.openolitor.core.models.VorlageTyp
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable

case class ProjektVorlageId(id: Long) extends BaseId

case class ProjektVorlage(
  id: ProjektVorlageId,
  typ: VorlageTyp,
  name: String,
  beschreibung: Option[String],
  fileStoreId: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[ProjektVorlageId] with JSONSerializable

case class ProjektVorlageModify(
  name: String,
  beschreibung: Option[String]
) extends JSONSerializable

case class ProjektVorlageUpload(
  fileStoreId: String
) extends JSONSerializable

case class ProjektVorlageCreate(
  typ: VorlageTyp,
  name: String,
  beschreibung: Option[String]
) extends JSONSerializable