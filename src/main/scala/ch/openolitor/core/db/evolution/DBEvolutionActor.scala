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