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
import ch.openolitor.core.repositories.BaseWriteRepository
import ch.openolitor.stammdaten.models._
import org.joda.time.DateTime
import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.AkkaEventStream
import ch.openolitor.core.EventStream

trait StammdatenWriteRepository extends BaseWriteRepository with EventStream {
  def cleanupDatabase(implicit cpContext: ConnectionPoolContext)

  def deleteLieferpositionen(id: LieferungId)(implicit session: DBSession): Int
  def deleteKoerbe(id: LieferungId)(implicit session: DBSession): Int

  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp]
  def getAboDetail(id: AboId)(implicit session: DBSession): Option[AboDetail]
  def getAboDetailAusstehend(id: AboId)(implicit session: DBSession): Option[AboDetail]

  def getProjekt(implicit session: DBSession): Option[Projekt]
  def getKunden(implicit session: DBSession): List[Kunde]
  def getKundentypen(implicit session: DBSession): List[Kundentyp]
  def getPersonen(kundeId: KundeId)(implicit session: DBSession): List[Person]
  def getPendenzen(id: KundeId)(implicit session: DBSession): List[Pendenz]

  def getLatestLieferplanung(implicit session: DBSession): Option[Lieferplanung]
  def getLieferungenNext()(implicit session: DBSession): List[Lieferung]
  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit session: DBSession): Option[Lieferung]
  def getBestellpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Bestellposition]
  def getLieferplanung(id: LieferplanungId)(implicit session: DBSession): Option[Lieferplanung]
  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Lieferposition]
  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung]
  def getProduktProduzenten(id: ProduktId)(implicit session: DBSession): List[ProduktProduzent]
  def getProduzentDetail(id: ProduzentId)(implicit session: DBSession): Option[Produzent]
  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit session: DBSession): Option[Produzent]
  def getProduktProduktekategorien(id: ProduktId)(implicit session: DBSession): List[ProduktProduktekategorie]
  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit session: DBSession): Option[Produktekategorie]
  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit session: DBSession): List[Produkt]
  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Korb]
  def getKoerbe(lieferungId: LieferungId, vertriebsartId: VertriebsartId, status: KorbStatus)(implicit session: DBSession): List[Korb]
  def getAktiveAbos(vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[Abo]
  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Int]
  def getLieferungen(id: LieferplanungId)(implicit session: DBSession): List[Lieferung]
  def getBestellungen(id: LieferplanungId)(implicit session: DBSession): List[Bestellung]
  def getBestellpositionen(id: BestellungId)(implicit session: DBSession): List[Bestellposition]
  def getVertriebsarten(vertriebId: VertriebId)(implicit session: DBSession): List[VertriebsartDetail]

  def getTourlieferungenByKunde(id: KundeId)(implicit session: DBSession): List[Tourlieferung]

  def getDepotAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[DepotAuslieferung]
  def getTourAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[TourAuslieferung]
  def getPostAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[PostAuslieferung]
  def getDepotlieferungAbosByDepot(id: DepotId)(implicit session: DBSession): List[DepotlieferungAbo]
}

class StammdatenWriteRepositoryImpl(val system: ActorSystem) extends StammdatenWriteRepository with LazyLogging with AkkaEventStream with StammdatenRepositoryQueries {

  override def cleanupDatabase(implicit cpContext: ConnectionPoolContext) = {
    DB autoCommit { implicit session =>
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
      sql"truncate table ${tourlieferungMapping.table}".execute.apply()
      sql"truncate table ${depotAuslieferungMapping.table}".execute.apply()
      sql"truncate table ${tourAuslieferungMapping.table}".execute.apply()
      sql"truncate table ${postAuslieferungMapping.table}".execute.apply()
    }
  }

  def deleteLieferpositionen(id: LieferungId)(implicit session: DBSession): Int = {
    deleteLieferpositionenQuery(id).update.apply
  }

  def deleteKoerbe(id: LieferungId)(implicit session: DBSession): Int = {
    deleteKoerbeQuery(id).update.apply
  }

  def getAbotypDetail(id: AbotypId)(implicit session: DBSession): Option[Abotyp] = {
    getAbotypDetailQuery(id).apply()
  }

  def getProjekt(implicit session: DBSession): Option[Projekt] = {
    getProjektQuery.apply()
  }

  def getAboDetail(id: AboId)(implicit session: DBSession): Option[AboDetail] = {
    getDepotlieferungAbo(id) orElse getHeimlieferungAbo(id) orElse getPostlieferungAbo(id)
  }

  def getDepotlieferungAbo(id: AboId)(implicit session: DBSession): Option[DepotlieferungAboDetail] = {
    getDepotlieferungAboQuery(id).apply()
  }

  def getHeimlieferungAbo(id: AboId)(implicit session: DBSession): Option[HeimlieferungAboDetail] = {
    getHeimlieferungAboQuery(id).apply()
  }

  def getPostlieferungAbo(id: AboId)(implicit session: DBSession): Option[PostlieferungAboDetail] = {
    getPostlieferungAboQuery(id).apply()
  }

  def getAboDetailAusstehend(id: AboId)(implicit session: DBSession): Option[AboDetail] = {
    getDepotlieferungAboAusstehend(id) orElse getHeimlieferungAboAusstehend(id) orElse getPostlieferungAboAusstehend(id)
  }

  def getDepotlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[DepotlieferungAboDetail] = {
    getDepotlieferungAboAusstehendQuery(id).apply()
  }

  def getHeimlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[HeimlieferungAboDetail] = {
    getHeimlieferungAboAusstehendQuery(id).apply()
  }

  def getPostlieferungAboAusstehend(id: AboId)(implicit session: DBSession): Option[PostlieferungAboDetail] = {
    getPostlieferungAboAusstehendQuery(id).apply()
  }

  def getKunden(implicit session: DBSession): List[Kunde] = {
    getKundenQuery.apply()
  }

  def getKundentypen(implicit session: DBSession): List[Kundentyp] = {
    (getCustomKundentypen ++ SystemKundentyp.ALL.toList).sortBy(_.kundentyp.id)
  }

  def getCustomKundentypen(implicit session: DBSession): List[CustomKundentyp] = {
    getCustomKundentypenQuery.apply()
  }

  def getPersonen(kundeId: KundeId)(implicit session: DBSession): List[Person] = {
    getPersonenQuery(kundeId).apply()
  }

  def getPendenzen(id: KundeId)(implicit session: DBSession): List[Pendenz] = {
    getPendenzenQuery(id).apply()
  }

  def getLatestLieferplanung(implicit session: DBSession): Option[Lieferplanung] = {
    getLatestLieferplanungQuery.apply()
  }

  def getLieferungenNext()(implicit session: DBSession): List[Lieferung] = {
    getLieferungenNextQuery.apply()
  }

  def getLastGeplanteLieferung(abotypId: AbotypId)(implicit session: DBSession): Option[Lieferung] = {
    getLastGeplanteLieferungQuery(abotypId).apply()
  }

  def getBestellpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Bestellposition] = {
    getBestellpositionenByLieferplanQuery(id).apply()
  }

  def getLieferplanung(id: LieferplanungId)(implicit session: DBSession): Option[Lieferplanung] = {
    getLieferplanungQuery(id).apply()
  }

  def getLieferpositionenByLieferplan(id: LieferplanungId)(implicit session: DBSession): List[Lieferposition] = {
    getLieferpositionenByLieferplanQuery(id).apply()
  }

  def getUngeplanteLieferungen(abotypId: AbotypId)(implicit session: DBSession): List[Lieferung] = {
    getUngeplanteLieferungenQuery(abotypId).apply()
  }

  def getProduktProduzenten(id: ProduktId)(implicit session: DBSession): List[ProduktProduzent] = {
    getProduktProduzentenQuery(id).apply()
  }

  def getProduzentDetail(id: ProduzentId)(implicit session: DBSession): Option[Produzent] = {
    getProduzentDetailQuery(id).apply()
  }

  def getProduzentDetailByKurzzeichen(kurzzeichen: String)(implicit session: DBSession): Option[Produzent] = {
    getProduzentDetailByKurzzeichenQuery(kurzzeichen).apply()
  }

  def getProduktProduktekategorien(id: ProduktId)(implicit session: DBSession): List[ProduktProduktekategorie] = {
    getProduktProduktekategorienQuery(id).apply()
  }

  def getProduktekategorieByBezeichnung(bezeichnung: String)(implicit session: DBSession): Option[Produktekategorie] = {
    getProduktekategorieByBezeichnungQuery(bezeichnung).apply()
  }

  def getProdukteByProduktekategorieBezeichnung(bezeichnung: String)(implicit session: DBSession): List[Produkt] = {
    getProdukteByProduktekategorieBezeichnungQuery(bezeichnung).apply()
  }

  def getKorb(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Korb] = {
    getKorbQuery(lieferungId, aboId).apply()
  }

  def getKoerbe(lieferungId: LieferungId, vertriebsartId: VertriebsartId, status: KorbStatus)(implicit session: DBSession): List[Korb] = {
    getKoerbeQuery(lieferungId, vertriebsartId, status).apply()
  }

  def getAktiveAbos(vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[Abo] = {
    getAktiveDepotlieferungAbos(vertriebId, lieferdatum) :::
      getAktiveHeimlieferungAbos(vertriebId, lieferdatum) :::
      getAktivePostlieferungAbos(vertriebId, lieferdatum)
  }

  def getAktiveDepotlieferungAbos(vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getAktiveDepotlieferungAbosQuery(vertriebId, lieferdatum).apply
  }

  def getAktiveHeimlieferungAbos(vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[HeimlieferungAbo] = {
    getAktiveHeimlieferungAbosQuery(vertriebId, lieferdatum).apply
  }

  def getAktivePostlieferungAbos(vertriebId: VertriebId, lieferdatum: DateTime)(implicit session: DBSession): List[PostlieferungAbo] = {
    getAktivePostlieferungAbosQuery(vertriebId, lieferdatum).apply
  }

  def countAbwesend(lieferungId: LieferungId, aboId: AboId)(implicit session: DBSession): Option[Int] = {
    countAbwesendQuery(lieferungId, aboId).apply()
  }

  def getLieferungen(id: LieferplanungId)(implicit session: DBSession): List[Lieferung] = {
    getLieferungenQuery(id).apply()
  }

  def getBestellungen(id: LieferplanungId)(implicit session: DBSession): List[Bestellung] = {
    getBestellungenQuery(id).apply()
  }

  def getBestellpositionen(id: BestellungId)(implicit session: DBSession): List[Bestellposition] = {
    getBestellpositionenQuery(id).apply()
  }

  def getTourlieferungenByKunde(id: KundeId)(implicit session: DBSession): List[Tourlieferung] = {
    getTourlieferungenByKundeQuery(id).apply()
  }

  def getVertriebsarten(vertriebId: VertriebId)(implicit session: DBSession): List[VertriebsartDetail] = {
    getDepotlieferung(vertriebId) ++ getHeimlieferung(vertriebId) ++ getPostlieferung(vertriebId)
  }

  def getDepotlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[DepotlieferungDetail] = {
    getDepotlieferungQuery(vertriebId).apply()
  }

  def getHeimlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[HeimlieferungDetail] = {
    getHeimlieferungQuery(vertriebId).apply()
  }

  def getPostlieferung(vertriebId: VertriebId)(implicit session: DBSession): List[PostlieferungDetail] = {
    getPostlieferungQuery(vertriebId).apply()
  }

  def getDepotAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[DepotAuslieferung] = {
    getDepotAuslieferungQuery(lieferungId).apply()
  }

  def getTourAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[TourAuslieferung] = {
    getTourAuslieferungQuery(lieferungId).apply()
  }

  def getPostAuslieferung(lieferungId: LieferungId)(implicit session: DBSession): Option[PostAuslieferung] = {
    getPostAuslieferungQuery(lieferungId).apply()
  }

  def getDepotlieferungAbosByDepot(id: DepotId)(implicit session: DBSession): List[DepotlieferungAbo] = {
    getDepotlieferungAbosByDepotQuery(id).apply()
  }
}
