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

trait StammdatenReadRepository {
  def getAbotyp(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AbotypDetail]]
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]]
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository {
  lazy val aboTyp = Abotyp.syntax("t")
  lazy val pl = Postlieferung.syntax("pl")
  lazy val dl = Depotlieferung.syntax("dl")
  lazy val d = Depot.syntax("d")
  lazy val t = Tour.syntax("t")
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
    }.one(Abotyp(aboTyp))
      .toManies(
        rs => Postlieferung.opt(pl)(rs),
        rs => Heimlieferung.opt(hl)(rs),
        rs => Depotlieferung.opt(dl)(rs),
        rs => Depot.opt(d)(rs),
        rs => Tour.opt(t)(rs))
      .map({ (abotyp, pls, hms, dls, depot, tour) =>
        val vertriebsarten =
          pls.map(pl => PostlieferungDetail(pl.id, pl.liefertage)) ++
            hms.map(hm => HeimlieferungDetail(hm.id, tour.head, hm.liefertage)) ++
            dls.map(dl => DepotlieferungDetail(dl.id, depot.head, dl.liefertage))

        AbotypDetail(abotyp.id,
          abotyp.name,
          abotyp.beschreibung,
          abotyp.lieferrhythmus,
          abotyp.enddatum,
          abotyp.anzahlLieferungen,
          abotyp.anzahlAbwesenheiten,
          abotyp.preis,
          abotyp.preisEinheit,
          abotyp.aktiv,
          vertriebsarten,
          abotyp.anzahlAbonnenten,
          abotyp.letzteLieferung)
      })
      .single.future
  }
}

trait StammdatenWriteRepository extends BaseWriteRepository {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)

  def insert(abotyp: Abotyp)(implicit session: DBSession)

  def delete(id: AbotypId)(implicit session: DBSession)

  def updateAbotyp(abotyp: Abotyp)(implicit session: DBSession)
}

class StammdatenWriteRepositoryImpl extends StammdatenWriteRepository {
  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {

    println(s"oo-system: cleanupDatabase - drop tables")

    //drop all tables
    DB autoCommit { implicit session =>
      sql"drop table if exists ${Postlieferung.table}".execute.apply()
      sql"drop table if exists ${Depotlieferung.table}".execute.apply()
      sql"drop table if exists ${Heimlieferung.table}".execute.apply()
      sql"drop table if exists ${Depot.table}".execute.apply()
      sql"drop table if exists ${Tour.table}".execute.apply()
      sql"drop table if exists ${Abotyp.table}".execute.apply()
    }

    println(s"oo-system: cleanupDatabase - create tables")
    //create tables
    DB autoCommit { implicit session =>
      sql"create table ${Postlieferung.table}  (id varchar(36) not null, abo_typ_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depotlieferung.table} (id varchar(36) not null, abo_typ_id int not null, depot_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Heimlieferung.table} (id varchar(36) not null, abo_typ_id int not null, tour_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depot.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Tour.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Abotyp.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), enddatum timestamp, anzahl_lieferungen int, anzahl_abwesenheiten int, preis NUMERIC not null, preisEinheit varchar(20) not null, aktiv bit)".execute.apply()
    }

    println(s"oo-system: cleanupDatabase - end")
  }

  def insert(abotyp: Abotyp)(implicit session: DBSession) = {
    withSQL(insertInto(Abotyp).values(abotyp)).update.apply()
  }

  def delete(id: AbotypId)(implicit session: DBSession) = {
    withSQL(deleteFrom(Abotyp).where.eq(Abotyp.column.id, id)).update.apply()
  }

  def updateAbotyp(abotyp: Abotyp)(implicit session: DBSession) = {
    withSQL(update(Abotyp).set(Abotyp.column.name -> abotyp.name).where.eq(Abotyp.column.id, abotyp.id.get)).update.apply()
  }

  /*override def insert(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession) = {
    withSQL {
      insert(entity).apply()
    }
  }

  override def delete[E <: BaseEntity[I], I >: BaseId](id: I, entity: BaseEntitySQLSyntaxSupport[_])(implicit session: DBSession) = {
    val col = Abotyp.column.id
    val f2 = withSQL(deleteFrom(Abotyp).where(Abotyp.column.id, id))
    val f = deleteFrom(entity).where(Abotyp.column.id, id)
    withSQL(deleteFrom(entity).where(Abotyp.column.id, id)).update.apply()
    id match {
      case id: AbotypId =>
        DB autoCommit { implicit session =>
          withSQL {
            deleteFrom(Abotyp).where.eq(Abotyp.column.id, id)
          }.update.apply()
        }
    }
  }

  override def update(entity: BaseEntity[_ <: BaseId])(implicit cpContext: ConnectionPoolContext) = {
    //TODO: implement using entity match
  }*/

}