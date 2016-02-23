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
  def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]]

  def getOffeneLieferungen(abotypId: AbotypId, vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]

  def getVertriebsart(vertriebsartId: VertriebsartId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[VertriebsartDetail]]
  def getVertriebsarten(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebsartDetail]]

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]]
  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]]

  def getKundentypen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kundentyp]]
  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]]

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]]
  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]]

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]]
  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abo]]
  
  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]]
  
  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]]
  
  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]]
  
  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produzent]]
  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]]
  
  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]]
}

trait StammdatenWriteRepository extends BaseWriteRepository {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository with LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val pendenz = pendenzMapping.syntax("pendenz")
  lazy val kundentyp = customKundentypMapping.syntax("kundentyp")
  lazy val postlieferung = postlieferungMapping.syntax("postlieferung")
  lazy val depotlieferung = depotlieferungMapping.syntax("depotlieferung")
  lazy val heimlieferung = heimlieferungMapping.syntax("heimlieferung")
  lazy val depot = depotMapping.syntax("depot")
  lazy val tour = tourMapping.syntax("tour")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val produkt = produktMapping.syntax("produkt")
  lazy val produktekategorie = produktekategorieMapping.syntax("produktekategorie")
  lazy val produzent = produzentMapping.syntax("produzent")

  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]] = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .orderBy(aboTyp.name)
    }.map(abotypMapping(aboTyp)).list.future
  }

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]] = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .orderBy(kunde.bezeichnung)
    }.map(kundeMapping(kunde)).list.future
  }

  def getKundentypen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kundentyp]] = {
    for {
      ct <- getCustomKundentypen
    } yield {
      (ct ++ SystemKundentyp.ALL.toList).sortBy(_.kundentyp.id)
    }
  }

  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]] = {
    withSQL {
      select
        .from(customKundentypMapping as kundentyp)
        .orderBy(kundentyp.id)
    }.map(customKundentypMapping(kundentyp)).list.future
  }

  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]] = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .where.eq(kunde.id, parameter(id))
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs))
      .map({ (kunde, pl, hl, dl, personen, pendenzen) =>
        val abos = pl ++ hl ++ dl

        copyTo[Kunde, KundeDetail](kunde, "abos" -> abos, "pendenzen" -> pendenzen, "ansprechpersonen" -> personen)
      }).single.future
  }

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]] = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, parameter(kundeId))
        .orderBy(person.sort)
    }.map(personMapping(person)).list.future
  }

  override def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]] = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.id, parameter(id))
    }.map(abotypMapping(aboTyp)).single.future
  }

  def getVertriebsarten(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext) = {
    for {
      d <- getDepotlieferung(abotypId)
      h <- getHeimlieferung(abotypId)
      p <- getPostlieferung(abotypId)
    } yield {
      d ++ h ++ p
    }
  }

  def getDepotlieferung(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungDetail]] = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.abotypId, parameter(abotypId))
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.list.future
  }

  def getHeimlieferung(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungDetail]] = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.abotypId, parameter(abotypId))
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.list.future
  }

  def getPostlieferung(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungDetail]] = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.abotypId, parameter(abotypId))
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.list.future
  }

  def getVertriebsart(vertriebsartId: VertriebsartId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext) = {
    for {
      d <- getDepotlieferung(vertriebsartId)
      h <- getHeimlieferung(vertriebsartId)
      p <- getPostlieferung(vertriebsartId)
    } yield {
      Seq(d, h, p).flatten.headOption
    }
  }

  def getDepotlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungDetail]] = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.id, parameter(vertriebsartId))
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.single.future
  }

  def getHeimlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungDetail]] = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.id, parameter(vertriebsartId))
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.single.future
  }

  def getPostlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungDetail]] = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.id, parameter(vertriebsartId))
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.single.future
  }

  def getOffeneLieferungen(abotypId: AbotypId, vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, parameter(abotypId)).and.eq(lieferung.vertriebsartId, parameter(vertriebsartId)).and.not.eq(lieferung.status, parameter(Bearbeitet))
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list.future
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
    } yield {
      d ::: h ::: p
    }
  }

  def getDepotlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungAbo]] = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.id, parameter(id))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).single.future
  }

  def getHeimlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungAbo]] = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.id, parameter(id))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).single.future
  }

  def getPostlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungAbo]] = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.id, parameter(id))
    }.map(postlieferungAboMapping(postlieferungAbo)).single.future
  }

  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abo]] = {
    for {
      d <- getDepotlieferungAbo(id)
      h <- getHeimlieferungAbo(id)
      p <- getPostlieferungAbo(id)
    } yield (d orElse h orElse p)
  }
  
  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .orderBy(pendenz.datum)
    }.map(pendenzMapping(pendenz)).list.future
  }
  
  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.kundeId, parameter(id))
    }.map(pendenzMapping(pendenz)).list.future
  }
    
  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]] = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.id, parameter(id))
    }.map(pendenzMapping(pendenz)).single.future
  }
  
  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    withSQL {
      select
        .from(produktMapping as produkt)
    }.map(produktMapping(produkt)).list.future
  }
  
  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]] = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
    }.map(produktekategorieMapping(produktekategorie)).list.future
  }
  
  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produzent]] = {
    withSQL {
      select
        .from(produzentMapping as produzent)
    }.map(produzentMapping(produzent)).list.future
  }
  
  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, parameter(id))
    }.map(produzentMapping(produzent)).single.future
  }
  
  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]] = {
    withSQL {
      select
        .from(tourMapping as tour)
    }.map(tourMapping(tour)).list.future
  }
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
      sql"drop table if exists ${kundeMapping.table}".execute.apply()
      sql"drop table if exists ${pendenzMapping.table}".execute.apply()
      sql"drop table if exists ${customKundentypMapping.table}".execute.apply()
      sql"drop table if exists ${personMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${postlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${lieferungMapping.table}".execute.apply()
      sql"drop table if exists ${produktMapping.table}".execute.apply()
      sql"drop table if exists ${produktekategorieMapping.table}".execute.apply()
      sql"drop table if exists ${produzentMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables")
      //create tables

      sql"create table ${postlieferungMapping.table}  (id varchar(36) not null, abotyp_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${depotlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, depot_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${heimlieferungMapping.table} (id varchar(36) not null, abotyp_id varchar(36) not null, tour_id varchar(36) not null, liefertag varchar(10))".execute.apply()
      sql"create table ${depotMapping.table} (id varchar(36) not null, name varchar(50) not null, kurzzeichen varchar(6) not null, ap_name varchar(50), ap_vorname varchar(50), ap_telefon varchar(20), ap_email varchar(100), v_name varchar(50), v_vorname varchar(50), v_telefon varchar(20), v_email varchar(100), strasse varchar(50), haus_nummer varchar(10), plz varchar(4) not null, ort varchar(50) not null, aktiv bit, oeffnungszeiten varchar(200), iban varchar(34), bank varchar(50), beschreibung varchar(200), anzahl_abonnenten_max int, anzahl_abonnenten int not null)".execute.apply()
      sql"create table ${tourMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256))".execute.apply()
      sql"create table ${abotypMapping.table} (id varchar(36) not null, name varchar(50) not null, beschreibung varchar(256), lieferrhythmus varchar(256), aktiv_von datetime default null, aktiv_bis datetime default null, preis DECIMAL(7,2) not null, preiseinheit varchar(20) not null, laufzeit int, laufzeiteinheit varchar(50), anzahl_abwesenheiten int, farb_code varchar(20), zielpreis DECIMAL(7,2), saldo_mindestbestand int, anzahl_abonnenten INT not null, letzte_lieferung datetime default null, waehrung varchar(10))".execute.apply()
      sql"create table ${kundeMapping.table} (id varchar(36) not null, bezeichnung varchar(50), strasse varchar(50) not null, haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4) not null, ort varchar(50) not null, bemerkungen varchar(512), strasse_lieferung varchar(50), haus_nummer_lieferung varchar(10), adress_zusatz_lieferung varchar(100), plz_lieferung varchar(4), ort_lieferung varchar(50), typen varchar(200), anzahl_abos int not null, anzahl_pendenzen int not null, anzahl_personen int not null)".execute.apply()
      sql"create table ${pendenzMapping.table} (id varchar(36) not null, kunde_id varchar(50) not null, kunde_bezeichnung varchar(50), datum datetime default null, bemerkung varchar(2000), status varchar(10))".execute.apply()
      sql"create table ${customKundentypMapping.table} (id varchar(36) not null, kundentyp varchar(50) not null, beschreibung varchar(250), anzahl_verknuepfungen int not null)".execute.apply()
      sql"create table ${personMapping.table} (id varchar(36) not null, kunde_id varchar(50) not null, name varchar(50) not null, vorname varchar(50) not null, email varchar(100) not null, email_alternative varchar(100), telefon_mobil varchar(50), telefon_festnetz varchar(50), bemerkungen varchar(512), sort int not null)".execute.apply()
      sql"create table ${depotlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), depot_id varchar(36), depot_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${heimlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), tour_id varchar(36), tour_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${postlieferungAboMapping.table}  (id varchar(36) not null,kunde_id varchar(36) not null, kunde varchar(100), abotyp_id varchar(36) not null, abotyp_name varchar(50), liefertag varchar(10), saldo int)".execute.apply()
      sql"create table ${lieferungMapping.table}  (id varchar(36) not null,abotyp_id varchar(36) not null, vertriebsart_id varchar(36) not null,datum datetime not null, anzahl_abwesenheiten int not null,status varchar(50) not null)".execute.apply()
      sql"create table ${produktMapping.table}  (id varchar(36) not null, name varchar(50) not null, verfuegbar_von varchar(10) not null, verfuegbar_bis varchar(10) not null, kategorien varchar(300), einheit varchar(20) not null, preis DECIMAL(7,2) not null, produzenten varchar(300))".execute.apply()
      sql"create table ${produktekategorieMapping.table}  (id varchar(36) not null, beschreibung varchar(50) not null)".execute.apply()
      sql"create table ${produzentMapping.table}  (id varchar(36) not null, name varchar(50) not null, vorname varchar(50), kurzzeichen varchar(6) not null, strasse varchar(50), haus_nummer varchar(10), adress_zusatz varchar(100), plz varchar(4) not null, ort varchar(50) not null, bemerkungen varchar(1000), email varchar(100) not null, telefon_mobil varchar(50), telefon_festnetz varchar(50), iban varchar(34), bank varchar(50), mwst bit, mwst_satz DECIMAL(4,2), aktiv bit)".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end")
    }
  }
}
