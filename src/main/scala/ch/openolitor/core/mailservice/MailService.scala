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
package ch.openolitor.core.mailservice

import ch.openolitor.core.domain.AggregateRoot
import akka.actor._
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.domain._
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore.UserCommandFailed
import org.joda.time.DateTime
import akka.persistence.SnapshotMetadata
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._
import courier._
import courier.Defaults._
import javax.mail.internet.InternetAddress
import scala.concurrent.Future
import stamina.Persister
import java.util.UUID
import scala.collection.immutable.TreeSet
import ch.openolitor.core.JSONSerializable
import ch.openolitor.util.ConfigUtil._

object MailService {
  import AggregateRoot._

  val VERSION = 1
  val persistenceId = "mail-store"

  case class MailServiceState(startTime: DateTime, seqNr: Long, mailQueue: TreeSet[MailEnqueued]) extends State

  case class SendMailCommandWithCallback[M <: AnyRef <% Persister[M, _]](originator: PersonId, entity: Mail, commandMeta: M) extends UserCommand
  case class SendMailCommand(originator: PersonId, entity: Mail) extends UserCommand

  //events raised by this aggregateroot
  case class MailServiceInitialized(meta: EventMetadata) extends PersistentEvent
  // resulting send mail event
  case class SendMailEvent(meta: EventMetadata, uid: String, mail: Mail, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable
  case class MailSentEvent(meta: EventMetadata, uid: String, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable
  case class SendMailFailedEvent(meta: EventMetadata, uid: String, afterNumberOfRetries: Int, commandMeta: Option[AnyRef]) extends PersistentEvent with JSONSerializable

  def props()(implicit sysConfig: SystemConfig): Props = Props(classOf[DefaultMailService], sysConfig)

  case object CheckMailQueue
}

trait MailService extends AggregateRoot
    with ConnectionPoolContextAware
    with CommandHandlerComponent
    with MailRetryHandler {

  import MailService._
  import AggregateRoot._

  override def persistenceId: String = MailService.persistenceId
  type S = MailServiceState

  lazy val fromAddress = sysConfig.mandantConfiguration.config.getString("smtp.from")
  lazy val MaxNumberOfRetries = sysConfig.mandantConfiguration.config.getInt("smtp.number-of-retries")
  lazy val sendEmailOutbound = sysConfig.mandantConfiguration.config.getBooleanOption("smtp.send-email").getOrElse(true)

  lazy val mailer = Mailer(sysConfig.mandantConfiguration.config.getString("smtp.endpoint"), sysConfig.mandantConfiguration.config.getInt("smtp.port"))
    .auth(true)
    .as(sysConfig.mandantConfiguration.config.getString("smtp.user"), sysConfig.mandantConfiguration.config.getString("smtp.password"))
    .startTtls(true)()

  override var state: MailServiceState = MailServiceState(DateTime.now, 0L, TreeSet.empty[MailEnqueued])

  override protected def afterEventPersisted(evt: PersistentEvent): Unit = {
    updateState(evt)
    publish(evt)
    state = incState
  }

  def initialize(): Unit = {
    // start mail queue checker
    context.system.scheduler.schedule(0 seconds, 10 seconds, self, CheckMailQueue)(context.system.dispatcher)
  }

  def checkMailQueue(): Unit = {
    if (!state.mailQueue.isEmpty) {
      state.mailQueue map { enqueued =>
        sendMail(enqueued.meta, enqueued.uid, enqueued.mail, enqueued.commandMeta) map { event =>
          persist(event)(afterEventPersisted)
        } recoverWith {
          case e =>
            log.error(s"Failed to send mail ${e.getMessage}. Trying again later.")

            calculateRetryEnqueued(enqueued) map { result =>
              state = state.copy(mailQueue = state.mailQueue - enqueued + result)
            } getOrElse {
              persist(SendMailFailedEvent(enqueued.meta, enqueued.uid, MaxNumberOfRetries, enqueued.commandMeta))(afterEventPersisted)
            }

            Future.failed(e)
        }
      }
    }
  }

  def sendMail(meta: EventMetadata, uid: String, mail: Mail, commandMeta: Option[AnyRef]): Future[MailSentEvent] = {
    if (sendEmailOutbound) {
      var envelope = Envelope.from(new InternetAddress(fromAddress))
        .to(InternetAddress.parse(mail.to): _*)
        .subject(mail.subject)
        .content(Text(mail.content))

      mail.cc map { cc =>
        envelope = envelope.cc(InternetAddress.parse(cc): _*)
      }

      mail.bcc map { bcc =>
        envelope = envelope.bcc(InternetAddress.parse(bcc): _*)
      }

      mailer(envelope) map { _ =>
        MailSentEvent(meta, uid, commandMeta)
      }
    } else {
      log.debug(s"=====================================================================")
      log.debug(s"| Sending Email: ${mail}")
      log.debug(s"=====================================================================")

      Future.successful(MailSentEvent(meta, uid, commandMeta))
    }
  }

  def enqueueMail(meta: EventMetadata, uid: String, mail: Mail, commandMeta: Option[AnyRef]): Unit = {
    state = state.copy(mailQueue = state.mailQueue + MailEnqueued(meta, uid, mail, commandMeta, DateTime.now(), 0))
  }

  def dequeueMail(uid: String): Unit = {
    state.mailQueue.find(_.uid == uid) map { dequeue =>
      state = state.copy(mailQueue = state.mailQueue - dequeue)
    }
  }

  override def updateState(evt: PersistentEvent): Unit = {
    log.debug(s"updateState:$evt")
    evt match {
      case MailServiceInitialized(_) =>
      case SendMailEvent(meta, uid, mail, commandMeta) =>
        enqueueMail(meta, uid, mail, commandMeta)
        self ! CheckMailQueue
      case MailSentEvent(_, uid, _) =>
        dequeueMail(uid)
      case SendMailFailedEvent(_, uid, _, _) =>
        dequeueMail(uid)
      case _ =>
    }
  }

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    log.debug(s"restoreFromSnapshot:$state")
    state match {
      case Removed => context become removed
      case Created => context become uninitialized
      case s: MailServiceState => this.state = s
      case other => log.error(s"Received unsupported state:$other")
    }
  }

  val uninitialized: Receive = {
    case GetState =>
      log.debug(s"uninitialized => GetState: $state")
      sender ! state
    case Initialize(state) =>
      // testing
      log.debug(s"uninitialized => Initialize: $state")
      this.state = state
      initialize()
      context become created
    case CheckMailQueue =>
  }

  val created: Receive = {
    case KillAggregate =>
      log.debug(s"created => KillAggregate")
      context.stop(self)
    case GetState =>
      log.debug(s"created => GetState")
      sender ! state
    case CheckMailQueue =>
      checkMailQueue()
    case SendMailCommandWithCallback(personId, mail, commandMeta) =>
      val meta = metadata(personId)
      val id = newId
      val event = SendMailEvent(meta, id, mail, Some(commandMeta))
      persist(event) { result =>
        afterEventPersisted(result)
        sender ! result
      }
    case SendMailCommand(personId, mail) =>
      val meta = metadata(personId)
      val id = newId
      val event = SendMailEvent(meta, id, mail, None)
      persist(event) { result =>
        afterEventPersisted(result)
        sender ! result
      }
    case other =>
      log.error(s"Received unknown command:$other")
  }

  val removed: Receive = {
    case GetState =>
      log.warning(s"Received GetState in state removed")
      sender() ! state
    case KillAggregate =>
      log.warning(s"Received KillAggregate in state removed")
      context.stop(self)
  }

  def metadata(personId: PersonId) = {
    EventMetadata(personId, VERSION, DateTime.now, state.seqNr, persistenceId)
  }

  def incState = {
    state.copy(seqNr = state.seqNr + 1)
  }

  def newId: String = UUID.randomUUID.toString

  override def afterRecoveryCompleted(): Unit = {
    context become created
    initialize()
  }

  override val receiveCommand = uninitialized
}

class DefaultMailService(override val sysConfig: SystemConfig) extends MailService
    with DefaultCommandHandlerComponent
    with DefaultMailRetryHandler {
  val system = context.system
}