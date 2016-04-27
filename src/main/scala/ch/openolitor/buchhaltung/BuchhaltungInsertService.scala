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

object BuchhaltungInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungInsertService = new DefaultBuchhaltungInsertService(sysConfig, system)
}

class DefaultBuchhaltungInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungInsertService(sysConfig) with DefaultBuchhaltungRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Buchhaltung Modul
 */
class BuchhaltungInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_, _]] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings {
  self: BuchhaltungRepositoryComponent =>

  val Divisor = 10
  val ReferneznummerLength = 26
  val BetragLength = 10
  val TeilnehmernummerLength = 9

  val belegarten = Map[Waehrung, String](CHF -> "01", EUR -> "21")

  // ESR checksum nach "Prüfzifferberechnung Modulo 10, rekursiv"
  val checkSumDefinition = List(0, 9, 4, 6, 8, 2, 7, 1, 3, 5)

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: RechnungId, entity: RechnungModify) =>
      createRechnung(meta, id, entity)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def createRechnung(meta: EventMetadata, id: RechnungId, entity: RechnungModify)(implicit userId: UserId = meta.originator) = {
    val referenzNummer = generateReferenzNummer(entity, id)
    val esrNummer = generateEsrNummer(entity, referenzNummer)

    val typ = copyTo[RechnungModify, Rechnung](entity,
      "id" -> id,
      "status" -> Erstellt,
      "referenzNummer" -> referenzNummer,
      "esrNummer" -> esrNummer,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Rechnung, RechnungId](typ)
    }
  }

  /**
   * Generieren einer Referenznummer, die die Kundennummer und Rechnungsnummer enthält.
   */
  def generateReferenzNummer(rechnung: RechnungModify, id: RechnungId): String = {
    // TODO use a configurable pattern
    val filled = ("%026d".format(0) + s"${rechnung.kundeId.id}${id.id}") takeRight (ReferneznummerLength)
    val checksum = calculateChecksum(filled.toList map (_.asDigit))

    s"$filled$checksum"
  }

  def generateEsrNummer(rechnung: RechnungModify, referenzNummer: String): String = {
    // TODO read teilnehmernummer from config
    val teilnehmernummer = "777777777"
    val bc = belegarten(rechnung.waehrung)
    val betrag = (s"%0${BetragLength}d".format(0) + s"${(rechnung.betrag * 100).toBigInt}") takeRight (BetragLength)
    val checksum = calculateChecksum((bc + betrag).toList map (_.asDigit))
    val filledTeilnehmernummer = (s"%0${TeilnehmernummerLength}d".format(0) + teilnehmernummer) takeRight (TeilnehmernummerLength)

    s"$bc$betrag$checksum>$referenzNummer+ $teilnehmernummer>"
  }

  def calculateChecksum(digits: List[Int], buffer: Int = 0): Int = digits match {
    case Nil =>
      (Divisor - buffer) % Divisor
    case d :: tail =>
      calculateChecksum(tail, checkSumDefinition((buffer + d) % Divisor))
  }
}