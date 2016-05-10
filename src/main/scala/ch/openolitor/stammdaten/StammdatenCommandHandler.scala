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
  case class BestellungErneutVersenden(originator: UserId, id: BestellungId) extends UserCommand

  case class LieferplanungAbschliessenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class LieferplanungAbrechnenEvent(meta: EventMetadata, id: LieferplanungId) extends PersistentEvent with JSONSerializable
  case class BestellungVersendenEvent(meta: EventMetadata, id: BestellungId) extends PersistentEvent with JSONSerializable
}

trait StammdatenCommandHandler extends CommandHandler with StammdatenDBMappings with ConnectionPoolContextAware {
  self: StammdatenWriteRepositoryComponent =>
  import StammdatenCommandHandler._

  override val handle: PartialFunction[UserCommand, EventMetadata => Try[PersistentEvent]] = {
    case LieferplanungAbschliessenCommand(userId, id: LieferplanungId) => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Offen =>
              Success(LieferplanungAbschliessenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Offen' abgeschlossen werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden")))
      }

    case LieferplanungAbrechnenCommand(userId, id: LieferplanungId) => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
          lieferplanung.status match {
            case Abgeschlossen =>
              Success(LieferplanungAbrechnenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Eine Lieferplanung kann nur im Status 'Abgeschlossen' verrechnet werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Lieferplanung mit der Nr. $id gefunden")))
      }

    case BestellungErneutVersenden(userId, id: BestellungId) => meta =>
      DB readOnly { implicit session =>
        stammdatenWriteRepository.getById(bestellungMapping, id) map { bestellung =>
          bestellung.status match {
            case Offen | Abgeschlossen =>
              Success(BestellungVersendenEvent(meta, id))
            case _ =>
              Failure(new InvalidStateException("Eine Bestellung kann nur in den Stati 'Offen' oder 'Abgeschlossen' erneut versendet werden"))
          }
        } getOrElse (Failure(new InvalidStateException(s"Keine Bestellung mit der Nr. $id gefunden")))
      }
  }
}

class DefaultStammdatenCommandHandler(override val sysConfig: SystemConfig, override val system: ActorSystem) extends StammdatenCommandHandler
    with DefaultStammdatenWriteRepositoryComponent {
}
