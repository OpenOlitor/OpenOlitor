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
package ch.openolitor.stammdaten.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import scalikejdbc.async._
import scala.concurrent.ExecutionContext
import ch.openolitor.core.db._
import ch.openolitor.core.db.OOAsyncDB._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import scala.concurrent._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import org.joda.time.DateTime
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.parsing.FilterExpr

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
  def getKundenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KundeUebersicht]]
  def getKundeDetail(id: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetail]]
  def getKundeDetailReport(kundeId: KundeId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetailReport]]

  def getKundentypen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kundentyp]]
  def getCustomKundentypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[CustomKundentyp]]

  def getPersonen(kundeId: KundeId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Person]]
  def getPersonByEmail(email: String)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
  def getPerson(id: PersonId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Person]]
  def getPersonenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonUebersicht]]

  def getDepots(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Depot]]
  def getDepotDetail(id: DepotId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Depot]]
  def getDepotDetailReport(id: DepotId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotDetailReport]]

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[Abo]]
  def getAboDetail(id: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AboDetail]]

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
  def getTourDetail(id: TourId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourDetail]]

  def getProjekt(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Projekt]]

  def getLieferplanungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferplanung]]
  def getLieferplanung(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLatestLieferplanung(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferplanung]]
  def getLieferungenNext()(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferung]]
  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Lieferung]]
  def getLieferungenDetails(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]]
  def getVerfuegbareLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]]
  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]]
  def getAboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]]

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Korb]]

  def getBestellungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[Bestellung]]
  def getBestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellung]]
  def getBestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellung]]
  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]]
  def getBestellungDetail(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[BestellungDetail]]
  def getBestellpositionByBestellungProdukt(bestellungId: BestellungId, produktId: ProduktId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellposition]]

  def getDepotAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotAuslieferung]]
  def getTourAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[TourAuslieferung]]
  def getPostAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostAuslieferung]]
  def getAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]]

  def getDepotAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotAuslieferungDetail]]
  def getTourAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourAuslieferungDetail]]
  def getPostAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostAuslieferungDetail]]

  def getProjektVorlagen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProjektVorlage]]
  def getProjektVorlage(id: ProjektVorlageId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektVorlage]]

  def getEinladung(token: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Einladung]]
}

class StammdatenReadRepositoryImpl extends BaseReadRepository with StammdatenReadRepository with LazyLogging with StammdatenRepositoryQueries {
  def getAbotypen(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Abotyp]] = {
    getAbotypenQuery.future
  }

  def getKunden(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Kunde]] = {
    getKundenQuery.future
  }

  def getKundenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[KundeUebersicht]] = {
    getKundenUebersichtQuery.future
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

  def getKundeDetailReport(kundeId: KundeId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[KundeDetailReport]] = {
    getKundeDetailReportQuery(kundeId, projekt).future
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

  def getPersonenUebersicht(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PersonUebersicht]] = {
    getPersonenUebersichtQuery(filter).future
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

  def getDepotDetailReport(id: DepotId, projekt: ProjektReport)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotDetailReport]] = {
    getDepotDetailReportQuery(id, projekt).future
  }

  def getDepotlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[DepotlieferungAbo]] = {
    getDepotlieferungAbosQuery(filter).future
  }

  def getHeimlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[HeimlieferungAbo]] = {
    getHeimlieferungAbosQuery(filter).future
  }

  def getPostlieferungAbos(implicit asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[PostlieferungAbo]] = {
    getPostlieferungAbosQuery(filter).future
  }

  def getAbos(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[Abo]] = {
    for {
      d <- getDepotlieferungAbos
      h <- getHeimlieferungAbos
      p <- getPostlieferungAbos
    } yield {
      d ::: h ::: p
    }
  }

  def getDepotlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotlieferungAboDetail]] = {
    getDepotlieferungAboAusstehendQuery(id).future
  }

  def getHeimlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[HeimlieferungAboDetail]] = {
    getHeimlieferungAboAusstehendQuery(id).future
  }

  def getPostlieferungAbo(id: AboId)(implicit asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostlieferungAboDetail]] = {
    getPostlieferungAboAusstehendQuery(id).future
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

  def getTourDetail(id: TourId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourDetail]] = {
    getTourDetailQuery(id).future
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

  def getLieferungenDetails(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]] = {
    getLieferungenDetailsQuery(id).future
  }

  def getVerfuegbareLieferungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[LieferungDetail]] = {
    getVerfuegbareLieferungenQuery(id).future
  }

  def getBestellungen(id: LieferplanungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellung]] = {
    getBestellungenQuery(id).future
  }

  def getBestellungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext, filter: Option[FilterExpr]): Future[List[Bestellung]] = {
    getBestellungenQuery(filter).future
  }

  def getBestellungByProduzentLieferplanungDatum(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Bestellung]] = {
    getBestellungByProduzentLieferplanungDatumQuery(produzentId, lieferplanungId, datum).future
  }

  def getBestellungDetail(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[BestellungDetail]] = {
    getBestellungDetailQuery(id).future
  }

  def getBestellpositionen(id: BestellungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Bestellposition]] = {
    getBestellpositionenQuery(id).future
  }

  def getLieferpositionen(id: LieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    getLieferpositionenQuery(id).future
  }

  def getLieferpositionenByLieferant(id: ProduzentId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[Lieferposition]] = {
    getLieferpositionenByLieferantQuery(id).future
  }

  def getAboIds(lieferungId: LieferungId, korbStatus: KorbStatus)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[AboId]] = {
    getAboIdsQuery(lieferungId, korbStatus).future
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

  def getDepotAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[DepotAuslieferung]] = {
    getDepotAuslieferungenQuery.future
  }
  def getTourAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[TourAuslieferung]] = {
    getTourAuslieferungenQuery.future

  }
  def getPostAuslieferungen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[PostAuslieferung]] = {
    getPostAuslieferungenQuery.future
  }

  def getDepotAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[DepotAuslieferungDetail]] = {
    getDepotAuslieferungDetailQuery(auslieferungId).future
  }

  def getTourAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[TourAuslieferungDetail]] = {
    getTourAuslieferungDetailQuery(auslieferungId).future
  }

  def getPostAuslieferungDetail(auslieferungId: AuslieferungId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[PostAuslieferungDetail]] = {
    getPostAuslieferungDetailQuery(auslieferungId).future
  }

  def getAuslieferungReport(id: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    for {
      d <- getDepotAuslieferungReport(id, projekt)
      h <- getTourAuslieferungReport(id, projekt)
      p <- getPostAuslieferungReport(id, projekt)
    } yield (d orElse h orElse p)
  }

  def getDepotAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getDepotAuslieferungReportQuery(auslieferungId, projekt).future
  }

  def getTourAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getTourAuslieferungReportQuery(auslieferungId, projekt).future
  }

  def getPostAuslieferungReport(auslieferungId: AuslieferungId, projekt: ProjektReport)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[AuslieferungReport]] = {
    getPostAuslieferungReportQuery(auslieferungId, projekt).future
  }

  def getProjektVorlagen(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[List[ProjektVorlage]] = {
    getProjektVorlagenQuery.future
  }

  def getProjektVorlage(id: ProjektVorlageId)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[ProjektVorlage]] = {
    getByIdQuery(projektVorlageMapping, id).future
  }

  def getEinladung(token: String)(implicit context: ExecutionContext, asyncCpContext: MultipleAsyncConnectionPoolContext): Future[Option[Einladung]] = {
    getEinladungQuery(token).future
  }
}
