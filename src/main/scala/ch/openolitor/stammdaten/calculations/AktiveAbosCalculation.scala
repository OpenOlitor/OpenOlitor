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
import org.joda.time.DateTime

object AktiveAbosCalculation {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[AktiveAbosCalculation], sysConfig, system)
}

class AktiveAbosCalculation(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BaseCalculation
    with AsyncConnectionPoolContextAware
    with DefaultStammdatenWriteRepositoryComponent
    with StammdatenDBMappings {

  override def calculate(): Unit = {
    DB autoCommit { implicit session =>
      val now = DateTime.now.toLocalDate

      sql"""update ${depotMapping.table} u 
        INNER JOIN (SELECT a.depot_id, count(a.id) counter FROM ${depotlieferungAboMapping.table} a 
        WHERE (${now} BETWEEN a.start AND a.ende)) a ON u.id=a.depot_id 
        SET u.anzahl_abonnenten_aktiv=a.counter""".execute.apply()

      sql"""update ${tourMapping.table} u 
        INNER JOIN (SELECT a.tour_id, count(a.id) counter FROM ${heimlieferungAboMapping.table} a 
        WHERE (${now} BETWEEN a.start AND a.ende)) a ON u.id=a.tour_id 
        SET u.anzahl_abonnenten_aktiv=a.counter""".execute.apply()

      sql"""update ${depotlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${depotlieferungAboMapping.table} a 
        WHERE (${now} BETWEEN a.start AND a.ende)) a ON u.id=a.vertriebsart_id 
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${heimlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${heimlieferungAboMapping.table} a 
        WHERE (${now} BETWEEN a.start AND a.ende)) a ON u.id=a.vertriebsart_id 
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${postlieferungMapping.table} u 
        INNER JOIN (SELECT a.vertriebsart_id, count(a.id) counter FROM ${postlieferungAboMapping.table} a 
        WHERE (${now} BETWEEN a.start AND a.ende)) a ON u.id=a.vertriebsart_id
        SET u.anzahl_abos_aktiv=a.counter""".execute.apply()

      sql"""update ${abotypMapping.table} u 
        INNER JOIN (SELECT d.abotyp_id, count(d.id) counter FROM ${depotlieferungAboMapping.table} d 
        WHERE (${now} BETWEEN d.start AND d.ende)) d ON u.id=d.abotyp_id 
        INNER JOIN (SELECT t.abotyp_id, count(t.id) counter FROM ${heimlieferungAboMapping.table} t 
        WHERE (${now} BETWEEN t.start AND t.ende)) t ON u.id=t.abotyp_id 
        INNER JOIN (SELECT p.abotyp_id, count(p.id) counter FROM ${postlieferungAboMapping.table} p 
        WHERE (${now} BETWEEN p.start AND p.ende)) p ON u.id=p.abotyp_id 
        SET u.anzahl_abonnenten_aktiv=d.counter + t.counter + p.counter""".execute.apply()

      sql"""update ${kundeMapping.table} u 
        INNER JOIN (SELECT d.kunde_id, count(d.id) counter FROM ${depotlieferungAboMapping.table} d 
        WHERE (${now} BETWEEN d.start AND d.ende)) d ON u.id=d.kunde_id 
        INNER JOIN (SELECT t.kunde_id, count(t.id) counter FROM ${heimlieferungAboMapping.table} t 
        WHERE (${now} BETWEEN t.start AND t.ende)) t ON u.id=t.kunde_id 
        INNER JOIN (SELECT p.kunde_id, count(p.id) counter FROM ${postlieferungAboMapping.table} p 
        WHERE (${now} BETWEEN p.start AND p.ende)) p ON u.id=p.kunde_id 
        SET u.anzahl_abos_aktiv=d.counter + t.counter + p.counter""".execute.apply()

    }
  }
}
