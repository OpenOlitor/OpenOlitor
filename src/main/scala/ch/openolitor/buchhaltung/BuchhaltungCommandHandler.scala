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

import ch.openolitor.core.domain.CommandHandler
import ch.openolitor.core.domain.EventMetadata
import ch.openolitor.core.domain.PersistentEvent
import ch.openolitor.buchhaltung.models.RechnungId
import ch.openolitor.buchhaltung.models.RechnungModify
import ch.openolitor.core.models.UserId
import ch.openolitor.core.models.BaseId
import scala.util._
import ch.openolitor.core.domain.UserCommand
import scalikejdbc.DB
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.exceptions.InvalidStateException
import ch.openolitor.core.domain.EventMetadata
import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.db.ConnectionPoolContextAware

object BuchhaltungCommandHandler {
  case class RechnungVerschickenCommand(originator: UserId, id: RechnungId) extends UserCommand
  case class RechnungMahnungVerschickenCommand(originator: UserId, id: RechnungId) extends UserCommand
  case class RechnungBezahlenCommand(originator: UserId, id: RechnungId, entity: RechnungModifyBezahlt) extends UserCommand
  case class RechnungStornierenCommand(originator: UserId, id: RechnungId) extends UserCommand

  case class RechnungVerschicktEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class RechnungMahnungVerschicktEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
  case class RechnungBezahltEvent(meta: EventMetadata, id: RechnungId, entity: RechnungModifyBezahlt) extends PersistentEvent with JSONSerializable
  case class RechnungStorniertEvent(meta: EventMetadata, id: RechnungId) extends PersistentEvent with JSONSerializable
}

trait BuchhaltungCommandHandler extends CommandHandler with BuchhaltungDBMappings with ConnectionPoolContextAware {
  self: BuchhaltungWriteRepositoryComponent =>
  import BuchhaltungCommandHandler._

  override val handle: PartialFunction[UserCommand, EventMetadata => Try[PersistentEvent]] = {
    case RechnungVerschickenCommand(userId, id: RechnungId) => meta =>
      DB readOnly { implicit session =>
        buchhaltungWriteRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Erstellt =>
              Success(RechnungVerschicktEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Eine Rechnung kann nur im Status 'Erstellt' verschickt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungMahnungVerschickenCommand(userId, id: RechnungId) => meta =>
      DB readOnly { implicit session =>
        buchhaltungWriteRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Verschickt =>
              Success(RechnungMahnungVerschicktEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Eine Mahnung kann nur im Status 'Verschickt' verschickt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungBezahlenCommand(userId, id: RechnungId, entity: RechnungModifyBezahlt) => meta =>
      DB readOnly { implicit session =>
        buchhaltungWriteRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Verschickt | MahnungVerschickt =>
              Success(RechnungBezahltEvent(meta, id, entity))
            case _ =>
              Failure(new InvalidStateException("Eine Rechnung kann nur im Status 'Verschickt' oder 'MahnungVerschickt' bezahlt werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }

    case RechnungStornierenCommand(userId, id: RechnungId) => meta =>
      DB readOnly { implicit session =>
        buchhaltungWriteRepository.getById(rechnungMapping, id) map { rechnung =>
          rechnung.status match {
            case Bezahlt =>
              Failure(new InvalidStateException("Eine Rechnung im Status 'Bezahlt' kann nicht mehr storniert werden"))
            case _ =>
              Success(RechnungStorniertEvent(meta, id))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Rechnung mit der Nr. $id gefunden")))
      }
  }
}

class DefaultBuchhaltungCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BuchhaltungCommandHandler
    with DefaultBuchhaltungWriteRepositoryComponent {
}