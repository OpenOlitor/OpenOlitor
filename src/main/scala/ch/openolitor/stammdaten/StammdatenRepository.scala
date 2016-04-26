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
import ch.openolitor.util.DateTimeUtil._

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
  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]]

  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]]
  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]]

  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]]
  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]]
  def getProduktProduzenten(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduzent]]
  def getProduktProduktekategorien(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduktekategorie]]

  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]]

  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produzent]]
  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]]
  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]]
  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produktekategorie]]

  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]]

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]]
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
  lazy val projekt = projektMapping.syntax("projekt")
  lazy val produktProduzent = produktProduzentMapping.syntax("produktProduzent")
  lazy val produktProduktekategorie = produktProduktekategorieMapping.syntax("produktProduktekategorie")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")

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

  def getDepotlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungAboDetail]] = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(depotlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(depotlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .where.eq(depotlieferungAbo.id, parameter(id)).and.withRoundBracket { _.isNull(lieferung.id).or.not.eq(lieferung.status, parameter(Bearbeitet)) }
    }
      .one(depotlieferungAboMapping(depotlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs))
      .map((abo, abw, lieferungen, aboTyp) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[DepotlieferungAbo, DepotlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen, "abotyp" -> aboTyp.headOption)
      }).single.future
  }

  def getHeimlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungAboDetail]] = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(heimlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(heimlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .where.eq(heimlieferungAbo.id, parameter(id)).and.withRoundBracket { _.isNull(lieferung.id).or.not.eq(lieferung.status, parameter(Bearbeitet)) }
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs))
      .map((abo, abw, lieferungen, aboTyp) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[HeimlieferungAbo, HeimlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen, "abotyp" -> aboTyp.headOption)
      }).single.future
  }

  def getPostlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungAboDetail]] = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(postlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(postlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .where.eq(postlieferungAbo.id, parameter(id)).and.withRoundBracket { _.isNull(lieferung.id).or.not.eq(lieferung.status, parameter(Bearbeitet)) }
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs))
      .map((abo, abw, lieferungen, aboTyp) => {
        val sortedAbw = abw.sortBy(_.datum)
        copyTo[PostlieferungAbo, PostlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> lieferungen, "abotyp" -> aboTyp.headOption)
      }).single.future
  }

  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]] = {
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

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]] = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single.future
  }

  def getProduktProduzenten(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduzent]] = {
    withSQL {
      select
        .from(produktProduzentMapping as produktProduzent)
        .where.eq(produktProduzent.produktId, parameter(id))
    }.map(produktProduzentMapping(produktProduzent)).list.future
  }

  def getProduktProduktekategorien(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduktekategorie]] = {
    withSQL {
      select
        .from(produktProduktekategorieMapping as produktProduktekategorie)
        .where.eq(produktProduktekategorie.produktId, parameter(id))
    }.map(produktProduktekategorieMapping(produktProduktekategorie)).list.future
  }

  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.kurzzeichen, parameter(kurzzeichen))
    }.map(produzentMapping(produzent)).single.future
  }

  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produktekategorie]] = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
        .where.eq(produktekategorie.beschreibung, parameter(bezeichnung))
    }.map(produktekategorieMapping(produktekategorie)).single.future
  }

  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    withSQL {
      select
        .from(produktMapping as produkt)
        .where.like(produkt.kategorien, '%' + bezeichnung + '%')
    }.map(produktMapping(produkt)).list.future
  }

}

class StammdatenWriteRepositoryImpl(val system: ActorSystem) extends StammdatenWriteRepository with LazyLogging with EventStream with StammdatenDBMappings {

  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {
  }
}
