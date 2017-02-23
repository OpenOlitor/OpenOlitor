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
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.mailservice.MailService._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import ch.openolitor.core.models.PersonId
import scalikejdbc._

object StammdatenMailListener {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[DefaultStammdatenMailListener], sysConfig, system)
}

class DefaultStammdatenMailListener(sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenMailListener(sysConfig) with DefaultStammdatenWriteRepositoryComponent

/**
 * Listens to succesful sent mails
 */
class StammdatenMailListener(override val sysConfig: SystemConfig) extends Actor with ActorLogging
    with StammdatenDBMappings
    with ConnectionPoolContextAware {
  this: StammdatenWriteRepositoryComponent =>
  import StammdatenMailListener._

  override def preStart() {
    super.preStart()
    context.system.eventStream.subscribe(self, classOf[MailSentEvent])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[MailSentEvent])
    super.postStop()
  }

  def receive: Receive = {
    case MailSentEvent(meta, uid, Some(id: SammelbestellungId)) => handleBestellungMailSent(meta, id)
    case MailSentEvent(meta, uid, Some(id: EinladungId)) => handleEinladungMailSent(meta, id)
    case x => log.debug(s"Received unknown mailsentevent:$x")
  }

  protected def handleBestellungMailSent(meta: EventMetadata, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    log.debug(s"handleBestellungMailSent:$id")
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
        val copy = sammelbestellung.copy(datumVersendet = Some(meta.timestamp))
        stammdatenWriteRepository.updateEntity[Sammelbestellung, SammelbestellungId](copy)
      }
    }
  }

  protected def handleEinladungMailSent(meta: EventMetadata, id: EinladungId)(implicit personId: PersonId = meta.originator) = {
    log.debug(s"handleEinladungMailSent:$id")
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(einladungMapping, id) map { einladung =>
        val copy = einladung.copy(datumVersendet = Some(meta.timestamp))
        stammdatenWriteRepository.updateEntity[Einladung, EinladungId](copy)
      }
    }
  }
}