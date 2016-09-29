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
import ch.openolitor.core.calculations.Calculations.StartCalculation
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc._
import ch.openolitor.stammdaten.repositories.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.util.Failure
import ch.openolitor.core.calculations.Calculations.CalculationResult
import org.joda.time.DateTime

object LieferungCounterCalculation {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[LieferungCounterCalculation], sysConfig, system)
}

class LieferungCounterCalculation(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BaseCalculation
    with AsyncConnectionPoolContextAware
    with DefaultStammdatenWriteRepositoryComponent
    with StammdatenDBMappings {

  override def calculate(): Unit = {
    DB autoCommit { implicit session =>
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='WirdGeliefert' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_koerbe_zu_liefern=k.counter""".execute.apply()
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='FaelltAusAbwesend' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_abwesenheiten=k.counter""".execute.apply()
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='FaelltAusSaldoZuTief' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_saldo_zu_tief=k.counter""".execute.apply()
    }
  }

  protected def handleInitialization(): Unit = {
    // disable scheduled calculation for now
  }
}
