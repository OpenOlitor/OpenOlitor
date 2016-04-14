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
import org.joda.time.DateTime

object StammdatenInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenInsertService = new DefaultStammdatenInsertService(sysConfig, system)
}

class DefaultStammdatenInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenInsertService(sysConfig) with DefaultStammdatenRepositoryComponent {
}

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_]] with LazyLogging with AsyncConnectionPoolContextAware
  with StammdatenDBMappings {
  self: StammdatenRepositoryComponent =>

  val ZERO = 0

  val handle: Handle = {
    case EntityInsertedEvent(meta, id, abotyp: AbotypModify) =>
      createAbotyp(meta, id, abotyp)
    case EntityInsertedEvent(meta, id, kunde: KundeModify) =>
      createKunde(meta, id, kunde)
    case EntityInsertedEvent(meta, id, depot: DepotModify) =>
      createDepot(meta, id, depot)
    case EntityInsertedEvent(meta, id, abo: AboModify) =>
      createAbo(meta, id, abo)
    case EntityInsertedEvent(meta, id, lieferung: LieferungAbotypCreate) =>
      createLieferung(meta, id, lieferung)
    case EntityInsertedEvent(meta, id, lieferung: HeimlieferungAbotypModify) =>
      createHeimlieferungVertriebsart(meta, id, lieferung)
    case EntityInsertedEvent(meta, id, lieferung: PostlieferungAbotypModify) =>
      createPostlieferungVertriebsart(meta, id, lieferung)
    case EntityInsertedEvent(meta, id, depotlieferung: DepotlieferungAbotypModify) =>
      createDepotlieferungVertriebsart(meta, id, depotlieferung)
    case EntityInsertedEvent(meta, id, kundentyp: CustomKundentypCreate) =>
      createKundentyp(meta, id, kundentyp)
    case EntityInsertedEvent(meta, id, produkt: ProduktModify) =>
      createProdukt(meta, id, produkt)
    case EntityInsertedEvent(meta, id, produktekategorie: ProduktekategorieModify) =>
      createProduktekategorie(meta, id, produktekategorie)
    case EntityInsertedEvent(meta, id, produzent: ProduzentModify) =>
      createProduzent(meta, id, produzent)
    case EntityInsertedEvent(meta, id, tour: TourModify) =>
      createTour(meta, id, tour)
    case EntityInsertedEvent(meta, id, projekt: ProjektModify) =>
      createProjekt(meta, id, projekt)
    case EntityInsertedEvent(meta, id, entity) =>
      logger.debug(s"Receive unmatched insert event for entity:$entity with id:$id")
    case e =>
      logger.warn(s"Unknown event:$e")
  }

  def createAbotyp(meta: EventMetadata, id: UUID, abotyp: AbotypModify)(implicit userId: UserId = meta.originator) = {
    val abotypId = AbotypId(id)
    val typ = copyTo[AbotypModify, Abotyp](abotyp, 
      "id" -> abotypId,
      "anzahlAbonnenten" -> ZERO,
      "letzteLieferung" -> None,
      "waehrung" -> CHF,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      //create abotyp
      writeRepository.insertEntity[Abotyp, AbotypId](typ)
    }
  }

  def createDepotlieferungVertriebsart(meta: EventMetadata, id: UUID, vertriebsart: DepotlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[DepotlieferungAbotypModify, Depotlieferung](vertriebsart, "id" -> vertriebsartId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Depotlieferung, VertriebsartId](insert)
    }
  }

  def createHeimlieferungVertriebsart(meta: EventMetadata, id: UUID, vertriebsart: HeimlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[HeimlieferungAbotypModify, Heimlieferung](vertriebsart, "id" -> vertriebsartId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Heimlieferung, VertriebsartId](insert)
    }
  }

  def createPostlieferungVertriebsart(meta: EventMetadata, id: UUID, vertriebsart: PostlieferungAbotypModify)(implicit userId: UserId = meta.originator) = {
    val vertriebsartId = VertriebsartId(id)
    val insert = copyTo[PostlieferungAbotypModify, Postlieferung](vertriebsart, "id" -> vertriebsartId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Postlieferung, VertriebsartId](insert)
    }
  }

  def createLieferung(meta: EventMetadata, id: UUID, lieferung: LieferungAbotypCreate)(implicit userId: UserId = meta.originator) = {
    val lieferungId = LieferungId(id)
    val insert = copyTo[LieferungAbotypCreate, Lieferung](lieferung, "id" -> lieferungId,
      "anzahlAbwesenheiten" -> ZERO,
      "status" -> Offen,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      //create lieferung
      writeRepository.insertEntity[Lieferung, LieferungId](insert)
    }
  }

  def createKunde(meta: EventMetadata, id: UUID, create: KundeModify)(implicit userId: UserId = meta.originator) = {
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
        "anzahlPendenzen" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
      DB autoCommit { implicit session =>
        //create abotyp
        writeRepository.insertEntity[Kunde, KundeId](kunde)
      }

      //create personen as well
      create.ansprechpersonen.zipWithIndex.map {
        case (person, index) =>
          createPerson(meta, UUID.randomUUID, person, kundeId, index)
      }
    }
  }

  def createPerson(meta: EventMetadata, id: UUID, create: PersonModify, kundeId: KundeId, sort: Int)(implicit userId: UserId = meta.originator) = {
    val personId = PersonId(id)

    val person = copyTo[PersonModify, Person](create, "id" -> personId,
      "kundeId" -> kundeId,
      "sort" -> sort,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Person, PersonId](person)
    }
  }
  
  def createPendenz(meta: EventMetadata, id: UUID, create: PendenzModify, kundeId: KundeId, kundeBezeichnung: String)(implicit userId: UserId = meta.originator) = {
    val pendenzId = PendenzId(id)

    val pendenz = copyTo[PendenzModify, Pendenz](create, "id" -> pendenzId,
        "kundeId" -> kundeId, "kundeBezeichnung" -> kundeBezeichnung,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Pendenz, PendenzId](pendenz)
    }
  }

  def createDepot(meta: EventMetadata, id: UUID, create: DepotModify)(implicit userId: UserId = meta.originator) = {
    val depotId = DepotId(id)
    val depot = copyTo[DepotModify, Depot](create,
      "id" -> depotId,
      "farbCode" -> "",
      "anzahlAbonnenten" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Depot, DepotId](depot)
    }
  }

  def createAbo(meta: EventMetadata, id: UUID, create: AboModify)(implicit userId: UserId = meta.originator) = {
    DB autoCommit { implicit session =>
      val aboId = AboId(id)
      val abo = create match {
        case create: DepotlieferungAboModify =>
          writeRepository.insertEntity[DepotlieferungAbo, AboId](copyTo[DepotlieferungAboModify, DepotlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator))
        case create: HeimlieferungAboModify =>
          writeRepository.insertEntity[HeimlieferungAbo, AboId](copyTo[HeimlieferungAboModify, HeimlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator))
        case create: PostlieferungAboModify =>
          writeRepository.insertEntity[PostlieferungAbo, AboId](copyTo[PostlieferungAboModify, PostlieferungAbo](create,
            "id" -> aboId, "saldo" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator))
      }
    }
  }

  def createKundentyp(meta: EventMetadata, id: UUID, create: CustomKundentypCreate)(implicit userId: UserId = meta.originator) = {
    val customKundentypId = CustomKundentypId(id)
    val kundentyp = copyTo[CustomKundentypCreate, CustomKundentyp](create,
      "id" -> customKundentypId,
      "anzahlVerknuepfungen" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[CustomKundentyp, CustomKundentypId](kundentyp)
    }
  }
  
  def createProdukt(meta: EventMetadata, id: UUID, create: ProduktModify)(implicit userId: UserId = meta.originator) = {
    val produktId = ProduktId(id)
    val produkt = copyTo[ProduktModify, Produkt](create,
      "id" -> produktId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Produkt, ProduktId](produkt)
    }
  }
  
  def createProduktekategorie(meta: EventMetadata, id: UUID, create: ProduktekategorieModify)(implicit userId: UserId = meta.originator) = {
    val produktekategorieId = ProduktekategorieId(id)
    val produktekategrie = copyTo[ProduktekategorieModify, Produktekategorie](create,
      "id" -> produktekategorieId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Produktekategorie, ProduktekategorieId](produktekategrie)
    }
  }
  
  def createProduzent(meta: EventMetadata, id: UUID, create: ProduzentModify)(implicit userId: UserId = meta.originator) = {
    val produzentId = ProduzentId(id)
    val produzent = copyTo[ProduzentModify, Produzent](create,
      "id" -> produzentId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Produzent, ProduzentId](produzent)
    }
  }
  
  def createTour(meta: EventMetadata, id: UUID, create: TourModify)(implicit userId: UserId = meta.originator) = {
    val tourId = TourId(id)
    val tour = copyTo[TourModify, Tour](create,
      "id" -> tourId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Tour, TourId](tour)
    }
  }
  
   def createProjekt(meta: EventMetadata, id: UUID, create: ProjektModify)(implicit userId: UserId = meta.originator) = {
    val projektId = ProjektId(id)
    val projekt = copyTo[ProjektModify, Projekt](create,
      "id" -> projektId,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)
    DB autoCommit { implicit session =>
      writeRepository.insertEntity[Projekt, ProjektId](projekt)
    }
  }
}