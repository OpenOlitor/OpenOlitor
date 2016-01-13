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

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.SqlBinder
import scala.concurrent.ExecutionContext.Implicits.global;

object StammdatenDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenDBEventEntityListener], sysConfig, system)
}

class DefaultStammdatenDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenDBEventEntityListener(sysConfig) with DefaultStammdatenRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class StammdatenDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with StammdatenDBMappings with AsyncConnectionPoolContextAware {
  this: StammdatenRepositoryComponent =>
  import StammdatenDBEventEntityListener._

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityCreated(userId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboCreated(entity)(userId)
      handleAboCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: DepotlieferungAbo) =>
      handleDepotlieferungAboDeleted(entity)(userId)
      handleAboDeleted(entity)(userId)
    case e @ EntityCreated(userId, entity: Abo) => handleAboCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Abo) => handleAboDeleted(entity)(userId)
    case e @ EntityModified(userId, entity: Kunde) => handleKundeModified(entity)(userId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleDepotlieferungAboCreated(abo: DepotlieferungAbo)(implicit userId: UserId) = {
    modifyEntity[Depot, DepotId](abo.depotId, { depot =>
      log.debug(s"Add abonnent to depot:${depot.id}")
      depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten + 1)
    })
  }

  def handleDepotlieferungAboDeleted(abo: DepotlieferungAbo)(implicit userId: UserId) = {
    modifyEntity[Depot, DepotId](abo.depotId, { depot =>
      log.debug(s"Remove abonnent from depot:${depot.id}")
      depot.copy(anzahlAbonnenten = depot.anzahlAbonnenten - 1)
    })
  }

  def handleAboCreated(abo: Abo)(implicit userId: UserId) = {
    modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
      log.debug(s"Add abonnent to abotyp:${abotyp.id}")
      abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten + 1)
    })
    modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
      log.debug(s"Add abonnent to kunde:${kunde.id}")
      kunde.copy(anzahlAbos = kunde.anzahlAbos + 1)
    })
  }

  def handleAboDeleted(abo: Abo)(implicit userId: UserId) = {
    modifyEntity[Abotyp, AbotypId](abo.abotypId, { abotyp =>
      log.debug(s"Remove abonnent from abotyp:${abotyp.id}")
      abotyp.copy(anzahlAbonnenten = abotyp.anzahlAbonnenten - 1)
    })
    modifyEntity[Kunde, KundeId](abo.kundeId, { kunde =>
      log.debug(s"Remove abonnent from kunde:${kunde.id}")
      kunde.copy(anzahlAbos = kunde.anzahlAbos - 1)
    })
  }

  def handleKundeModified(kunde: Kunde)(implicit userId: UserId) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(kundeMapping, kunde.id) map { orig =>
        //compare typen
        //find removed typen
        val removed = orig.typen -- kunde.typen

        //tag typen which where added
        val added = kunde.typen -- orig.typen

        readRepository.getKundentypen map { kundetypen =>
          removed.map { kundetypId =>
            kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
              case customKundetyp: CustomKundentyp =>
                val copy = customKundetyp.copy(anzahlVerknuepfungen = customKundetyp.anzahlVerknuepfungen - 1)
                writeRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
            }
          }

          added.map { kundetypId =>
            kundetypen.filter(kt => kt.kundentyp == kundetypId && !kt.system).headOption.map {
              case customKundetyp: CustomKundentyp =>
                val copy = customKundetyp.copy(anzahlVerknuepfungen = customKundetyp.anzahlVerknuepfungen + 1)
                writeRepository.updateEntity[CustomKundentyp, CustomKundentypId](copy)
            }
          }
        }
      }
    }
  }

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E)(implicit syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], userId: UserId) = {
    DB autoCommit { implicit session =>
      writeRepository.getById(syntax, id) map { result =>
        val copy = mod(result)
        writeRepository.updateEntity[E, I](copy)
      }
    }
  }
}