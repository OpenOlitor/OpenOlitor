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
package ch.openolitor.core.db.evolution.scripts.recalculations

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.stammdaten.repositories.StammdatenWriteRepositoryImpl
import ch.openolitor.core.NoPublishEventStream
import ch.openolitor.stammdaten.models.Abwesenheit
import scala.collection.immutable.TreeMap
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Boot
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts

object RecalculateAnzahlAbwesenheiten {
  val scripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts with StammdatenWriteRepositoryImpl with NoPublishEventStream {

    def buildAbwesenheitMap(projekt: Projekt, abwesenheiten: Seq[Abwesenheit]): TreeMap[String, Int] = {
      TreeMap(abwesenheiten.groupBy(a => projekt.geschaftsjahr.key(a.datum)).map {
        case (key, set) => (key, set.length)
      }.toSeq: _*)
    }

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      // recalculate abwesenheiten

      val abw = abwesenheitMapping.syntax("abw")
      implicit val personId = Boot.systemPersonId

      getProjekt map { projekt =>
        withSQL {
          select.from(abwesenheitMapping as abw)
        }.map(abwesenheitMapping(abw)).list.apply().groupBy(_.aboId) map {
          case (aboId, abwesenheiten) =>
            val abwMap = buildAbwesenheitMap(projekt, abwesenheiten)
            getAboDetail(aboId) map {
              case a: DepotlieferungAboDetail =>
                val copy = a.copy(anzahlAbwesenheiten = abwMap)
                updateEntity[DepotlieferungAbo, AboId](copyTo[DepotlieferungAboDetail, DepotlieferungAbo](copy))
              case a: HeimlieferungAboDetail =>
                val copy = a.copy(anzahlAbwesenheiten = abwMap)
                updateEntity[HeimlieferungAbo, AboId](copyTo[HeimlieferungAboDetail, HeimlieferungAbo](copy))
              case a: PostlieferungAboDetail =>
                val copy = a.copy(anzahlAbwesenheiten = abwMap)
                updateEntity[PostlieferungAbo, AboId](copyTo[PostlieferungAboDetail, PostlieferungAbo](copy))
            }
        }
      }

      Success(true)
    }
  }
}
