package ch.openolitor.core.domain

import akka.actor._
import ch.openolitor.core.repositories.CoreDBMappings
import ch.openolitor.core.db.ConnectionPoolContextAware
import scalikejdbc._
import scalikejdbc.SQLSyntax._
import ch.openolitor.core.models.PersistenceEventState
import ch.openolitor.core.Boot
import org.joda.time.DateTime
import ch.openolitor.core.models.PersistenceEventStateId

/**
 * This trait provides helper methods to keep track of latest processed sequenceNr of messages to limit reprocessing after a specified sequence nr per persistenceId
 */
trait PersistenceEventStateSupport extends Actor with ActorLogging with CoreDBMappings with ConnectionPoolContextAware {

  def persistenceId(): String

  lazy val peState = persistenceEventStateMapping.syntax("pe_state")
  private var lastSequenceNr = 0L

  val personId = Boot.systemPersonId

  override def preStart(): Unit = {
    lastSequenceNr = loadLastSequenceNr()
    super.preStart()
  }

  def lastProcessedSequenceNr = lastSequenceNr

  protected def loadLastSequenceNr(): Long = {
    DB readOnly { implicit session =>
      getByPersistenceId(persistenceId()) map (_.lastSequenceNr) getOrElse (0L)
    }
  }

  protected def getByPersistenceId(persistenceId: String)(implicit session: DBSession): Option[PersistenceEventState] = {
    withSQL {
      select
        .from(persistenceEventStateMapping as peState)
        .where.eq(peState.persistenceId, parameter(persistenceId))
    }.map(persistenceEventStateMapping.apply(peState)).single.apply()
  }

  protected def setLastProcessedSequenceNr(sequenceNr: Long): Boolean = {
    if (sequenceNr > lastSequenceNr) {
      DB autoCommit { implicit session =>
        getByPersistenceId(persistenceId()) match {
          case Some(entity: PersistenceEventState) =>
            val modEntity = entity.copy(lastSequenceNr = sequenceNr, modifidat = DateTime.now)
            val params = persistenceEventStateMapping.updateParameters(modEntity)
            val id = peState.id
            withSQL(update(persistenceEventStateMapping as peState).set(params: _*).where.eq(id, parameter(modEntity.id))).update.apply() == 1
          case None =>
            val entity = PersistenceEventState(PersistenceEventStateId(), persistenceId, sequenceNr, DateTime.now, personId, DateTime.now, personId)
            val params = persistenceEventStateMapping.parameterMappings(entity)
            withSQL(insertInto(persistenceEventStateMapping).values(params: _*)).update.apply() == 1
        }
      }
    } else {
      true
    }
  }
}