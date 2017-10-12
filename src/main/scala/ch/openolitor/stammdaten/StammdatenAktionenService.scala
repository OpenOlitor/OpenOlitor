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
import BigDecimal.RoundingMode._
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

object StammdatenAktionenService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem, mailService: ActorRef): StammdatenAktionenService = new DefaultStammdatenAktionenService(sysConfig, system, mailService)
}

class DefaultStammdatenAktionenService(sysConfig: SystemConfig, override val system: ActorSystem, override val mailService: ActorRef)
    extends StammdatenAktionenService(sysConfig, mailService) with DefaultStammdatenWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Aktionen für das Stammdaten Modul
 */
class StammdatenAktionenService(override val sysConfig: SystemConfig, override val mailService: ActorRef) extends EventService[PersistentEvent]
    with LazyLogging
    with AsyncConnectionPoolContextAware
    with StammdatenDBMappings
    with MailServiceReference
    with StammdatenEventStoreSerializer
    with SammelbestellungenHandler
    with LieferungHandler {
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
    case LieferplanungDataModifiedEvent(meta, result: LieferplanungDataModify) =>
      lieferplanungDataModified(meta, result)
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Lieferplanung, LieferplanungId](Offen == _.status)(id)(lieferplanungMapping.column.status -> Abgeschlossen)
    }
  }

  def lieferplanungVerrechnet(meta: EventMetadata, id: LieferplanungId)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Lieferplanung, LieferplanungId](Abgeschlossen == _.status)(id) {
        lieferplanungMapping.column.status -> Verrechnet
      }

      stammdatenWriteRepository.getLieferungen(id) map { lieferung =>
        stammdatenWriteRepository.updateEntityIf[Lieferung, LieferungId](Abgeschlossen == _.status)(lieferung.id) {
          lieferungMapping.column.status -> Verrechnet
        }
      }
      stammdatenWriteRepository.getSammelbestellungen(id) map { sammelbestellung =>
        stammdatenWriteRepository.updateEntityIf[Sammelbestellung, SammelbestellungId](Abgeschlossen == _.status)(sammelbestellung.id)(
          sammelbestellungMapping.column.status -> Verrechnet,
          sammelbestellungMapping.column.datumAbrechnung -> Option(DateTime.now)
        )

      }
    }
  }

  def lieferplanungDataModified(meta: EventMetadata, result: LieferplanungDataModify)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getById(lieferplanungMapping, result.id) map { lieferplanung =>
        if (Offen == lieferplanung.status || Abgeschlossen == lieferplanung.status) {
          // lieferungen mit positionen anpassen
          result.lieferungen map { l =>
            recreateLieferpositionen(meta, l.id, l.lieferpositionen)
          }

          // existierende Sammelbestellungen neu ausrechnen
          stammdatenWriteRepository.getSammelbestellungen(result.id) map { s =>
            createOrUpdateSammelbestellungen(s.id, SammelbestellungModify(s.produzentId, s.lieferplanungId, s.datum))
          }

          // neue sammelbestellungen erstellen
          result.newSammelbestellungen map { s =>
            createOrUpdateSammelbestellungen(s.id, SammelbestellungModify(s.produzentId, s.lieferplanungId, s.datum))
          }
        }
      }
    }
  }

  def sammelbestellungVersenden(meta: EventMetadata, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    val format = DateTimeFormat.forPattern("dd.MM.yyyy")

    DB localTxPostPublish { implicit session => implicit publisher =>
      //send mails to Produzenten
      stammdatenWriteRepository.getProjekt map { projekt =>
        stammdatenWriteRepository.getById(sammelbestellungMapping, id) map {
          //send mails only if current event timestamp is past the timestamp of last delivered mail
          case sammelbestellung if (sammelbestellung.datumVersendet.isEmpty || sammelbestellung.datumVersendet.get.isBefore(meta.timestamp)) =>
            stammdatenWriteRepository.getProduzentDetail(sammelbestellung.produzentId) map { produzent =>

              val bestellungen = stammdatenWriteRepository.getBestellungen(sammelbestellung.id) map { bestellung =>

                val bestellpositionen = stammdatenWriteRepository.getBestellpositionen(bestellung.id) map {
                  bestellposition =>
                    val preisPos = (bestellposition.preisEinheit.getOrElse(0: BigDecimal) * bestellposition.menge).setScale(2, HALF_UP)
                    val mengeTotal = bestellposition.anzahl * bestellposition.menge
                    val detail = if (bestellposition.preisEinheit.getOrElse(0: BigDecimal).compare(preisPos) == 0) "" else s""" ≙ ${preisPos}"""
                    s"""${bestellposition.produktBeschrieb}: ${bestellposition.anzahl} x ${bestellposition.menge} ${bestellposition.einheit} à ${bestellposition.preisEinheit.getOrElse("")}${detail} = ${bestellposition.preis.getOrElse("")} ${projekt.waehrung} ⇒ ${mengeTotal} ${bestellposition.einheit}"""
                }

                val infoAdminproz = bestellung.adminProzente match {
                  case x if x == 0 => ""
                  case _ => s"""Adminprozente: ${bestellung.adminProzente}%:"""
                }

                s"""${infoAdminproz}

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
                //ok
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
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](id)(personMapping.column.passwort -> Option(pwd))

      einladungId map { id =>
        stammdatenWriteRepository.updateEntity[Einladung, EinladungId](id)(einladungMapping.column.expires -> new DateTime())
      }
    }
  }

  def disableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.loginAktiv -> false)
    }
  }

  def enableLogin(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      setLoginAktiv(meta, personId)
    }
  }

  def setLoginAktiv(meta: EventMetadata, personId: PersonId)(implicit originator: PersonId = meta.originator, session: DBSession, publisher: EventPublisher) = {
    stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.loginAktiv -> true)
  }

  def sendPasswortReset(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, "Sie können Ihr Passwort mit folgendem Link neu setzten:", BasePasswortResetLink)
  }

  def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate)(implicit originator: PersonId = meta.originator): Unit = {
    sendEinladung(meta, einladungCreate, "Aktivieren Sie Ihren Zugang mit folgendem Link:", BaseZugangLink)
  }

  def sendEinladung(meta: EventMetadata, einladungCreate: EinladungCreate, baseText: String, baseLink: String)(implicit originator: PersonId): Unit = {
    DB localTxPostPublish { implicit session => implicit publisher =>
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

        if (einladung.erstelldat.isAfter(new DateTime(2017, 3, 2, 12, 0)) && (einladung.datumVersendet.isEmpty || einladung.datumVersendet.get.isBefore(meta.timestamp)) && einladung.expires.isAfter(DateTime.now)) {
          setLoginAktiv(meta, einladung.personId)

          val text = s"""
	        ${person.vorname} ${person.name},

	        ${baseText} ${baseLink}?token=${einladung.uid}

	        """

          // email wurde bereits im CommandHandler überprüft
          val mail = Mail(1, person.email.get, None, None, "OpenOlitor Zugang", text)

          mailService ? SendMailCommandWithCallback(originator, mail, Some(5 minutes), einladung.id) map {
            case _: SendMailEvent =>
            //ok
            case other =>
              logger.debug(s"Sending Mail failed resulting in $other")
          }
        } else {
          logger.debug(s"Don't send Einladung, has been send earlier: ${einladungCreate.id}")
        }
      }
    }
  }

  def changeRolle(meta: EventMetadata, personId: PersonId, rolle: Rolle)(implicit originator: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntity[Person, PersonId](personId)(personMapping.column.rolle -> Option(rolle))
    }
  }

  /**
   * @deprecated handling already persisted events. auslieferungAusgeliefert is now done in update service.
   */
  def auslieferungAusgeliefert(meta: EventMetadata, id: AuslieferungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[DepotAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        depotAuslieferungMapping.column.status -> Ausgeliefert
      } orElse stammdatenWriteRepository.updateEntityIf[TourAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        tourAuslieferungMapping.column.status -> Ausgeliefert
      } orElse stammdatenWriteRepository.updateEntityIf[PostAuslieferung, AuslieferungId](Erfasst == _.status)(id) {
        tourAuslieferungMapping.column.status -> Ausgeliefert
      }
    }
  }

  def sammelbestellungAbgerechnet(meta: EventMetadata, datum: DateTime, id: SammelbestellungId)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.updateEntityIf[Sammelbestellung, SammelbestellungId](Abgeschlossen == _.status)(id)(
        sammelbestellungMapping.column.status -> Verrechnet,
        sammelbestellungMapping.column.datumAbrechnung -> Option(datum)
      )

    }
  }
}
