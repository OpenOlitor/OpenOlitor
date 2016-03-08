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
  def evolveDatabase(fromRevision: Int)(implicit cpContext: ConnectionPoolContext): Try[Int] = {    
    val scriptsToApply = scripts.take(scripts.length - fromRevision)
    evolve(scriptsToApply, fromRevision)
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
   * load current version
   */
  def currentVersion(implicit cpContext: ConnectionPoolContext): Int = {
    DB autoCommit { implicit session =>
      withSQL {
        select(max(schema.revision))
        .from(dbSchemaMapping as schema)
        .where.eq(schema.status, Applying)
      }.map(_.int(1)).single.apply().get
    }
  }

}