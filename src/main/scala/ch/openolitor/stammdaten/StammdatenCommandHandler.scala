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

import ch.openolitor.core.domain.CommandHandler
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.domain.PersistentEvent
import ch.openolitor.core.models.UserId
import ch.openolitor.core.models.BaseId
import scala.util._
import ch.openolitor.core.domain.UserCommand
import scalikejdbc.DB
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.exceptions.InvalidStateException
import ch.openolitor.core.domain.EventMetadata
import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.db.ConnectionPoolContextAware

object StammdatenCommandHandler {
  case class LieferplanungAbschliessenCommand(originator: UserId, id: LieferplanungId) extends UserCommand
  case class LieferplanungAbrechnenCommand(originator: UserId, id: LieferplanungId) extends UserCommand
  case class LieferungErneutBestellen(originator: UserId, id: LieferungId) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferungBestellenEvent(meta: EventMetadata, id: LieferungId) extends PersistentEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler with StammdatenDBMappings with ConnectionPoolContextAware {
  self: StammdatenWriteRepositoryComponent =>
  import StammdatenCommandHandler._

  override def handle(meta: EventMetadata): UserCommand => Option[Try[PersistentEvent]] = {
    case LieferplanungAbschliessenCommand(userId, id: LieferplanungId) =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Offen =>
              Success(LieferplanungAbschliessenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Lieferplanung has to be in status Offen in order to transition to Abgeschlossen"))
          }
        }
      }

    case LieferplanungAbrechnenCommand(userId, id: LieferplanungId) =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Abgeschlossen =>
              Success(LieferplanungAbrechnenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Lieferplanung has to be in status Abgeschlossen in order to transition to Verrechnet"))
          }
        }
      }

    case LieferungErneutBestellen(userId, id: LieferungId) =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferungMapping, id) map { lieferung =>
          lieferung.status match {
            case Offen | Abgeschlossen =>
              Success(LieferungBestellenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Lieferung has to be in status Offen | Abgeschlossen in order to execute LieferungBestellen"))
          }
        }
      }
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCommandHandler
    with DefaultStammdatenWriteRepositoryComponent {
}
