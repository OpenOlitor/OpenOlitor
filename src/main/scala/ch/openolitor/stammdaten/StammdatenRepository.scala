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

import ch.openolitor.core.models._
import java.util.UUID
import scalikejdbc._
import scalikejdbc.async._
import scalikejdbc.async.FutureImplicits._
import scala.concurrent.ExecutionContext
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories.BaseWriteRepository
import scala.concurrent._
import akka.event.Logging
import ch.openolitor.stammdaten.dto._
import com.typesafe.scalalogging.LazyLogging
import scala.collection.SortedSet

trait StammdatenReadRepository {
  def getAbotyp(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]]
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]]
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository with LazyLogging {
  lazy val aboTyp = Abotyp.syntax("t")
  lazy val pl = Postlieferung.syntax("pl")
  lazy val dl = Depotlieferung.syntax("dl")
  lazy val d = Depot.syntax("d")
  lazy val t = Tour.syntax("tr")
  lazy val hl = Heimlieferung.syntax("hl")

  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]] = {
    withSQL {
      select
        .from(Abotyp as aboTyp)
        .where.append(aboTyp.aktiv)
        .orderBy(aboTyp.name)
    }.map(Abotyp(aboTyp)).list.future
  }

  override def getAbotyp(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]] = {
    withSQL {
      select
        .from(Abotyp as aboTyp)
        .leftJoin(Postlieferung as pl).on(aboTyp.id, pl.abotypId)
        .leftJoin(Heimlieferung as hl).on(aboTyp.id, hl.abotypId)
        .leftJoin(Depotlieferung as dl).on(aboTyp.id, dl.abotypId)
        .leftJoin(Depot as d).on(dl.depotId, d.id)
        .leftJoin(Tour as t).on(hl.tourId, t.id)
        .where.eq(aboTyp.id, id.id.toString)
    }.one(Abotyp(aboTyp))
      .toManies(
        rs => Postlieferung.opt(pl)(rs),
        rs => Heimlieferung.opt(hl)(rs),
        rs => Depotlieferung.opt(dl)(rs),
        rs => Depot.opt(d)(rs),
        rs => Tour.opt(t)(rs))
      .map({ (abotyp, pls, hms, dls, depot, tour) =>
        val vertriebsarten =
          pls.map(pl => PostlieferungDetail(pl.liefertage)) ++
            hms.map(hm => HeimlieferungDetail(tour.head, hm.liefertage)) ++
            dls.map(dl => DepotlieferungDetail(depot.head, dl.liefertage))
        logger.debug(s"getAbottyp:$id, abotyp:$abotyp:$vertriebsarten")
        AbotypDetail(abotyp.id,
          abotyp.name,
          abotyp.beschreibung,
          abotyp.lieferrhythmus,
          abotyp.enddatum,
          abotyp.anzahlLieferungen,
          abotyp.anzahlAbwesenheiten,
          abotyp.preis,
          abotyp.preiseinheit,
          abotyp.aktiv,
          vertriebsarten.toSet,
          abotyp.anzahlAbonnenten,
          abotyp.letzteLieferung)
      })
      .single.future
  }
}

trait StammdatenWriteRepository extends BaseWriteRepository {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)
}

class StammdatenWriteRepositoryImpl extends StammdatenWriteRepository with LazyLogging {
  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {

    //drop all tables
    DB autoCommit { implicit session =>
      logger.debug(s"oo-system: cleanupDatabase - drop tables")

      sql"drop table if exists ${Postlieferung.table}".execute.apply()
      sql"drop table if exists ${Depotlieferung.table}".execute.apply()
      sql"drop table if exists ${Heimlieferung.table}".execute.apply()
      sql"drop table if exists ${Depot.table}".execute.apply()
      sql"drop table if exists ${Tour.table}".execute.apply()
      sql"drop table if exists ${Abotyp.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables")
      //create tables

      sql"create table ${Postlieferung.table}  (id varchar(36) not null, abotyp_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depotlieferung.table} (id varchar(36) not null, abotyp_id int not null, depot_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Heimlieferung.table} (id varchar(36) not null, abotyp_id int not null, tour_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depot.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Tour.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Abotyp.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), enddatum timestamp, anzahl_lieferungen int, anzahl_abwesenheiten int, preis NUMERIC not null, preiseinheit varchar(20) not null, aktiv bit, anzahl_abonnenten INT not null, letzte_lieferung timestamp, waehrung varchar(10))".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end")
    }
  }

  def insertEntity(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession) = {
    entity match {
      case abotyp: Abotyp =>
        logger.debug(s"create Abotyp values:${abotyp.productIterator.toSeq.mkString(",")}")
        withSQL(insertInto(Abotyp).values(parameters(abotyp)(Map()): _*)).update.apply()
    }
  }

  def updateEntity(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession) = {
    entity match {
      case abotyp: Abotyp => withSQL(update(Abotyp).set(Abotyp.column.name -> abotyp.name).where.eq(Abotyp.column.id, abotyp.id)).update.apply()
    }
  }

  def deleteEntity(id: BaseId)(implicit session: DBSession) = {
    id match {
      case abotypId: AbotypId =>
        logger.debug(s"delete from abotypen:$id")
        withSQL(deleteFrom(Abotyp).where.eq(Abotyp.column.id, abotypId.id.toString)).update.apply()
      case x =>
        logger.warn(s"Can't delete requested  entity:$x")
    }
  }
}