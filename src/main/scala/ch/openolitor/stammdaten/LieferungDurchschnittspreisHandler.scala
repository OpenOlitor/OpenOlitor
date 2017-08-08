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
package ch.openolitor.stammdaten

import ch.openolitor.core.Macros._
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc._
import ch.openolitor.util.IdUtil
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.repositories.EventPublisher

trait LieferungDurchschnittspreisHandler {
  def calcDurchschnittspreis(durchschnittspreis: BigDecimal, anzahlLieferungen: Int, neuerPreis: BigDecimal): BigDecimal =
    if (anzahlLieferungen == 0) {
      0
    } else {
      ((durchschnittspreis * (anzahlLieferungen - 1)) + neuerPreis) / anzahlLieferungen
    }
}
