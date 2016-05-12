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
package ch.openolitor.buchhaltung

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.Boot
import ch.openolitor.core.repositories.SqlBinder
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.stammdaten.models.AboId
import ch.openolitor.stammdaten.models.{ DepotlieferungAbo, HeimlieferungAbo, PostlieferungAbo }

object BuchhaltungDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultBuchhaltungDBEventEntityListener], sysConfig, system)
}

class DefaultBuchhaltungDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends BuchhaltungDBEventEntityListener(sysConfig) with DefaultBuchhaltungWriteRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class BuchhaltungDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with BuchhaltungDBMappings with AsyncConnectionPoolContextAware {
  this: BuchhaltungWriteRepositoryComponent =>
  import BuchhaltungDBEventEntityListener._

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityCreated(userId, entity: Rechnung) => handleRechnungCreated(entity)(userId)
    case e @ EntityDeleted(userId, entity: Rechnung) => handleRechnungDeleted(entity)(userId)
    case e @ EntityModified(userId, entity: Rechnung, orig: Rechnung) => handleRechnungModified(entity, orig)(userId)
    case e @ EntityModified(userId, entity: ZahlungsEingang, orig: ZahlungsEingang) => handleZahlungsEingangModified(entity, orig)(userId)

    case x => //log.debug(s"receive unused event $x")
  }

  def handleRechnungModified(rechnung: Rechnung, orig: Rechnung)(implicit userId: UserId) = {
  }

  def handleRechnungDeleted(rechnung: Rechnung)(implicit userId: UserId) = {
  }

  def handleRechnungCreated(rechnung: Rechnung)(implicit userId: UserId) = {
  }

  def handleZahlungsEingangModified(entity: ZahlungsEingang, orig: ZahlungsEingang)(implicit userId: UserId) = {
    DB autoCommit { implicit session =>
      if (!orig.erledigt && entity.erledigt) {
        modifyEntity[ZahlungsImport, ZahlungsImportId](entity.zahlungsImportId, { zahlungsImport =>
          zahlungsImport.copy(anzahlZahlungsEingaengeErledigt = zahlungsImport.anzahlZahlungsEingaengeErledigt + 1)
        })
      } else if (orig.erledigt && !entity.erledigt) {
        modifyEntity[ZahlungsImport, ZahlungsImportId](entity.zahlungsImportId, { zahlungsImport =>
          zahlungsImport.copy(anzahlZahlungsEingaengeErledigt = zahlungsImport.anzahlZahlungsEingaengeErledigt - 1)
        })
      }
    }
  }

  def modifyEntity[E <: BaseEntity[I], I <: BaseId](
    id: I, mod: E => E
  )(implicit syntax: BaseEntitySQLSyntaxSupport[E], binder: SqlBinder[I], userId: UserId) = {
    DB autoCommit { implicit session =>
      buchhaltungWriteRepository.getById(syntax, id) map { result =>
        val copy = mod(result)
        buchhaltungWriteRepository.updateEntity[E, I](copy)
      }
    }
  }
}
