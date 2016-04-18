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
import ch.openolitor.util.IteratorUtil
import org.joda.time.DateTime

trait Script {
  def execute(implicit session: DBSession): Try[Boolean]
}

case class EvolutionException(msg: String) extends Exception

object Evolution extends Evolution(V1Scripts.scripts)

/**
 * Base evolution class to evolve database from a specific revision to another
 */
class Evolution(scripts: Seq[Script]) extends CoreDBMappings with LazyLogging {
  import IteratorUtil._

  logger.debug(s"Evolution manager consists of:$scripts")

  def evolveDatabase(fromRevision: Int = 0)(implicit cpContext: ConnectionPoolContext, userId: UserId): Try[Int] = {
    val currentDBRevision = DB readOnly { implicit session => currentRevision }
    val revision = Math.max(fromRevision, currentDBRevision)
    scripts.take(scripts.length - revision) match {
      case Nil => Success(revision)
      case scriptsToApply => evolve(scriptsToApply, revision)
    }
  }

  def evolve(scripts: Seq[Script], currentRevision: Int)(implicit cpContext: ConnectionPoolContext, userId: UserId): Try[Int] = {
    logger.debug(s"evolve database from:$currentRevision")
    val x = scripts.zipWithIndex.view.map {
      case (script, index) =>
        try {
          logger.debug(s"evolve script:$script [$index]")
          DB localTx { implicit session =>
            script.execute match {
              case Success(x) =>
                val rev = currentRevision + index + 1
                insertRevision(rev) match {
                  case true => Success(rev)
                  case false =>
                    //fail
                    throw EvolutionException(s"Couldn't register new db schema")
                }
              case Failure(e) =>
                //failed throw e to rollback transaction
                throw e
            }
          }
        } catch {
          case e: Exception =>
            logger.warn(s"catched exception:", e)
            Failure(e)
        }
    }.toIterator.takeWhileInclusive(_.isSuccess).toSeq

    logger.debug(s"Evolved:$x:${x.reverse.headOption.getOrElse("xxx")}")
    x.reverse.headOption.getOrElse(Failure(EvolutionException(s"No Script found")))
  }

  def insertRevision(revision: Int)(implicit session: DBSession, userId: UserId): Boolean = {
    val entity = DBSchema(DBSchemaId(), revision, Done, DateTime.now, userId, DateTime.now, userId)
    val params = dbSchemaMapping.parameterMappings(entity)
    withSQL(insertInto(dbSchemaMapping).values(params: _*)).update.apply() == 1
  }

  lazy val schema = dbSchemaMapping.syntax("db_schema")

  /**
   * load current revision from database schema
   */
  def currentRevision(implicit session: DBSession): Int = {
    withSQL {
      select(max(schema.revision))
        .from(dbSchemaMapping as schema)
        .where.eq(schema.status, parameter(Applying))
    }.map(_.intOpt(1).getOrElse(0)).single.apply().getOrElse(0)
  }

  def revisions(implicit session: DBSession): List[DBSchema] = {
    withSQL {
      select
        .from(dbSchemaMapping as schema)
        .where.eq(schema.status, parameter(Applying))
    }.map(dbSchemaMapping(schema)).list.apply()
  }
}