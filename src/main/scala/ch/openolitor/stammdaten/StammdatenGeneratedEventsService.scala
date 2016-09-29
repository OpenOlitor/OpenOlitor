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

import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.StammdatenCommandHandler._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ch.openolitor.util.ConfigUtil._
import scalikejdbc.DBSession
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.repositories.SqlBinder

object StammdatenGeneratedEventsService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenGeneratedEventsService = new DefaultStammdatenGeneratedEventsService(sysConfig, system)
}

class DefaultStammdatenGeneratedEventsService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends StammdatenGeneratedEventsService(sysConfig) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten generierter Events (z.B. datumsabhängige Events)
 */
class StammdatenGeneratedEventsService(override val sysConfig: SystemConfig) extends EventService[PersistentGeneratedEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with StammdatenDBMappings with StammdatenEventStoreSerializer {
  self: StammdatenWriteRepositoryComponent =>

  val handle: Handle = {
    case AboAktiviertEvent(meta, id: AboId) =>
      handleAboAktiviert(meta, id)
    case AboDeaktiviertEvent(meta, id: AboId) =>
      handleAboDeaktiviert(meta, id)
    case _ =>
    // nothing to handle
  }

  def handleAboAktiviert(meta: EventMetadata, id: AboId)(implicit personId: PersonId = meta.originator) = {
    handleChange(id, 1)
  }

  def handleAboDeaktiviert(meta: EventMetadata, id: AboId)(implicit personId: PersonId = meta.originator) = {
    handleChange(id, -1)
  }

  private def handleChange(id: AboId, change: Int)(implicit personId: PersonId) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getAboDetail(id) map { abo =>

        abo match {
          case d: DepotlieferungAboDetail =>
            modifyEntity[Depot, DepotId](d.depotId, { depot =>
              depot.copy(anzahlAbonnentenAktiv = depot.anzahlAbonnentenAktiv + change)
            })
          case h: HeimlieferungAboDetail =>
            modifyEntity[Tour, TourId](h.tourId, { tour =>
              tour.copy(anzahlAbonnentenAktiv = tour.anzahlAbonnentenAktiv + change)
            })
          case _ =>
          // nothing to change
        }

        modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
          abotyp.copy(anzahlAbonnentenAktiv = abotyp.anzahlAbonnentenAktiv + change)
        })
        modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
          kunde.copy(anzahlAbosAktiv = kunde.anzahlAbosAktiv + change)
        })
        modifyEntity[Vertrieb, VertriebId](abo.vertriebId, { vertrieb =>
          vertrieb.copy(anzahlAbosAktiv = vertrieb.anzahlAbosAktiv + change)
        })
        modifyEntity[Depotlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
          vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
        })
        modifyEntity[Heimlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
          vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
        })
        modifyEntity[Postlieferung, VertriebsartId](abo.vertriebsartId, { vertriebsart =>
          vertriebsart.copy(anzahlAbosAktiv = vertriebsart.anzahlAbosAktiv + change)
        })
      }
    }
  }

  // TODO refactor this further
  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E
  )(implicit session: DBSession, syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], personId: PersonId): Option[E] = {
    modifyEntityWithRepository(stammdatenWriteRepository)(id, mod)
  }
}
