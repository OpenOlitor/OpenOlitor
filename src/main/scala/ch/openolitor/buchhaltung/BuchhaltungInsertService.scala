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
package ch.openolitor.buchhaltung

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.core.models._
import ch.openolitor.buchhaltung._
import ch.openolitor.buchhaltung.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.{ Waehrung, CHF, EUR }
import ch.openolitor.stammdaten.models.KontoDaten
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungWriteRepositoryComponent
import ch.openolitor.buchhaltung.repositories.BuchhaltungWriteRepositoryComponent
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher
import ch.openolitor.stammdaten.models.KundeId

object BuchhaltungInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungInsertService = new DefaultBuchhaltungInsertService(sysConfig, system)
}

class DefaultBuchhaltungInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungInsertService(sysConfig) with DefaultBuchhaltungWriteRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Buchhaltung Modul
 */
class BuchhaltungInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings {
  self: BuchhaltungWriteRepositoryComponent =>

  val Divisor = 10
  val ReferenznummerLength = 26
  val BetragLength = 10
  val TeilnehmernummerLength = 9

  lazy val config = sysConfig.mandantConfiguration.config
  lazy val RechnungIdLength = config.getIntOption(s"buchhaltung.rechnung-id-length").getOrElse(6)
  lazy val KundeIdLength = config.getIntOption(s"buchhaltung.kunde-id-length").getOrElse(6)

  val belegarten = Map[Waehrung, String](CHF -> "01", EUR -> "21")

  // ESR checksum nach "Prüfzifferberechnung Modulo 10, rekursiv"
  val checkSumDefinition = List(0, 9, 4, 6, 8, 2, 7, 1, 3, 5)

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: RechnungsPositionId, entity: RechnungsPositionCreate) =>
      createRechnungsPosition(meta, id, entity)
    case EntityInsertedEvent(meta, id: RechnungId, entity: RechnungCreateFromRechnungsPositionen) => createRechnung(meta, id, entity)
    case e =>
  }

  def createRechnungsPosition(meta: EventMetadata, id: RechnungsPositionId, entity: RechnungsPositionCreate)(implicit personId: PersonId = meta.originator): Option[RechnungsPosition] = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>

      val rp = copyTo[RechnungsPositionCreate, RechnungsPosition](
        entity,
        "id" -> id,
        "rechnungId" -> None,
        "parentRechnungsPositionId" -> None,
        "status" -> RechnungsPositionStatus.Offen,
        "sort" -> None,
        "erstelldat" -> meta.timestamp,
        "ersteller" -> meta.originator,
        "modifidat" -> meta.timestamp,
        "modifikator" -> meta.originator
      )

      buchhaltungWriteRepository.insertEntity[RechnungsPosition, RechnungsPositionId](rp)
    }
  }

  def createRechnung(meta: EventMetadata, id: RechnungId, entity: RechnungCreateFromRechnungsPositionen)(implicit personId: PersonId = meta.originator): Option[Rechnung] = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      buchhaltungWriteRepository.getKontoDaten flatMap { kontoDaten =>
        val referenzNummer = generateReferenzNummer(kontoDaten, entity.kundeId, id)
        val esrNummer = generateEsrNummer(kontoDaten, entity.betrag, entity.waehrung, referenzNummer)

        val typ = copyTo[RechnungCreateFromRechnungsPositionen, Rechnung](
          entity,
          "id" -> id,
          "einbezahlterBetrag" -> None,
          "status" -> Erstellt,
          "referenzNummer" -> referenzNummer,
          "fileStoreId" -> None,
          "anzahlMahnungen" -> 0.toInt,
          "mahnungFileStoreIds" -> Set.empty[String],
          "esrNummer" -> esrNummer,
          "erstelldat" -> meta.timestamp,
          "ersteller" -> meta.originator,
          "modifidat" -> meta.timestamp,
          "modifikator" -> meta.originator
        )
        buchhaltungWriteRepository.insertEntity[Rechnung, RechnungId](typ)
      }
    }
  }

  /**
   * Generieren einer Referenznummer, die die Kundennummer und Rechnungsnummer enthält.
   */
  def generateReferenzNummer(kontoDaten: KontoDaten, kundeId: KundeId, id: RechnungId): String = {
    val referenzNummerPrefix = kontoDaten.referenzNummerPrefix getOrElse ("")

    val filled = s"${referenzNummerPrefix}%0${ReferenznummerLength - referenzNummerPrefix.size - RechnungIdLength}d%0${RechnungIdLength}d".format(kundeId.id, id.id)
    val checksum = calculateChecksum(filled.toList map (_.asDigit))

    s"$filled$checksum"
  }

  def generateEsrNummer(kontoDaten: KontoDaten, betrag: BigDecimal, waehrung: Waehrung, referenzNummer: String): String = {
    val teilnehmerNummer = kontoDaten.teilnehmerNummer getOrElse ("")

    val bc = belegarten(waehrung)
    val zeroes = s"%0${BetragLength}d".format(0)
    val formated_betrag = (s"$zeroes${(betrag * 100).toBigInt}") takeRight (BetragLength)
    val checksum = calculateChecksum((bc + formated_betrag).toList map (_.asDigit))
    val filledTeilnehmernummer = (s"%0${TeilnehmernummerLength}d".format(0) + teilnehmerNummer) takeRight (TeilnehmernummerLength)

    s"$bc$formated_betrag$checksum>$referenzNummer+ $filledTeilnehmernummer>"
  }

  def calculateChecksum(digits: List[Int], buffer: Int = 0): Int = digits match {
    case Nil =>
      (Divisor - buffer) % Divisor
    case d :: tail =>
      calculateChecksum(tail, checkSumDefinition((buffer + d) % Divisor))
  }
}
