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

import scalikejdbc._
import scalikejdbc.SQLSyntax._
import ch.openolitor.core.repositories.CoreDBMappings
import scala.util._
import ch.openolitor.core.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.db.evolution.scripts.V1Scripts

trait Script {
  def execute(implicit session: DBSession): Try[Boolean]
}

case class EvolutionException(msg: String) extends Exception 

object Evolution extends Evolution(V1Scripts.scripts)

/**
 * Base evolution class to evolve database from a specific revision to another
 */
class Evolution(scripts:Seq[Script]) extends CoreDBMappings with LazyLogging {  
  def evolveDatabase(fromRevision: Int=0)(implicit cpContext: ConnectionPoolContext): Try[Int] = {
    val currentDBRevision = currentRevision
    val revision = Math.max(fromRevision, currentDBRevision)
    val scriptsToApply = scripts.take(scripts.length - revision)
    evolve(scriptsToApply, revision)
  }

  def evolve(scripts:Seq[Script], currentRevision: Int)(implicit cpContext: ConnectionPoolContext): Try[Int] = {
    scripts.headOption.map {  head => 
      try {
        DB localTx { implicit session => 
          head.execute match {
            case Success(x) => 
              logger.info(s"evolved database to $currentRevision, applied => $x")
              insertRevision(currentRevision) match {
                case true =>
                  //proceed with next script
                  evolve(scripts.tail, currentRevision+1)                  
                case false =>
                  //fail
                  throw EvolutionException(s"Couldn't register new db schema") 
              }
            case Failure(e) => 
              //failed throw e to rollback transaction
              throw e
          }
        }
      }
      catch {
        case e : Exception =>
          Failure(e)
      }
    }.getOrElse(Success(currentRevision))
  }
  
  def insertRevision(revision: Int)(implicit session: DBSession):Boolean = {
    val entity = DBSchema(DBSchemaId(), revision, Done)
    val params = dbSchemaMapping.parameterMappings(entity)
    withSQL(insertInto(dbSchemaMapping).values(params: _*)).update.apply() == 1
  }
  
  lazy val schema = dbSchemaMapping.syntax("db_schema")
  
  /**
   * load current revision from database schema
   */
  def currentRevision(implicit cpContext: ConnectionPoolContext): Int = {
    DB autoCommit { implicit session =>
      withSQL {
        select(max(schema.revision))
        .from(dbSchemaMapping as schema)
        .where.eq(schema.status, Applying)
      }.map(_.int(1)).single.apply().get
    }
  }
}