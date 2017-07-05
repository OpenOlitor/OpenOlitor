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
import ch.openolitor.core.DBEvolutionReference
import ch.openolitor.core.db.evolution.DBEvolutionActor
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{ Success, Failure }
import akka.pattern.ask

/**
 * This trait provides helper methods to keep track of latest processed sequenceNr of messages to limit reprocessing after a specified sequence nr per persistenceId
 */
trait PersistenceEventStateSupport extends Actor with ActorLogging with CoreDBMappings with ConnectionPoolContextAware with DBEvolutionReference {

  def persistenceStateStoreId: String

  lazy val peState = persistenceEventStateMapping.syntax("pe_state")
  private var lastTransactionNr = 0L
  private var lastSequenceNr = 0L
  var dbState: DBEvolutionActor.DBEvolutionState = _

  val personId = Boot.systemPersonId
  implicit val excecutionContext = context.dispatcher

  /**
   * start with event recovery after evolution complete
   */
  override def preStart(): Unit = {
    implicit val timeout = Timeout(50.seconds)
    log.debug(s"preStart PersistenceEventStateSupport")
    dbEvolutionActor ? DBEvolutionActor.CheckDBEvolution map {
      case Success(state: DBEvolutionActor.DBEvolutionState) =>
        val (tnr, snr) = loadLastSequenceNr()
        lastTransactionNr = tnr
        lastSequenceNr = snr
        dbState = state
        log.debug(s"Initialize PersistenceEventStateSupport ${persistenceStateStoreId} to lastSequenceNr: $lastTransactionNr.$lastSequenceNr")
        dbInitialized()
        super.preStart()
      case Failure(e) =>
        log.warning(s"Failed initializing DB, stopping PersistenceEventStateSupport ${persistenceStateStoreId}")
      //self ! PoisonPill
      case x =>
        log.warning(s"Received unexpected result:$x")
    }
  }

  def dbInitialized(): Unit = {}

  def lastProcessedSequenceNr = lastSequenceNr

  def lastProcessedTransactionNr = lastTransactionNr

  protected def loadLastSequenceNr(): (Long, Long) = {
    DB readOnly { implicit session =>
      getByPersistenceId(persistenceStateStoreId) map (x => (x.lastTransactionNr, x.lastSequenceNr)) getOrElse ((0L, 0L))
    }
  }

  protected def getByPersistenceId(persistenceId: String)(implicit session: DBSession): Option[PersistenceEventState] = {
    withSQL {
      select
        .from(persistenceEventStateMapping as peState)
        .where.eq(peState.persistenceId, parameter(persistenceId))
    }.map(persistenceEventStateMapping.apply(peState)).single.apply()
  }

  protected def setLastProcessedSequenceNr(meta: EventMetadata): Boolean = {
    log.debug(s"setLastProcessedSequenceNr in $persistenceStateStoreId ${meta.transactionNr}.${meta.seqNr} > ${lastTransactionNr}.${lastSequenceNr}")
    if (meta.transactionNr > lastTransactionNr) {
      lastSequenceNr = meta.seqNr
      lastTransactionNr = meta.transactionNr
      upatePersistenceEventState()
    } else if (meta.transactionNr == lastTransactionNr && meta.seqNr > lastSequenceNr) {
      lastSequenceNr = meta.seqNr
      upatePersistenceEventState()
    } else {
      true
    }
  }

  private def upatePersistenceEventState() = {
    DB autoCommit { implicit session =>
      getByPersistenceId(persistenceStateStoreId) match {
        case Some(entity: PersistenceEventState) =>
          val modEntity = entity.copy(lastTransactionNr = lastTransactionNr, lastSequenceNr = lastSequenceNr, modifidat = DateTime.now)
          val params = persistenceEventStateMapping.updateParameters(modEntity)
          val id = peState.id
          withSQL(update(persistenceEventStateMapping as peState).set(params: _*).where.eq(id, parameter(modEntity.id))).update.apply() == 1
        case None =>
          val entity = PersistenceEventState(PersistenceEventStateId(), persistenceStateStoreId, lastTransactionNr, lastSequenceNr, DateTime.now, personId, DateTime.now, personId)
          val params = persistenceEventStateMapping.parameterMappings(entity)
          withSQL(insertInto(persistenceEventStateMapping).values(params: _*)).update.apply() == 1
      }
    }
  }
}