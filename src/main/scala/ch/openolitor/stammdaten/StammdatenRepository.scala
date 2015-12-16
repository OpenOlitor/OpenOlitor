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
import ch.openolitor.core.Macros._

trait StammdatenReadRepository {
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]]
  def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]]

  def getPersonen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]
  def getPersonDetail(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]]
  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]]

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]]
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository with LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val pl = postlieferungMapping.syntax("pl")
  lazy val dl = depotlieferungMapping.syntax("dl")
  lazy val depot = depotMapping.syntax("d")
  lazy val t = tourMapping.syntax("tr")
  lazy val hl = heimlieferungMapping.syntax("hl")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")

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
        .where.eq(person.id, parameter(id))
    }.map(personMapping(person)).single.future
  }

  override def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]] = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .leftJoin(postlieferungMapping as pl).on(aboTyp.id, pl.abotypId)
        .leftJoin(heimlieferungMapping as hl).on(aboTyp.id, hl.abotypId)
        .leftJoin(depotlieferungMapping as dl).on(aboTyp.id, dl.abotypId)
        .leftJoin(depotMapping as depot).on(dl.depotId, depot.id)
        .leftJoin(tourMapping as t).on(hl.tourId, t.id)
        .where.eq(aboTyp.id, parameter(id))
    }.one(abotypMapping(aboTyp))
      .toManies(
        rs => postlieferungMapping.opt(pl)(rs),
        rs => heimlieferungMapping.opt(hl)(rs),
        rs => depotlieferungMapping.opt(dl)(rs),
        rs => depotMapping.opt(depot)(rs),
        rs => tourMapping.opt(t)(rs))
      .map({ (abotyp, pls, hms, dls, depot, tour) =>
        val vertriebsarten =
          pls.map(pl => PostlieferungDetail(pl.liefertage)) ++
            hms.map(hm => HeimlieferungDetail(tour.head, hm.liefertage)) ++
            dls.map(dl => DepotlieferungDetail(DepotSummary(depot.head.id, depot.head.name), dl.liefertage))
        logger.debug(s"getAbottyp:$id, abotyp:$abotyp:$vertriebsarten")

        //must be cast to vertriebsartdetail without serializable extension
        val set: Set[Vertriebsartdetail] = vertriebsarten.toSet
        copyTo[Abotyp, AbotypDetail](abotyp, "vertriebsarten" -> set)
      })
      .single.future
  }

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]] = {
    withSQL {
      select
        .from(depotMapping as depot)
        .orderBy(depot.name)
    }.map(depotMapping(depot)).list.future
  }

  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]] = {
    withSQL {
      select
        .from(depotMapping as depot)
        .where.eq(depot.id, parameter(id))
    }.map(depotMapping(depot)).single.future
  }

  def getDepotlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungAbo]] = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list.future
  }

  def getHeimlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungAbo]] = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list.future
  }

  def getPostlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungAbo]] = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
    }.map(postlieferungAboMapping(postlieferungAbo)).list.future
  }

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]] = {
    for {
      d <- getDepotlieferungAbos
      h <- getHeimlieferungAbos
      p <- getPostlieferungAbos
    } yield d ::: h ::: p
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
      sql"drop table if exists ${depotlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${postlieferungAboMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables")
      //create tables

      sql"create table ${postlieferungMapping.table}  (id varchar(36) not null, abotyp_id varchar(36) not null, liefertage varchar(256))".execute.apply()
      sql"create table ${depotlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, depot_id varchar(36) not null, liefertage varchar(256))".execute.apply()
      sql"create table ${heimlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, tour_id varchar(36) not null, liefertage varchar(256))".execute.apply()
      sql"create table ${depotMapping.table} (id varchar(36) not null, name varchar(50) not null, ap_name varchar(50), ap_vorname varchar(50), ap_telefon varchar(20), ap_email varchar(100), v_name varchar(50), v_vorname varchar(50), v_telefon varchar(20), v_email varchar(100), strasse varchar(50), haus_nummer varchar(10), plz varchar(4) not null, ort varchar(50) not null, aktiv bit, oeffnungszeiten varchar(200), iban varchar(30), bank varchar(50), beschreibung varchar(200), anzahl_abonnenten int, anzahl_abonnenten_max int)".execute.apply()
      sql"create table ${tourMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${abotypMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), enddatum timestamp, anzahl_lieferungen int, anzahl_abwesenheiten int, preis NUMERIC not null, preiseinheit varchar(20) not null, aktiv bit, anzahl_abonnenten INT not null, letzte_lieferung timestamp, waehrung varchar(10))".execute.apply()
      sql"create table ${personMapping.table} (id varchar(36) not null, name varchar(50) not null, vorname varchar(50) not null, strasse varchar(50) not null, haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4) not null, ort varchar(50) not null, email varchar(100) not null, email_alternative varchar(100), telefon varchar(50), telefon_alternative varchar(50), bemerkungen varchar(512), typen varchar(200))".execute.apply()
      sql"create table ${depotlieferungAboMapping.table}  (id varchar(36) not null,person_id varchar(36) not null, person_name varchar(50), person_vorname varchar(50), abotyp_id varchar(36) not null, abotyp_name varchar(50), depot_id varchar(36), depot_name varchar(50), lieferzeitpunkt varchar(10))".execute.apply()
      sql"create table ${heimlieferungAboMapping.table}  (id varchar(36) not null,person_id varchar(36) not null, person_name varchar(50), person_vorname varchar(50), abotyp_id varchar(36) not null, abotyp_name varchar(50), tour_id varchar(36), tour_name varchar(50), lieferzeitpunkt varchar(10))".execute.apply()
      sql"create table ${postlieferungAboMapping.table}  (id varchar(36) not null,person_id varchar(36) not null, person_name varchar(50), person_vorname varchar(50), abotyp_id varchar(36) not null, abotyp_name varchar(50), lieferzeitpunkt varchar(10))".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end")
    }
  }
}
