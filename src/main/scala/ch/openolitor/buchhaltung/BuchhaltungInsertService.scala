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

object BuchhaltungInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): BuchhaltungInsertService = new DefaultBuchhaltungInsertService(sysConfig, system)
}

class DefaultBuchhaltungInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
    extends BuchhaltungInsertService(sysConfig) with DefaultBuchhaltungRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Buchhaltung Modul
 */
class BuchhaltungInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_]] with LazyLogging with AsyncConnectionPoolContextAware
    with BuchhaltungDBMappings {
  self: BuchhaltungRepositoryComponent =>

  val ZERO = 0

  val handle: Handle = {
    case EntityInsertedEvent(meta, id, entity: RechnungModify) =>
      createRechnung(meta, id, entity)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def createRechnung(meta: EventMetadata, id: UUID, entity: RechnungModify)(implicit userId: UserId = meta.originator) = {
    val entityId = RechnungId(id)
    val referenzNummer = generateReferenzNummer(entity)
    val esrNummer = generateEsrNummer(entity, referenzNummer)
    val typ = copyTo[RechnungModify, Rechnung](entity,
      "id" -> entityId,
      "status" -> Erstellt,
      "referenzNummer" -> referenzNummer,
      "esrNummer" -> esrNummer,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity[Rechnung, RechnungId](typ)
    }
  }

  def generateReferenzNummer(rechnung: RechnungModify): String = {
    ???
  }

  def generateEsrNummer(rechnung: RechnungModify, esrNummer: String): String = {
    ???
  }
}