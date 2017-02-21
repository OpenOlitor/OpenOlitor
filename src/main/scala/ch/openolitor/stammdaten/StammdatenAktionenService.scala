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
package ch.openolitor.stammdaten

import ch.openolitor.core._
import ch.openolitor.core.Macros._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import scala.concurrent.duration._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import shapeless.LabelledGeneric
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import ch.openolitor.core.models.PersonId
import ch.openolitor.stammdaten.StammdatenCommandHandler._
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.stammdaten.eventsourcing.StammdatenEventStoreSerializer
import org.joda.time.DateTime
import ch.openolitor.core.mailservice.Mail
import ch.openolitor.core.mailservice.MailService._
import org.joda.time.format.DateTimeFormat
import ch.openolitor.util.ConfigUtil._
import scalikejdbc.DBSession

object StammdatenAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem, mailService: ActorRef): StammdatenAktionenService = new DefaultStammdatenAktionenService(sysConfig, system, mailService)
}

class DefaultStammdatenAktionenService(sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef)
    extends StammdatenAktionenService(sysConfig, mailService) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Stammdaten Modul
 */
class StammdatenAktionenService(override val sysConfig: SystemConfig, override val mailService: ActorRef) extends EventService[PersistentEvent] with LazyLogging with AsyncConnectionPoolContextAware
    with StammdatenDBMappings with MailServiceReference with StammdatenEventStoreSerializer {
  self: StammdatenWriteRepositoryComponent =>

  implicit val timeout = Timeout(15.seconds) //sending mails might take a little longer

  lazy val config = sysConfig.mandantConfiguration.config
  lazy val BaseZugangLink = config.getStringOption(s"security.zugang-base-url").getOrElse("")
  lazy val BasePasswortResetLink = config.getStringOption(s"security.passwort-reset-base-url").getOrElse("")

  val handle: Handle = {
    case LieferplanungAbschliessenEvent(meta, id: LieferplanungId) =>
      lieferplanungAbschliessen(meta, id)
    case LieferplanungAbrechnenEvent(meta, id: LieferplanungId) =>
      lieferplanungVerrechnet(meta, id)
    case SammelbestellungVersendenEvent(meta, id: SammelbestellungId) =>
      sammelbestellungVersenden(meta, id)
    case AuslieferungAlsAusgeliefertMarkierenEvent(meta, id: AuslieferungId) =>
      auslieferungAusgeliefert(meta, id)
    case SammelbestellungAlsAbgerechnetMarkierenEvent(meta, datum, id: SammelbestellungId) =>
      sammelbestellungAbgerechnet(meta, datum, id)
    case PasswortGewechseltEvent(meta, personId, pwd, einladungId) =>
      updatePasswort(meta, personId, pwd, einladungId)
    case LoginDeaktiviertEvent(meta, _, personId) =>
      disableLogin(meta, personId)
    case LoginAktiviertEvent(meta, _, personId) =>
      enableLogin(meta, personId)
    case EinladungGesendetEvent(meta, einladung) =>
      sendEinladung(meta, einladung)
    case PasswortResetGesendetEvent(meta, einladung) =>
      sendPasswortReset(meta, einladung)
    case RolleGewechseltEvent(meta, _, personId, rolle) =>
      changeRolle(meta, personId, rolle)
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def lieferplanungAbschliessen(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Offen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Abgeschlossen))
        }
      }
    }
  }

  def lieferplanungVerrechnet(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, id) map { lieferplanung =>
        if (Abgeschlossen == lieferplanung.status) {
          stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.copy(status = Verrechnet))
        }
      }
      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        if (Abgeschlossen == lieferung.status) {
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](lieferung.copy(status = Verrechnet))
        }
      }
      stammdatenWriteRepository.getSammelbestellungen(id) map { sammelbestellung =>
        if (Abgeschlossen == sammelbestellung.status) {
          stammdatenWriteRepository.updateEntity[Sammelbestellung, SammelbestellungId](sammelbestellung.copy(status = Verrechnet, datumAbrechnung = Some(DateTime.now)))
        }
      }
    }
  }

  def sammelbestellungVersenden(meta: EventMetadata, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    val format = DateTimeFormat.forPattern("dd.MM.yyyy")

    DB autoCommit { implicit session =>
      //send mails to Produzenten
      stammdatenWriteRepository.getProjekt map { projekt =>
        stammdatenWriteRepository.getById(sammelbestellungMapping, id) map {
          //send mails only if current event timestamp is past the timestamp of last delivered mail
          case sammelbestellung if (sammelbestellung.datumVersendet.isEmpty || sammelbestellung.datumVersendet.get.isBefore(meta.timestamp)) =>
            stammdatenWriteRepository.getProduzentDetail(sammelbestellung.produzentId) map { produzent =>

              val bestellungen = stammdatenWriteRepository.getBestellungen(sammelbestellung.id) map { bestellung =>

                val bestellpositionen = stammdatenWriteRepository.getBestellpositionen(bestellung.id) map {
                  bestellposition =>
                    val preisPos = bestellposition.preisEinheit.getOrElse(0: BigDecimal) * bestellposition.menge
                    val mengeTotal = bestellposition.anzahl * bestellposition.menge
                    s"""${bestellposition.produktBeschrieb}: ${bestellposition.anzahl} x ${bestellposition.menge} ${bestellposition.einheit} à ${bestellposition.preisEinheit.getOrElse("")} ≙ ${preisPos} = ${bestellposition.preis.getOrElse("")} ${projekt.waehrung} ⇒ ${mengeTotal} ${bestellposition.einheit}"""
                }

                s"""Adminprozente: ${bestellung.adminProzente}%:
                ${bestellpositionen.mkString("\n")}
                """

              }
              val text = s"""Bestellung von ${projekt.bezeichnung} an ${produzent.name} ${produzent.vorname.getOrElse("")}:

Lieferung: ${format.print(sammelbestellung.datum)}

Bestellpositionen:
${bestellungen.mkString("\n")}

Summe [${projekt.waehrung}]: ${sammelbestellung.preisTotal}"""
              val mail = Mail(1, produzent.email, None, None, "Bestellung " + format.print(sammelbestellung.datum), text)

              mailService ? SendMailCommandWithCallback(SystemEvents.SystemPersonId, mail, Some(5 minutes), id) map {
                case _: SendMailEvent =>
                // ok
                case other =>
                  logger.debug(s"Sending Mail failed resulting in $other")
              }
            }
          case _ => //ignore
            logger.debug(s"Don't resend Bestellung, already delivered")
        }
      }
    }
  }

  def updatePasswort(meta: EventMetadata, id: PersonId, pwd: Array[Char], einladungId: Option[EinladungId])(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(personMapping, id) map { person =>
        val updated = person.copy(passwort = Some(pwd))
        stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
      }

      einladungId map { id =>
        stammdatenWriteRepository.deleteEntity[Einladung, EinladungId](id)
      }
    }
  }

  def disableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(personMapping, personId) map { person =>
        val updated = person.copy(loginAktiv = false)
        stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
      }
    }
  }

  def enableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      setLoginAktiv(meta, personId)
    }
  }

  def setLoginAktiv(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator, session: DBSession) = {
    stammdatenWriteRepository.getById(personMapping, personId) map { person =>
      val updated = person.copy(loginAktiv = true)
      stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
    }
  }

  def sendPasswortReset(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, "Sie können ihr Passwort mit folgendem Link neu setzten:", BasePasswortResetLink)
  }

  def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, "Aktivieren Sie ihren Zugang mit folgendem Link:", BaseZugangLink)
  }

  def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate, baseText: String, baseLink: String)(implicit originator: PersonId): Unit = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(personMapping, einladungCreate.personId) map { person =>

        // existierende einladung überprüfen
        val einladung = stammdatenWriteRepository.getById(einladungMapping, einladungCreate.id) getOrElse {
          val inserted = copyTo[EinladungCreate, Einladung](
            einladungCreate,
            "erstelldat" -> meta.timestamp,
            "ersteller" -> meta.originator,
            "modifidat" -> meta.timestamp,
            "modifikator" -> meta.originator
          )

          stammdatenWriteRepository.insertEntity[Einladung, EinladungId](inserted)
          inserted
        }

        if ((einladung.datumVersendet.isEmpty || einladung.datumVersendet.get.isBefore(meta.timestamp)) && einladung.expires.isAfter(DateTime.now)) {
          setLoginAktiv(meta, einladung.personId)

          val text = s"""
	        ${person.vorname} ${person.name},

	        ${baseText} ${baseLink}?token=${einladung.uid}

	        """

          // email wurde bereits im CommandHandler überprüft
          val mail = Mail(1, person.email.get, None, None, "OpenOlitor Zugang", text)

          mailService ? SendMailCommandWithCallback(originator, mail, Some(5 minutes), einladung.id) map {
            case _: SendMailEvent =>
            // ok
            case other =>
              logger.debug(s"Sending Mail failed resulting in $other")
          }
        }
      }
    }
  }

  def changeRolle(meta: EventMetadata, personId: PersonId, rolle: Rolle)(implicit originator: PersonId = meta.originator) = {
    DB localTx { implicit session =>
      stammdatenWriteRepository.getById(personMapping, personId) map { person =>
        val updated = person.copy(rolle = Some(rolle))
        stammdatenWriteRepository.updateEntity[Person, PersonId](updated)
      }
    }
  }

  /**
   * @deprecated handling already persisted events. auslieferungAusgeliefert is now done in update service.
   */
  def auslieferungAusgeliefert(meta: EventMetadata, id: AuslieferungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(depotAuslieferungMapping, id) map { auslieferung =>
        if (Erfasst == auslieferung.status) {
          stammdatenWriteRepository.updateEntity[DepotAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
        }
      } orElse {
        stammdatenWriteRepository.getById(tourAuslieferungMapping, id) map { auslieferung =>
          if (Erfasst == auslieferung.status) {
            stammdatenWriteRepository.updateEntity[TourAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
          }
        }
      } orElse {
        stammdatenWriteRepository.getById(postAuslieferungMapping, id) map { auslieferung =>
          if (Erfasst == auslieferung.status) {
            stammdatenWriteRepository.updateEntity[PostAuslieferung, AuslieferungId](auslieferung.copy(status = Ausgeliefert))
          }
        }
      }
    }
  }

  def sammelbestellungAbgerechnet(meta: EventMetadata, datum: DateTime, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(sammelbestellungMapping, id) map { sammelbestellung =>
        if (Abgeschlossen == sammelbestellung.status) {
          stammdatenWriteRepository.updateEntity[Sammelbestellung, SammelbestellungId](sammelbestellung.copy(status = Verrechnet, datumAbrechnung = Some(datum)))
        }
      }
    }
  }
}
