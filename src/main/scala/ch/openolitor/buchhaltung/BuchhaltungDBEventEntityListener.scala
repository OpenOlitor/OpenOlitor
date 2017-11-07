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
import spray.json._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db._
import scalikejdbc._
import ch.openolitor.core.SystemConfig
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungUpdateRepositoryComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungUpdateRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._

object BuchhaltungDBEventEntityListener extends DefaultJsonProtocol {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultBuchhaltungDBEventEntityListener], sysConfig, system)
}

class DefaultBuchhaltungDBEventEntityListener(sysConfig: SystemConfig, override val system: ActorSystem) extends BuchhaltungDBEventEntityListener(sysConfig) with DefaultBuchhaltungUpdateRepositoryComponent

/**
 * Listen on DBEvents and adjust calculated fields within this module
 */
class BuchhaltungDBEventEntityListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging with BuchhaltungDBMappings with AsyncConnectionPoolContextAware {
  this: BuchhaltungUpdateRepositoryComponent =>

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    case e @ EntityCreated(personId, entity: Rechnung) => handleRechnungCreated(entity)(personId)
    case e @ EntityDeleted(personId, entity: Rechnung) => handleRechnungDeleted(entity)(personId)
    case e @ EntityModified(personId, entity: Rechnung, orig: Rechnung) => handleRechnungModified(entity, orig)(personId)
    case e @ EntityModified(personId, entity: ZahlungsEingang, orig: ZahlungsEingang) => handleZahlungsEingangModified(entity, orig)(personId)

    case x =>
  }

  def handleRechnungModified(rechnung: Rechnung, orig: Rechnung)(implicit personId: PersonId) = {
  }

  def handleRechnungDeleted(rechnung: Rechnung)(implicit personId: PersonId) = {
  }

  def handleRechnungCreated(rechnung: Rechnung)(implicit personId: PersonId) = {
  }

  def handleZahlungsEingangModified(entity: ZahlungsEingang, orig: ZahlungsEingang)(implicit userId: PersonId) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      if (!orig.erledigt && entity.erledigt) {
        buchhaltungUpdateRepository.modifyEntity[ZahlungsImport, ZahlungsImportId](entity.zahlungsImportId) { zahlungsImport =>
          Map(zahlungsImportMapping.column.anzahlZahlungsEingaengeErledigt -> (zahlungsImport.anzahlZahlungsEingaengeErledigt + 1))
        }
      } else if (orig.erledigt && !entity.erledigt) {
        buchhaltungUpdateRepository.modifyEntity[ZahlungsImport, ZahlungsImportId](entity.zahlungsImportId) { zahlungsImport =>
          Map(zahlungsImportMapping.column.anzahlZahlungsEingaengeErledigt -> (zahlungsImport.anzahlZahlungsEingaengeErledigt - 1))
        }
      }
    }
  }
}
