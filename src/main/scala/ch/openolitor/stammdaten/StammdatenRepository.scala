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
import org.joda.time.DateTime
import sqls.distinct
import sqls.{ min, count }
import scalaz.IsEmpty
import ch.openolitor.core.AkkaEventStream

trait StammdatenReadRepository {
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]]
  def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]]

  def getUngeplanteLieferungen(abotypId: AbotypId, vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]

  def getVertrieb(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Vertrieb]]
  def getVertriebe(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebVertriebsarten]]

  def getVertriebsart(vertriebsartId: VertriebsartId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[VertriebsartDetail]]
  def getVertriebsarten(vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebsartDetail]]

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]]
  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]]

  def getKundentypen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kundentyp]]
  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]]

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]
  def getPersonByEmail(email: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
  def getPerson(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]]
  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]]

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]]
  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]]
  def getAktiveAbos(abotypId: AbotypId, lieferdatum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]]

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Int]]

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

  def getLieferplanungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferplanung]]
  def getLieferplanung(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLatestLieferplanung(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLieferungenNext()(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferung]]
  def getLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getNichtInkludierteAbotypenLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Korb]]

  def getBestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellung]]
  def getBestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellung]]
  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]]
  def getBestellpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]]
  def getBestellpositionByBestellungProdukt(bestellungId: BestellungId, produktId: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellposition]]
}

trait StammdatenWriteRepository extends BaseWriteRepository with EventStream {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)

  def deleteLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, session: DBSession): Int
  def deleteKoerbe(id: LieferungId)(implicit context: ExecutionContext, session: DBSession): Int

  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp]

  def getProjekt(implicit session: DBSession): Option[Projekt]
}

class StammdatenReadRepositoryImpl extends StammdatenReadRepository with LazyLogging with StammdatenRepositoryQueries {

  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]] = {
    getAbotypenQuery.future
  }

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]] = {
    getKundenQuery.future
  }

  def getKundentypen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kundentyp]] = {
    for {
      ct <- getCustomKundentypen
    } yield {
      (ct ++ SystemKundentyp.ALL.toList).sortBy(_.kundentyp.id)
    }
  }

  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]] = {
    getCustomKundentypenQuery.future
  }

  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]] = {
    getKundeDetailQuery(id).future
  }

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]] = {
    getPersonenQuery(kundeId).future
  }

  def getPersonByEmail(email: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]] = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.email, parameter(email))
    }.map(personMapping(person)).single.future
  }

  def getPerson(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]] = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.id, parameter(id))
    }.map(personMapping(person)).single.future
  }

  override def getAbotypDetail(id: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Abotyp]] = {
    getAbotypDetailQuery(id).future
  }

  def getVertrieb(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Vertrieb]] = {
    getVertriebQuery(vertriebId).future
  }

  def getVertriebe(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[VertriebVertriebsarten]] = {
    getVertriebeQuery(abotypId).future
  }

  def getVertriebsarten(vertriebId: VertriebId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext) = {
    for {
      d <- getDepotlieferung(vertriebId)
      h <- getHeimlieferung(vertriebId)
      p <- getPostlieferung(vertriebId)
    } yield {
      d ++ h ++ p
    }
  }

  def getDepotlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungDetail]] = {
    getDepotlieferungQuery(vertriebId).future
  }

  def getHeimlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungDetail]] = {
    getHeimlieferungQuery(vertriebId).future
  }

  def getPostlieferung(vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungDetail]] = {
    getPostlieferungQuery(vertriebId).future
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
    getDepotlieferungQuery(vertriebsartId).future
  }

  def getHeimlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungDetail]] = {
    getHeimlieferungQuery(vertriebsartId).future
  }

  def getPostlieferung(vertriebsartId: VertriebsartId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungDetail]] = {
    getPostlieferungQuery(vertriebsartId).future
  }

  def getUngeplanteLieferungen(abotypId: AbotypId, vertriebId: VertriebId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    getUngeplanteLieferungenQuery(abotypId, vertriebId).future
  }

  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    getUngeplanteLieferungenQuery(abotypId).future
  }

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]] = {
    getDepotsQuery.future
  }

  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]] = {
    getDepotDetailQuery(id).future
  }

  def getDepotlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungAbo]] = {
    getDepotlieferungAbosQuery.future
  }

  def getHeimlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungAbo]] = {
    getHeimlieferungAbosQuery.future
  }

  def getPostlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungAbo]] = {
    getPostlieferungAbosQuery.future
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
    getDepotlieferungAboQuery(id).future
  }

  def getHeimlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungAboDetail]] = {
    getHeimlieferungAboQuery(id).future
  }

  def getPostlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungAboDetail]] = {
    getPostlieferungAboQuery(id).future
  }

  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]] = {
    for {
      d <- getDepotlieferungAbo(id)
      h <- getHeimlieferungAbo(id)
      p <- getPostlieferungAbo(id)
    } yield (d orElse h orElse p)
  }

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Int]] = {
    countAbwesendQuery(lieferungId, aboId).future
  }

  def getAktiveAbos(abotypId: AbotypId, lieferdatum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abo]] = {
    for {
      d <- getAktiveDepotlieferungAbos(abotypId, lieferdatum)
      h <- getAktiveHeimlieferungAbos(abotypId, lieferdatum)
      p <- getAktivePostlieferungAbos(abotypId, lieferdatum)
    } yield {
      d ::: h ::: p
    }
  }

  def getAktiveDepotlieferungAbos(abotypId: AbotypId, lieferdatum: DateTime)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotlieferungAbo]] = {
    getAktiveDepotlieferungAbosQuery(abotypId, lieferdatum).future
  }

  def getAktiveHeimlieferungAbos(abotypId: AbotypId, lieferdatum: DateTime)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[HeimlieferungAbo]] = {
    getAktiveHeimlieferungAbosQuery(abotypId, lieferdatum).future
  }

  def getAktivePostlieferungAbos(abotypId: AbotypId, lieferdatum: DateTime)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostlieferungAbo]] = {
    getAktivePostlieferungAbosQuery(abotypId, lieferdatum).future
  }

  def getPendenzen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    getPendenzenQuery.future
  }

  def getPendenzen(id: KundeId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Pendenz]] = {
    getPendenzenQuery(id).future
  }

  def getPendenzDetail(id: PendenzId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Pendenz]] = {
    getPendenzDetailQuery(id).future
  }

  def getProdukte(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    getProdukteQuery.future
  }

  def getProduktekategorien(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produktekategorie]] = {
    getProduktekategorienQuery.future
  }

  def getProduzenten(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produzent]] = {
    getProduzentenQuery.future
  }

  def getProduzentDetail(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    getProduzentDetailQuery(id).future
  }

  def getTouren(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Tour]] = {
    getTourenQuery.future
  }

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]] = {
    getProjektQuery.future
  }

  def getProduktProduzenten(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduzent]] = {
    getProduktProduzentenQuery(id).future
  }

  def getProduktProduktekategorien(id: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProduktProduktekategorie]] = {
    getProduktProduktekategorienQuery(id).future
  }

  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produzent]] = {
    getProduzentDetailByKurzzeichenQuery(kurzzeichen).future
  }

  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Produktekategorie]] = {
    getProduktekategorieByBezeichnungQuery(bezeichnung).future
  }

  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Produkt]] = {
    getProdukteByProduktekategorieBezeichnungQuery(bezeichnung).future
  }

  def getLieferplanungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferplanung]] = {
    getLieferplanungenQuery.future
  }

  def getLatestLieferplanung(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]] = {
    getLatestLieferplanungQuery.future
  }

  def getLieferplanung(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]] = {
    getLieferplanungQuery(id).future
  }

  def getLieferungenNext()(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    getLieferungenNextQuery.future
  }

  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferung]] = {
    getLastGeplanteLieferungQuery(abotypId).future
  }

  def getLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    getLieferungenQuery(id).future
  }

  def getNichtInkludierteAbotypenLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]] = {
    getNichtInkludierteAbotypenLieferungenQuery(id).future
  }

  def getBestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellung]] = {
    getBestellungenQuery(id).future
  }

  def getBestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellung]] = {
    getBestellungByProduzentLieferplanungDatumQuery(produzentId, lieferplanungId, datum).future
  }

  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]] = {
    getBestellpositionenQuery(id).future
  }

  def getBestellpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]] = {
    getBestellpositionenByLieferplanQuery(id).future
  }

  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    getLieferpositionenQuery(id).future
  }

  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    getLieferpositionenByLieferantQuery(id).future
  }

  def getBestellpositionByBestellungProdukt(bestellungId: BestellungId, produktId: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellposition]] = {
    getBestellpositionByBestellungProduktQuery(bestellungId, produktId).future
  }

  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    getLieferpositionenByLieferplanQuery(id).future
  }

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Korb]] = {
    getKorbQuery(lieferungId, aboId).future
  }

}

class StammdatenWriteRepositoryImpl(val system: ActorSystem) extends StammdatenWriteRepository with LazyLogging with AkkaEventStream with StammdatenRepositoryQueries {

  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {
    DB localTx { implicit session =>
      sql"truncate table ${postlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotlieferungMapping.table}".execute.apply()
      sql"truncate table ${heimlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotMapping.table}".execute.apply()
      sql"truncate table ${tourMapping.table}".execute.apply()
      sql"truncate table ${abotypMapping.table}".execute.apply()
      sql"truncate table ${kundeMapping.table}".execute.apply()
      sql"truncate table ${pendenzMapping.table}".execute.apply()
      sql"truncate table ${customKundentypMapping.table}".execute.apply()
      sql"truncate table ${personMapping.table}".execute.apply()
      sql"truncate table ${depotlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${heimlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${postlieferungAboMapping.table}".execute.apply()
      sql"truncate table ${lieferplanungMapping.table}".execute.apply()
      sql"truncate table ${lieferungMapping.table}".execute.apply()
      sql"truncate table ${lieferpositionMapping.table}".execute.apply()
      sql"truncate table ${bestellungMapping.table}".execute.apply()
      sql"truncate table ${bestellpositionMapping.table}".execute.apply()
      sql"truncate table ${produktMapping.table}".execute.apply()
      sql"truncate table ${produktekategorieMapping.table}".execute.apply()
      sql"truncate table ${produzentMapping.table}".execute.apply()
      sql"truncate table ${projektMapping.table}".execute.apply()
      sql"truncate table ${produktProduzentMapping.table}".execute.apply()
      sql"truncate table ${produktProduktekategorieMapping.table}".execute.apply()
      sql"truncate table ${abwesenheitMapping.table}".execute.apply()
      sql"truncate table ${korbMapping.table}".execute.apply()
    }
  }

  def deleteLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, session: DBSession): Int = {
    deleteLieferpositionenQuery(id).update.apply
  }

  def deleteKoerbe(id: LieferungId)(implicit context: ExecutionContext, session: DBSession): Int = {
    deleteKoerbeQuery(id).update.apply
  }

  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp] = {
    getAbotypDetailQuery(id).apply()
  }

  def getProjekt(implicit session: DBSession): Option[Projekt] = {
    getProjektQuery.apply()
  }

}
