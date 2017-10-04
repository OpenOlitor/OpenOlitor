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

import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.models.PersonId
import scalikejdbc._
import ch.openolitor.core.repositories.EventPublisher

trait AboAktivChangeHandler extends StammdatenDBMappings {
  this: StammdatenUpdateRepositoryComponent =>
  def handleAboAktivChange(abo: Abo, change: Int)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId) = {
    abo match {
      case d: DepotlieferungAbo =>
        stammdatenUpdateRepository.modifyEntity[Depot, DepotId](d.depotId) { depot =>
          Map(depotMapping.column.anzahlAbonnentenAktiv -> (depot.anzahlAbonnentenAktiv + change))
        }
      case h: HeimlieferungAbo =>
        stammdatenUpdateRepository.modifyEntity[Tour, TourId](h.tourId) { tour =>
          Map(tourMapping.column.anzahlAbonnentenAktiv -> (tour.anzahlAbonnentenAktiv + change))
        }
      case _ =>
      // nothing to change
    }

    stammdatenUpdateRepository.modifyEntity[Abotyp, AbotypId](abo.abotypId) { abotyp =>
      Map(abotypMapping.column.anzahlAbonnentenAktiv -> (abotyp.anzahlAbonnentenAktiv + change))
    }

    stammdatenUpdateRepository.modifyEntity[Kunde, KundeId](abo.kundeId) { kunde =>
      Map(kundeMapping.column.anzahlAbosAktiv -> (kunde.anzahlAbosAktiv + change))
    }

    stammdatenUpdateRepository.modifyEntity[Vertrieb, VertriebId](abo.vertriebId) { vertrieb =>
      Map(vertriebMapping.column.anzahlAbosAktiv -> (vertrieb.anzahlAbosAktiv + change))
    }

    stammdatenUpdateRepository.modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      Map(depotlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + change))
    }

    stammdatenUpdateRepository.modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      Map(heimlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + change))
    }

    stammdatenUpdateRepository.modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId) { vertriebsart =>
      Map(postlieferungMapping.column.anzahlAbosAktiv -> (vertriebsart.anzahlAbosAktiv + change))
    }
  }
}
