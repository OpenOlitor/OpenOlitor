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

import ch.openolitor.core._
import ch.openolitor.core.db._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models._

object StammdatenInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenInsertService = new DefaultStammdatenInsertService(sysConfig, system)
}

class DefaultStammdatenInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenInsertService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent] with LazyLogging with AsyncConnectionPoolContextAware
  with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>

  //TODO: replace with credentials of logged in user
  implicit val userId = Boot.systemUserId

  val ZERO = 0

  val handle: Handle = {
    case EntityInsertedEvent(meta, id, abotyp: AbotypModify) =>
      createAbotyp(id, abotyp)
    case EntityInsertedEvent(meta, id, kunde: KundeModify) =>
      createKunde(id, kunde)
    case EntityInsertedEvent(meta, id, depot: DepotModify) =>
      createDepot(id, depot)
    case EntityInsertedEvent(meta, id, abo: AboModify) =>
      createAbo(id, abo)
    case EntityInsertedEvent(meta, id, lieferung: LieferungAbotypCreate) =>
      createLieferung(id, lieferung)
    case EntityInsertedEvent(meta, id, lieferung: HeimlieferungAbotypModify) =>
      createVertriebsart(id, lieferung)
    case EntityInsertedEvent(meta, id, lieferung: PostlieferungAbotypModify) =>
      createVertriebsart(id, lieferung)
    case EntityInsertedEvent(meta, id, depotlieferung: DepotlieferungAbotypModify) =>
      createVertriebsart(id, depotlieferung)
    case EntityInsertedEvent(meta, id, kundentyp: CustomKundentypCreate) =>
      createKundentyp(id, kundentyp)
    case EntityInsertedEvent(meta, id, produkt: ProduktModify) =>
      createProdukt(id, produkt)
    case EntityInsertedEvent(meta, id, produktekategorie: ProduktekategorieModify) =>
      createProduktekategorie(id, produktekategorie)
    case EntityInsertedEvent(meta, id, produzent: ProduzentModify) =>
      createProduzent(id, produzent)
    case EntityInsertedEvent(meta, id, tour: TourModify) =>
      createTour(id, tour)
    case EntityInsertedEvent(meta, id, projekt: ProjektModify) =>
      createProjekt(id, projekt)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def createAbotyp(id: UUID, abotyp: AbotypModify) = {
    val abotypId = AbotypId(id)
    val typ = copyTo[AbotypModify, Abotyp](abotyp, "id" -> abotypId,
      "anzahlAbonnenten" -> ZERO,
      "letzteLieferung" -> None,
      "waehrung" -> CHF)

    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity(typ)
    }
  }

  def createVertriebsart(id: UUID, vertriebsart: DepotlieferungAbotypModify) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[DepotlieferungAbotypModify, Depotlieferung](vertriebsart, "id" -> vertriebsartId)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity(insert)
    }
  }

  def createVertriebsart(id: UUID, vertriebsart: HeimlieferungAbotypModify) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[HeimlieferungAbotypModify, Heimlieferung](vertriebsart, "id" -> vertriebsartId)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity(insert)
    }
  }

  def createVertriebsart(id: UUID, vertriebsart: PostlieferungAbotypModify) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[PostlieferungAbotypModify, Postlieferung](vertriebsart, "id" -> vertriebsartId)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity(insert)
    }
  }

  def createLieferung(id: UUID, lieferung: LieferungAbotypCreate) = {
    val lieferungId = LieferungId(id)
    val insert = copyTo[LieferungAbotypCreate, Lieferung](lieferung, "id" -> lieferungId,
      "anzahlAbwesenheiten" -> ZERO,
      "status" -> Offen)

    DB autoCommit { implicit session =>
      //create lieferung
      writeRepository.insertEntity(insert)
    }
  }

  def createKunde(id: UUID, create: KundeModify) = {
    if (create.ansprechpersonen.isEmpty) {
      //TODO: handle error
    } else {
      val kundeId = KundeId(id)
      val bez = create.bezeichnung.getOrElse(create.ansprechpersonen.head.fullName)
      val kunde = copyTo[KundeModify, Kunde](create,
        "id" -> kundeId,
        "bezeichnung" -> bez,
        "anzahlPersonen" -> create.ansprechpersonen.length,
        "anzahlAbos" -> ZERO,
        "anzahlPendenzen" -> ZERO)
      DB autoCommit { implicit session =>
        //create abotyp
        writeRepository.insertEntity(kunde)
      }

      //create personen as well
      create.ansprechpersonen.zipWithIndex.map {
        case (person, index) =>
          createPerson(UUID.randomUUID, person, kundeId, index)
      }
    }
  }

  def createPerson(id: UUID, create: PersonModify, kundeId: KundeId, sort: Int) = {
    val personId = PersonId(id)

    val person = copyTo[PersonModify, Person](create, "id" -> personId,
      "kundeId" -> kundeId,
      "sort" -> sort)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity(person)
    }
  }
  
  def createPendenz(id: UUID, create: PendenzModify, kundeId: KundeId, kundeBezeichnung: String) = {
    val pendenzId = PendenzId(id)

    val pendenz = copyTo[PendenzModify, Pendenz](create, "id" -> pendenzId,
        "kundeId" -> kundeId, "kundeBezeichnung" -> kundeBezeichnung)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity(pendenz)
    }
  }

  def createDepot(id: UUID, create: DepotModify) = {
    val depotId = DepotId(id)
    val depot = copyTo[DepotModify, Depot](create,
      "id" -> depotId,
      "anzahlAbonnenten" -> ZERO)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(depot)
    }
  }

  def createAbo(id: UUID, create: AboModify) = {
    DB autoCommit { implicit session =>
      val aboId = AboId(id)
      val abo = create match {
        case create: DepotlieferungAboModify =>
          writeRepository.insertEntity(copyTo[DepotlieferungAboModify, DepotlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO))
        case create: HeimlieferungAboModify =>
          writeRepository.insertEntity(copyTo[HeimlieferungAboModify, HeimlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO))
        case create: PostlieferungAboModify =>
          writeRepository.insertEntity(copyTo[PostlieferungAboModify, PostlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO))
      }
    }
  }

  def createKundentyp(id: UUID, create: CustomKundentypCreate) = {
    val customKundentypId = CustomKundentypId(id)
    val kundentyp = copyTo[CustomKundentypCreate, CustomKundentyp](create,
      "id" -> customKundentypId,
      "anzahlVerknuepfungen" -> ZERO)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(kundentyp)
    }
  }
  
  def createProdukt(id: UUID, create: ProduktModify) = {
    val produktId = ProduktId(id)
    val produkt = copyTo[ProduktModify, Produkt](create,
      "id" -> produktId)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(produkt)
    }
  }
  
  def createProduktekategorie(id: UUID, create: ProduktekategorieModify) = {
    val produktekategorieId = ProduktekategorieId(id)
    val produktekategrie = copyTo[ProduktekategorieModify, Produktekategorie](create,
      "id" -> produktekategorieId)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(produktekategrie)
    }
  }
  
  def createProduzent(id: UUID, create: ProduzentModify) = {
    val produzentId = ProduzentId(id)
    val produzent = copyTo[ProduzentModify, Produzent](create,
      "id" -> produzentId)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(produzent)
    }
  }
  
  def createTour(id: UUID, create: TourModify) = {
    val tourId = TourId(id)
    val tour = copyTo[TourModify, Tour](create,
      "id" -> tourId)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(tour)
    }
  }
  
   def createProjekt(id: UUID, create: ProjektModify) = {
    val projektId = ProjektId(id)
    val projekt = copyTo[ProjektModify, Projekt](create,
      "id" -> projektId)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity(projekt)
    }
  }
}