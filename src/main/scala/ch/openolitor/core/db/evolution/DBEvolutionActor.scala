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
package ch.openolitor.core.db.evolution

import akka.actor._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.domain.DefaultCommandHandlerComponent
import scala.util.{ Try, Success, Failure }
import ch.openolitor.core.Boot
import ch.openolitor.core.db.ConnectionPoolContextAware
import scalikejdbc.DB
import ch.openolitor.core.models.BaseId

object DBEvolutionActor {
  case object CheckDBEvolution

  case class DBEvolutionState(dbRevision: Int)

  def props(evolution: Evolution)(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultDBEvolutionActor], sysConfig, evolution)
}

trait DBEvolutionActor extends Actor with ActorLogging with ConnectionPoolContextAware {
  import DBEvolutionActor._
  val evolution: Evolution

  var state = DBEvolutionState(0)
  var exception: Throwable = _

  val created: Receive = {
    case CheckDBEvolution =>
      log.debug(s"received additional CheckDBEvolution; evolution has been successful, otherwise I would be in uncheckedDB")
      sender ! Success(state)
  }

  val failed: Receive = {
    case CheckDBEvolution =>
      log.debug(s"received additional CheckDBEvolution; evolution has been successful, otherwise I would be in uncheckedDB")
      sender ! Failure(exception)
  }

  val uncheckedDB: Receive = {
    case CheckDBEvolution =>
      log.debug(s"uncheckedDB => check db evolution")
      sender ! checkDBEvolution()
    case x =>
      log.error(s"uncheckedDB => unsupported command:$x")
  }

  def checkDBEvolution(): Try[DBEvolutionState] = {
    log.debug(s"Check DB Evolution: current revision=${state.dbRevision}")
    implicit val personId = Boot.systemPersonId
    evolution.evolveDatabase(state.dbRevision) match {
      case Success(rev) =>
        log.debug(s"Successfully updated to db rev:$rev")
        state = state.copy(dbRevision = rev)

        context become created
        Success(state)
      case Failure(e) =>
        log.warning(s"dB Evolution failed", e)
        exception = e
        DB readOnly { implicit session =>
          val newRev = evolution.currentRevision
          state = state.copy(dbRevision = newRev)
        }
        context become failed
        Failure(e)
    }
  }

  def receive: Receive = uncheckedDB
}

class DefaultDBEvolutionActor(override val sysConfig: SystemConfig, override val evolution: Evolution) extends DBEvolutionActor {
  val system = context.system
}