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

trait LieferungHandler extends StammdatenDBMappings {
  this: StammdatenWriteRepositoryComponent =>

  def calcDurchschnittspreis(durchschnittspreis: BigDecimal, anzahlLieferungen: Int, neuerPreis: BigDecimal): BigDecimal =
    if (anzahlLieferungen == 0) {
      0
    } else {
      ((durchschnittspreis * (anzahlLieferungen - 1)) + neuerPreis) / anzahlLieferungen
    }

  def recreateLieferpositionen(meta: EventMetadata, lieferungId: LieferungId, positionen: LieferpositionenModify)(implicit personId: PersonId, session: DBSession) = {
    stammdatenWriteRepository.deleteLieferpositionen(lieferungId)

    stammdatenWriteRepository.getById(lieferungMapping, lieferungId) map { lieferung =>
      positionen.preisTotal match {
        case Some(preis) =>
          val copy = lieferung.copy(preisTotal = preis, modifidat = meta.timestamp, modifikator = personId)
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](copy)
        case _ =>
      }

      //save Lieferpositionen
      positionen.lieferpositionen map { create =>
        val lpId = LieferpositionId(IdUtil.positiveRandomId)
        val newObj = copyTo[LieferpositionModify, Lieferposition](
          create,
          "id" -> lpId,
          "lieferungId" -> lieferungId,
          "erstelldat" -> meta.timestamp,
          "ersteller" -> meta.originator,
          "modifidat" -> meta.timestamp,
          "modifikator" -> meta.originator
        )
        stammdatenWriteRepository.insertEntity[Lieferposition, LieferpositionId](newObj)
      }
    }
  }
}
