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
package ch.openolitor.core.db.evolution.scripts

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.core.repositories.BaseWriteRepository
import scala.collection.immutable.TreeMap
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Boot
import ch.openolitor.core.NoPublishEventStream

object OO314_DBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with BaseWriteRepository with DefaultDBScripts with NoPublishEventStream {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"add columns durchschnittspreis and anzahlLieferungen to vertrieb...")
      alterTableAddColumnIfNotExists(vertriebMapping, "durchschnittspreis", """VARCHAR(1000) not null default """"", "anzahl_abos")
      alterTableAddColumnIfNotExists(vertriebMapping, "anzahl_lieferungen", """VARCHAR(1000) not null default """"", "durchschnittspreis")

      logger.debug(s"Calculate preisTotal on abgeschlossenen lieferplanungen")
      sql"""update ${lieferungMapping.table} l 
      	INNER JOIN (SELECT p.lieferung_id, sum(p.preis) total from ${lieferpositionMapping.table} p group by p.lieferung_id) p ON l.id=p.lieferung_id
      	INNER JOIN ${lieferplanungMapping.table} lp ON l.lieferplanung_id=lp.id
      	set l.preis_total=p.total
      	where lp.status='Abgeschlossen'""".execute.apply()

      logger.debug(s"Calculate durchschnittspreis and anzahl_lieferungen on vertriebe")
      lazy val v = vertriebMapping.syntax("vertrieb")
      lazy val l = lieferungMapping.syntax("l")
      lazy val lp = lieferplanungMapping.syntax("lp")
      lazy val p = projektMapping.syntax("p")
      implicit val personId = Boot.systemPersonId
      withSQL { select.from(vertriebMapping as v) }.map(vertriebMapping(v)).list.apply() map { vertrieb =>
        //get call abeschlossenen lieferplanungen

        withSQL { select.from(projektMapping as p) }.map(projektMapping(p)).single.apply() map { projekt =>

          //calculate durchschnittspreise
          val lieferungenByGj = withSQL {
            select.from(lieferungMapping as l).
              join(lieferplanungMapping as lp).on(l.lieferplanungId, lp.id).
              where.eq(lp.status, parameter(Abgeschlossen)).and.eq(l.vertriebId, parameter(vertrieb.id))
          }.
            map(lieferungMapping(l)).list.apply() map { lieferung =>

              val gjKey = projekt.geschaftsjahr.key(lieferung.datum.toLocalDate)
              (gjKey, lieferung)
            } groupBy (_._1)

          val anzahlLieferungenMap = TreeMap(lieferungenByGj.map { case (gjKey, preise) => (gjKey, preise.size) }.toSeq: _*)
          val durchschnittspreisMap = TreeMap(lieferungenByGj.map { case (gjKey, preise) => (gjKey, preise.map(_._2.preisTotal).sum / preise.size) }.toSeq: _*)

          val copy = vertrieb.copy(anzahlLieferungen = anzahlLieferungenMap, durchschnittspreis = durchschnittspreisMap)
          updateEntity[Vertrieb, VertriebId](copy)

          //update lieferungen
          withSQL { select.from(lieferungMapping as l).where.eq(l.vertriebId, parameter(vertrieb.id)) }.map(lieferungMapping(l)).list.apply() map { lieferung =>
            val gjKey = projekt.geschaftsjahr.key(lieferung.datum.toLocalDate)
            val anzahl = anzahlLieferungenMap.get(gjKey).getOrElse(0)
            val dpreis: BigDecimal = durchschnittspreisMap.get(gjKey).getOrElse(0)

            val lieferungCopy = lieferung.copy(anzahlLieferungen = anzahl, durchschnittspreis = dpreis)
            updateEntity[Lieferung, LieferungId](lieferungCopy)
          }
        }

      }

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}
