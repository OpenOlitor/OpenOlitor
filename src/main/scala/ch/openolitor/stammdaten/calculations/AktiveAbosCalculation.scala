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
package ch.openolitor.stammdaten.calculations

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorLogging
import ch.openolitor.core.calculations.BaseCalculation
import scala.concurrent.duration._
import ch.openolitor.core.calculations.Calculations._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.util.Success
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.stammdaten.repositories.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc._
import sqls.{ distinct, count }
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.stammdaten.repositories.StammdatenRepositoryQueries
import ch.openolitor.stammdaten.StammdatenCommandHandler
import akka.actor.ActorRef
import ch.openolitor.stammdaten.models.AboId

object AktiveAbosCalculation {
  def props(sysConfig: SystemConfig, system: ActorSystem, entityStore: ActorRef): Props = Props(classOf[AktiveAbosCalculation], sysConfig, system, entityStore)
}

class AktiveAbosCalculation(override val sysConfig: SystemConfig, override val system: ActorSystem, val entityStore: ActorRef) extends BaseCalculation
    with AsyncConnectionPoolContextAware
    with DefaultStammdatenWriteRepositoryComponent
    with StammdatenRepositoryQueries {

  override def calculate(): Unit = {
    DB autoCommit { implicit session =>
      val yesterday = LocalDate.now.minusDays(1).toDateTimeAtStartOfDay
      val today = LocalDate.now.toDateTimeAtStartOfDay

      val aktiviert = getAktivierteAbosQuery()

      logger.debug(s"Found ${aktiviert.size} activated abos for ${sysConfig.mandantConfiguration.name}")

      aktiviert foreach { id =>
        entityStore ! StammdatenCommandHandler.AboAktivierenCommand(id)
      }

      val deaktiviert = getDeaktivierteAbosQuery()

      logger.debug(s"found ${deaktiviert.size} deactivated abos for ${sysConfig.mandantConfiguration.name}")

      deaktiviert foreach { id =>
        entityStore ! StammdatenCommandHandler.AboDeaktivierenCommand(id)
      }
    }
  }

  protected def handleInitialization(): Unit = {
    scheduledCalculation = Some(context.system.scheduler.schedule(untilNextMidnight, 24 hours)(self ! StartCalculation))
  }
}
