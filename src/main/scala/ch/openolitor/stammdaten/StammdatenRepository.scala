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
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.core.repositories.BaseWriteRepository
import scala.concurrent._
import akka.event.Logging
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.EventStream
import ch.openolitor.core.Boot
import akka.actor.ActorSystem
import ch.openolitor.stammdaten.models._

trait StammdatenReadRepository {
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]]
  def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]]

  def getPersonen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]
  def getPersonDetail(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository with LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val pl = postlieferungMapping.syntax("pl")
  lazy val dl = depotlieferungMapping.syntax("dl")
  lazy val d = depotMapping.syntax("d")
  lazy val t = tourMapping.syntax("tr")
  lazy val hl = heimlieferungMapping.syntax("hl")

  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]] = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.aktiv, true)
        .orderBy(aboTyp.name)
    }.map(abotypMapping(aboTyp)).list.future
  }

  def getPersonen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]] = {
    withSQL {
      select
        .from(personMapping as person)
        .orderBy(person.name)
    }.map(personMapping(person)).list.future
  }

  def getPersonDetail(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]] = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.id, id)
    }.map(personMapping(person)).single.future
  }

  override def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]] = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .leftJoin(postlieferungMapping as pl).on(aboTyp.id, pl.abotypId)
        .leftJoin(heimlieferungMapping as hl).on(aboTyp.id, hl.abotypId)
        .leftJoin(depotlieferungMapping as dl).on(aboTyp.id, dl.abotypId)
        .leftJoin(depotMapping as d).on(dl.depotId, d.id)
        .leftJoin(tourMapping as t).on(hl.tourId, t.id)
        .where.eq(aboTyp.id, parameter(id))
    }.one(abotypMapping(aboTyp))
      .toManies(
        rs => postlieferungMapping.opt(pl)(rs),
        rs => heimlieferungMapping.opt(hl)(rs),
        rs => depotlieferungMapping.opt(dl)(rs),
        rs => depotMapping.opt(d)(rs),
        rs => tourMapping.opt(t)(rs))
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

class StammdatenWriteRepositoryImpl(val system: ActorSystem) extends StammdatenWriteRepository with LazyLogging with EventStream with StammdatenDBMappings {

  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {

    //drop all tables
    DB autoCommit { implicit session =>
      logger.debug(s"oo-system: cleanupDatabase - drop tables")

      sql"drop table if exists ${postlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotMapping.table}".execute.apply()
      sql"drop table if exists ${tourMapping.table}".execute.apply()
      sql"drop table if exists ${abotypMapping.table}".execute.apply()
      sql"drop table if exists ${personMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables")
      //create tables

      sql"create table ${postlieferungMapping.table}  (id varchar(36) not null, abotyp_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${depotlieferungMapping.table} (id varchar(36) not null, abotyp_id int not null, depot_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${heimlieferungMapping.table} (id varchar(36) not null, abotyp_id int not null, tour_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${depotMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${tourMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${abotypMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), enddatum timestamp, anzahl_lieferungen int, anzahl_abwesenheiten int, preis NUMERIC not null, preiseinheit varchar(20) not null, aktiv bit, anzahl_abonnenten INT not null, letzte_lieferung timestamp, waehrung varchar(10))".execute.apply()
      sql"create table ${personMapping.table} (id varchar(36) not null, name varchar(50) not null, vorname varchar(50) not null, strasse varchar(50) not null, haus_nummer varchar(10), plz int not null, ort varchar(50) not null, typen varchar(200))".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end")
    }
  }

  def deleteEntity(id: BaseId)(implicit session: DBSession) = {
    id match {
      case abotypId: AbotypId =>
        logger.debug(s"delete from abotypen:$id")
        getById(abotypMapping, abotypId) map { abotyp =>
          withSQL(deleteFrom(abotypMapping).where.eq(abotypMapping.column.id, parameter(abotypId))).update.apply()

          //publish event to stream
          //TODO: fetch real user when security gets integrated 
          publish(EntityDeleted(Boot.systemUserId, abotyp))
        }

      case x =>
        logger.warn(s"Can't delete requested  entity:$x")
    }
  }
}
