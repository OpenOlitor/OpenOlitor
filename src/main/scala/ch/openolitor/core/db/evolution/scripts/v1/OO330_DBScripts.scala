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
package ch.openolitor.core.db.evolution.scripts.v1

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.stammdaten.StammdatenInsertService
import ch.openolitor.core.Boot
import ch.openolitor.stammdaten.repositories._
import ch.openolitor.core.NoPublishEventStream
import ch.openolitor.core.db.evolution.scripts.recalculations.RecalulateLieferungCounter
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts

object OO330_DBScripts {

  trait ScriptStammdatenWriteRepositoryComponent extends StammdatenWriteRepositoryComponent {

    override val stammdatenWriteRepository: StammdatenWriteRepository = new StammdatenWriteRepositoryImpl with NoPublishEventStream
  }

  class ScriptStammdatenInsertService(sysConfig: SystemConfig)
    extends StammdatenInsertService(sysConfig) with ScriptStammdatenWriteRepositoryComponent

  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts with NoPublishEventStream {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //create missing koerbe due to older releases
      implicit val personId = Boot.systemPersonId
      lazy val lieferung = lieferungMapping.syntax("lieferung")
      lazy val korb = korbMapping.syntax("korb")

      val insertService = new ScriptStammdatenInsertService(sysConfig)

      withSQL {
        select.from(lieferungMapping as lieferung).
          where.not.eq(lieferung.lieferplanungId, None).and.notIn(lieferung.id, (select(korb.lieferungId).from(korbMapping as korb)))
      }.map(lieferungMapping(lieferung)).list.apply() map { lieferung =>
        insertService.createKoerbe(lieferung)
      }

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts, RecalulateLieferungCounter.scripts)
}