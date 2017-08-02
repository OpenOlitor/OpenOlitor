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
package ch.openolitor.stammdaten.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime
import org.joda.time.LocalDate
import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.AkkaEventStream
import ch.openolitor.core.EventStream

trait StammdatenWriteRepository extends StammdatenReadRepositorySync
    with StammdatenInsertRepository
    with StammdatenUpdateRepository
    with StammdatenDeleteRepository
    with BaseWriteRepository
    with EventStream {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)
}

trait StammdatenWriteRepositoryImpl extends StammdatenWriteRepository
    with StammdatenReadRepositorySyncImpl
    with StammdatenInsertRepositoryImpl
    with StammdatenUpdateRepositoryImpl
    with StammdatenDeleteRepositoryImpl
    with LazyLogging
    with StammdatenRepositoryQueries {
  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {
    DB autoCommit { implicit session =>
      sql"truncate table ${postlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotlieferungMapping.table}".execute.apply()
      sql"truncate table ${heimlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotMapping.table}".execute.apply()
      sql"truncate table ${tourMapping.table}".execute.apply()
      sql"truncate table ${abotypMapping.table}".execute.apply()
      sql"truncate table ${kundeMapping.table}".execute.apply()
      sql"truncate table ${pendenzMapping.table}".execute.apply()
      sql"truncate table ${customKundentypMapping.table}".execute.apply()
      sql"truncate table ${personMapping.table}".execute.apply()
      sql"truncate table ${depotlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${heimlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${postlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${lieferplanungMapping.table}".execute.apply()
      sql"truncate table ${lieferungMapping.table}".execute.apply()
      sql"truncate table ${lieferpositionMapping.table}".execute.apply()
      sql"truncate table ${bestellungMapping.table}".execute.apply()
      sql"truncate table ${bestellpositionMapping.table}".execute.apply()
      sql"truncate table ${produktMapping.table}".execute.apply()
      sql"truncate table ${produktekategorieMapping.table}".execute.apply()
      sql"truncate table ${produzentMapping.table}".execute.apply()
      sql"truncate table ${projektMapping.table}".execute.apply()
      sql"truncate table ${produktProduzentMapping.table}".execute.apply()
      sql"truncate table ${produktProduktekategorieMapping.table}".execute.apply()
      sql"truncate table ${abwesenheitMapping.table}".execute.apply()
      sql"truncate table ${korbMapping.table}".execute.apply()
      sql"truncate table ${tourlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotAuslieferungMapping.table}".execute.apply()
      sql"truncate table ${tourAuslieferungMapping.table}".execute.apply()
      sql"truncate table ${postAuslieferungMapping.table}".execute.apply()
    }
  }
}
