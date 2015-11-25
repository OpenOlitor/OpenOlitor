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

import ch.openolitor.core.repositories.StammdatenWriteRepository
import ch.openolitor.core.models._
import java.util.UUID
import scalikejdbc._

trait StammdatenWriteRepositoryImpl extends StammdatenWriteRepository {
  override def cleanupDatabase()(implicit session: DBSession = AutoSession) = {

    //drop all tables
    DB autoCommit { implicit session =>
      sql"drop table ${Postlieferung.table}".execute.apply()
      sql"drop table ${Depotlieferung.table}".execute.apply()
      sql"drop table ${Heimlieferung.table}".execute.apply()
      sql"drop table ${Depot.table}".execute.apply()
      sql"drop table ${Tour.table}".execute.apply()
      sql"drop table ${Abotyp.table}".execute.apply()
    }

    //create tables
    DB autoCommit { implicit session =>
      sql"create table ${Postlieferung.table}  (id int not null, abo_typ_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depotlieferung.table} (id int not null, abo_typ_id int not null, depot_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Heimlieferung.table} (id int not null, abo_typ_id int not null, tour_id int not null, liefertage varchar(256))".execute.apply()
      sql"create table ${Depot.table} (id int not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Tour.table} (id int not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${Abotyp.table} (id int not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), enddatum timestamp, anzahl_lieferungen int, anzahl_abwesenheiten int, preis NUMERIC not null, preisEinheit varchar(20) not null, aktiv bit)".execute.apply()
    }
  }

  override def insert(id: UUID, entity: BaseEntity[_ <: BaseId])(implicit session: DBSession = AutoSession) = {
    //TODO: implement using entity match
  }

  override def delete(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession = AutoSession) = {
    //TODO: implement using entity match
  }

  override def update(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession = AutoSession) = {
    //TODO: implement using entity match
  }
}