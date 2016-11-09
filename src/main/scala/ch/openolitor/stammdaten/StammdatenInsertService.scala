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
import ch.openolitor.stammdaten.repositories._
import java.util.UUID
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.Macros._
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.models._
import org.joda.time.DateTime
import org.joda.time.LocalDate
import ch.openolitor.core.Macros._
import scala.collection.immutable.TreeMap
import scalaz._
import Scalaz._
import ch.openolitor.util.IdUtil
import ch.openolitor.stammdaten.models.LieferpositionenModify
import scalikejdbc.DBSession

object StammdatenInsertService {
  def apply(implicit sysConfig: SystemConfig, system: ActorSystem): StammdatenInsertService = new DefaultStammdatenInsertService(sysConfig, system)
}

class DefaultStammdatenInsertService(sysConfig: SystemConfig, override val system: ActorSystem)
  extends StammdatenInsertService(sysConfig) with DefaultStammdatenWriteRepositoryComponent

/**
 * Actor zum Verarbeiten der Insert Anweisungen für das Stammdaten Modul
 */
class StammdatenInsertService(override val sysConfig: SystemConfig) extends EventService[EntityInsertedEvent[_, _]]
    with LazyLogging
    with AsyncConnectionPoolContextAware
    with StammdatenDBMappings
    with KorbHandler
    with LieferungHandler {
  self: StammdatenWriteRepositoryComponent =>

  val ZERO = 0
  val FALSE = false

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: AbotypId, abotyp: AbotypModify) =>
      createAbotyp(meta, id, abotyp)
    case EntityInsertedEvent(meta, id: KundeId, kunde: KundeModify) =>
      createKunde(meta, id, kunde)
    case EntityInsertedEvent(meta, id: PersonId, person: PersonCreate) =>
      createPerson(meta, id, person)
    case EntityInsertedEvent(meta, id: PendenzId, pendenz: PendenzCreate) =>
      createPendenz(meta, id, pendenz)
    case EntityInsertedEvent(meta, id: DepotId, depot: DepotModify) =>
      createDepot(meta, id, depot)
    case EntityInsertedEvent(meta, id: AboId, abo: AboModify) =>
      createAbo(meta, id, abo)
    case EntityInsertedEvent(meta, id: LieferungId, lieferung: LieferungAbotypCreate) =>
      createLieferung(meta, id, lieferung)
    case EntityInsertedEvent(meta, id: VertriebId, vertrieb: VertriebModify) =>
      createVertrieb(meta, id, vertrieb)
    case EntityInsertedEvent(meta, id: VertriebsartId, lieferung: HeimlieferungAbotypModify) =>
      createHeimlieferungVertriebsart(meta, id, lieferung)
    case EntityInsertedEvent(meta, id: VertriebsartId, lieferung: PostlieferungAbotypModify) =>
      createPostlieferungVertriebsart(meta, id, lieferung)
    case EntityInsertedEvent(meta, id: VertriebsartId, depotlieferung: DepotlieferungAbotypModify) =>
      createDepotlieferungVertriebsart(meta, id, depotlieferung)
    case EntityInsertedEvent(meta, id: CustomKundentypId, kundentyp: CustomKundentypCreate) =>
      createKundentyp(meta, id, kundentyp)
    case EntityInsertedEvent(meta, id: ProduktId, produkt: ProduktModify) =>
      createProdukt(meta, id, produkt)
    case EntityInsertedEvent(meta, id: ProduktekategorieId, produktekategorie: ProduktekategorieModify) =>
      createProduktekategorie(meta, id, produktekategorie)
    case EntityInsertedEvent(meta, id: ProduzentId, produzent: ProduzentModify) =>
      createProduzent(meta, id, produzent)
    case EntityInsertedEvent(meta, id: TourId, tour: TourCreate) =>
      createTour(meta, id, tour)
    case EntityInsertedEvent(meta, id: ProjektId, projekt: ProjektModify) =>
      createProjekt(meta, id, projekt)
    case EntityInsertedEvent(meta, id: AbwesenheitId, abw: AbwesenheitCreate) =>
      createAbwesenheit(meta, id, abw)
    case EntityInsertedEvent(meta, id: LieferplanungId, lieferplanungCreateData: LieferplanungCreate) =>
      createLieferplanung(meta, id, lieferplanungCreateData)
    case EntityInsertedEvent(meta, id: LieferungId, entity: LieferungPlanungAdd) =>
      addLieferungToPlanung(meta, id, entity)
    case EntityInsertedEvent(meta, id: BestellungId, bestellungCreateData: BestellungCreate) =>
      createBestellungen(meta, id, bestellungCreateData)
    case EntityInsertedEvent(meta, id: ProjektVorlageId, vorlage: ProjektVorlageCreate) =>
      createProjektVorlage(meta, id, vorlage)
    case e =>
  }

  def createAbotyp(meta: EventMetadata, id: AbotypId, abotyp: AbotypModify)(implicit personId: PersonId = meta.originator) = {
    val typ = copyTo[AbotypModify, Abotyp](
      abotyp,
      "id" -> id,
      "anzahlAbonnenten" -> ZERO,
      "anzahlAbonnentenAktiv" -> ZERO,
      "letzteLieferung" -> None,
      "waehrung" -> CHF,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      //create abotyp
      stammdatenWriteRepository.insertEntity[Abotyp, AbotypId](typ)
    }
  }

  def createVertrieb(meta: EventMetadata, id: VertriebId, vertrieb: VertriebModify)(implicit personId: PersonId = meta.originator) = {
    val emptyIntMap: TreeMap[String, Int] = TreeMap()
    val emptyDecimalMap: TreeMap[String, BigDecimal] = TreeMap()
    val vertriebCreate = copyTo[VertriebModify, Vertrieb](
      vertrieb,
      "id" -> id,
      "anzahlAbos" -> ZERO,
      "anzahlAbosAktiv" -> ZERO,
      "durchschnittspreis" -> emptyDecimalMap,
      "anzahlLieferungen" -> emptyIntMap,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      //create abotyp
      stammdatenWriteRepository.insertEntity[Vertrieb, VertriebId](vertriebCreate)
    }
  }

  def createDepotlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: DepotlieferungAbotypModify)(implicit personId: PersonId = meta.originator) = {
    val insert = copyTo[DepotlieferungAbotypModify, Depotlieferung](vertriebsart, "id" -> id,
      "anzahlAbos" -> ZERO,
      "anzahlAbosAktiv" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Depotlieferung, VertriebsartId](insert)
    }
  }

  def createHeimlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: HeimlieferungAbotypModify)(implicit personId: PersonId = meta.originator) = {
    val insert = copyTo[HeimlieferungAbotypModify, Heimlieferung](vertriebsart, "id" -> id,
      "anzahlAbos" -> ZERO,
      "anzahlAbosAktiv" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Heimlieferung, VertriebsartId](insert)
    }
  }

  def createPostlieferungVertriebsart(meta: EventMetadata, id: VertriebsartId, vertriebsart: PostlieferungAbotypModify)(implicit personId: PersonId = meta.originator) = {
    val insert = copyTo[PostlieferungAbotypModify, Postlieferung](vertriebsart, "id" -> id,
      "anzahlAbos" -> ZERO,
      "anzahlAbosAktiv" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator)

    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Postlieferung, VertriebsartId](insert)
    }
  }

  def createLieferung(meta: EventMetadata, id: LieferungId, lieferung: LieferungAbotypCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getAbotypDetail(lieferung.abotypId) match {
        case Some(abotyp) =>
          stammdatenWriteRepository.getById(vertriebMapping, lieferung.vertriebId) map {
            vertrieb =>
              val vBeschrieb = vertrieb.beschrieb
              val atBeschrieb = abotyp.name

              val insert = copyTo[LieferungAbotypCreate, Lieferung](lieferung, "id" -> id,
                "abotypBeschrieb" -> atBeschrieb,
                "vertriebBeschrieb" -> vBeschrieb,
                "anzahlAbwesenheiten" -> ZERO,
                "durchschnittspreis" -> ZERO,
                "anzahlLieferungen" -> ZERO,
                "anzahlKoerbeZuLiefern" -> ZERO,
                "anzahlSaldoZuTief" -> ZERO,
                "zielpreis" -> abotyp.zielpreis,
                "preisTotal" -> ZERO,
                "status" -> Ungeplant,
                "lieferplanungId" -> None,
                "erstelldat" -> meta.timestamp,
                "ersteller" -> meta.originator,
                "modifidat" -> meta.timestamp,
                "modifikator" -> meta.originator)

              //create lieferung
              stammdatenWriteRepository.insertEntity[Lieferung, LieferungId](insert)
          }
        case _ =>
          logger.error(s"Abotyp with id ${lieferung.abotypId} not found.")
      }
    }
  }

  def createKunde(meta: EventMetadata, id: KundeId, create: KundeModify)(implicit personId: PersonId = meta.originator) = {
    val bez = create.bezeichnung.getOrElse(create.ansprechpersonen.head.fullName)
    val kunde = copyTo[KundeModify, Kunde](
      create,
      "id" -> id,
      "bezeichnung" -> bez,
      "anzahlPersonen" -> create.ansprechpersonen.length,
      "anzahlAbos" -> ZERO,
      "anzahlAbosAktiv" -> ZERO,
      "anzahlPendenzen" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      //create abotyp
      stammdatenWriteRepository.insertEntity[Kunde, KundeId](kunde)
    }
  }

  def createPerson(meta: EventMetadata, id: PersonId, create: PersonCreate)(implicit personId: PersonId = meta.originator) = {
    val rolle: Option[Rolle] = Some(KundenZugang)

    val person = copyTo[PersonCreate, Person](
      create,
      "id" -> id,
      "kundeId" -> create.kundeId,
      "sort" -> create.sort,
      "loginAktiv" -> FALSE,
      "letzteAnmeldung" -> None,
      "passwort" -> None,
      "passwortWechselErforderlich" -> FALSE,
      "rolle" -> rolle,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )

    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Person, PersonId](person)
    }
  }

  def createPendenz(meta: EventMetadata, id: PendenzId, create: PendenzCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(kundeMapping, create.kundeId) map { kunde =>
        val pendenz = copyTo[PendenzCreate, Pendenz](create, "id" -> id,
          "kundeId" -> create.kundeId,
          "kundeBezeichnung" -> kunde.bezeichnung,
          "erstelldat" -> meta.timestamp,
          "ersteller" -> meta.originator,
          "modifidat" -> meta.timestamp,
          "modifikator" -> meta.originator)

        stammdatenWriteRepository.insertEntity[Pendenz, PendenzId](pendenz)
      }
    }
  }

  def createDepot(meta: EventMetadata, id: DepotId, create: DepotModify)(implicit personId: PersonId = meta.originator) = {
    val depot = copyTo[DepotModify, Depot](
      create,
      "id" -> id,
      "anzahlAbonnenten" -> ZERO,
      "anzahlAbonnentenAktiv" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Depot, DepotId](depot)
    }
  }

  def aboParameters(create: AboModify)(abotyp: Abotyp): (Option[Int], Option[LocalDate], Boolean) = {
    abotyp.laufzeiteinheit match {
      case Unbeschraenkt =>
        (None, None, IAbo.calculateAktiv(create.start, None))

      case Lieferungen =>
        (abotyp.laufzeit, None, IAbo.calculateAktiv(create.start, None))

      case Monate =>
        val ende = Some(create.start.plusMonths(abotyp.laufzeit.get))
        (None, ende, IAbo.calculateAktiv(create.start, ende))
    }
  }

  private def vertriebsartById(vertriebsartId: VertriebsartId)(implicit session: DBSession): Option[Vertriebsart] = {
    stammdatenWriteRepository.getById(depotlieferungMapping, vertriebsartId) orElse
      stammdatenWriteRepository.getById(heimlieferungMapping, vertriebsartId) orElse
      stammdatenWriteRepository.getById(postlieferungMapping, vertriebsartId)
  }

  private def abotypById(abotypId: AbotypId)(implicit session: DBSession): Option[Abotyp] = {
    stammdatenWriteRepository.getById(abotypMapping, abotypId)
  }

  private def vertriebById(vertriebId: VertriebId)(implicit session: DBSession): Option[Vertrieb] = {
    stammdatenWriteRepository.getById(vertriebMapping, vertriebId)
  }

  private def abotypByVertriebartId(vertriebsartId: VertriebsartId)(implicit session: DBSession): Option[(Vertriebsart, Vertrieb, Abotyp)] = {
    vertriebsartById(vertriebsartId) flatMap (va => vertriebById(va.vertriebId) flatMap (v => abotypById(v.abotypId).map(at => (va, v, at))))
  }

  private def depotById(depotId: DepotId)(implicit session: DBSession): Option[Depot] = {
    stammdatenWriteRepository.getById(depotMapping, depotId)
  }

  private def tourById(tourId: TourId)(implicit session: DBSession): Option[Tour] = {
    stammdatenWriteRepository.getById(tourMapping, tourId)
  }

  def createAbo(meta: EventMetadata, id: AboId, create: AboModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      val emptyMap: TreeMap[String, Int] = TreeMap()
      abotypByVertriebartId(create.vertriebsartId) map {
        case (vertriebsart, vertrieb, abotyp) =>
          aboParameters(create)(abotyp) match {
            case (guthaben, ende, aktiv) =>
              val abo = create match {
                case create: DepotlieferungAboModify =>
                  val depotName = depotById(create.depotId).map(_.name).getOrElse("")

                  stammdatenWriteRepository.insertEntity[DepotlieferungAbo, AboId](copyTo[DepotlieferungAboModify, DepotlieferungAbo](
                    create,
                    "id" -> id,
                    "vertriebId" -> vertrieb.id,
                    "vertriebBeschrieb" -> vertrieb.beschrieb,
                    "abotypId" -> abotyp.id,
                    "abotypName" -> abotyp.name,
                    "depotName" -> depotName,
                    "ende" -> ende,
                    "guthabenVertraglich" -> guthaben,
                    "guthaben" -> ZERO,
                    "guthabenInRechnung" -> ZERO,
                    "letzteLieferung" -> None,
                    "anzahlAbwesenheiten" -> emptyMap,
                    "anzahlLieferungen" -> emptyMap,
                    "aktiv" -> aktiv,
                    "erstelldat" -> meta.timestamp,
                    "ersteller" -> meta.originator,
                    "modifidat" -> meta.timestamp,
                    "modifikator" -> meta.originator
                  ))
                case create: HeimlieferungAboModify =>
                  val tourName = tourById(create.tourId).map(_.name).getOrElse("")

                  stammdatenWriteRepository.insertEntity[HeimlieferungAbo, AboId](copyTo[HeimlieferungAboModify, HeimlieferungAbo](
                    create,
                    "id" -> id,
                    "vertriebId" -> vertrieb.id,
                    "vertriebBeschrieb" -> vertrieb.beschrieb,
                    "abotypId" -> abotyp.id,
                    "abotypName" -> abotyp.name,
                    "tourName" -> tourName,
                    "ende" -> ende,
                    "guthabenVertraglich" -> guthaben,
                    "guthaben" -> ZERO,
                    "guthabenInRechnung" -> ZERO,
                    "letzteLieferung" -> None,
                    "anzahlAbwesenheiten" -> emptyMap,
                    "anzahlLieferungen" -> emptyMap,
                    "aktiv" -> aktiv,
                    "erstelldat" -> meta.timestamp,
                    "ersteller" -> meta.originator,
                    "modifidat" -> meta.timestamp,
                    "modifikator" -> meta.originator
                  ))
                case create: PostlieferungAboModify =>
                  stammdatenWriteRepository.insertEntity[PostlieferungAbo, AboId](copyTo[PostlieferungAboModify, PostlieferungAbo](
                    create,
                    "id" -> id,
                    "vertriebId" -> vertrieb.id,
                    "vertriebBeschrieb" -> vertrieb.beschrieb,
                    "abotypId" -> abotyp.id,
                    "abotypName" -> abotyp.name,
                    "ende" -> ende,
                    "guthabenVertraglich" -> guthaben,
                    "guthaben" -> ZERO,
                    "guthabenInRechnung" -> ZERO,
                    "letzteLieferung" -> None,
                    "anzahlAbwesenheiten" -> emptyMap,
                    "anzahlLieferungen" -> emptyMap,
                    "aktiv" -> aktiv,
                    "erstelldat" -> meta.timestamp,
                    "ersteller" -> meta.originator,
                    "modifidat" -> meta.timestamp,
                    "modifikator" -> meta.originator
                  ))
              }
          }
      }
    }
  }

  def createKundentyp(meta: EventMetadata, id: CustomKundentypId, create: CustomKundentypCreate)(implicit personId: PersonId = meta.originator) = {
    val kundentyp = copyTo[CustomKundentypCreate, CustomKundentyp](
      create,
      "id" -> id,
      "anzahlVerknuepfungen" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[CustomKundentyp, CustomKundentypId](kundentyp)
    }
  }

  def createProdukt(meta: EventMetadata, id: ProduktId, create: ProduktModify)(implicit personId: PersonId = meta.originator) = {
    val produkt = copyTo[ProduktModify, Produkt](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Produkt, ProduktId](produkt)
    }
  }

  def createProduktekategorie(meta: EventMetadata, id: ProduktekategorieId, create: ProduktekategorieModify)(implicit personId: PersonId = meta.originator) = {
    val produktekategrie = copyTo[ProduktekategorieModify, Produktekategorie](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Produktekategorie, ProduktekategorieId](produktekategrie)
    }
  }

  def createProduzent(meta: EventMetadata, id: ProduzentId, create: ProduzentModify)(implicit personId: PersonId = meta.originator) = {
    val produzent = copyTo[ProduzentModify, Produzent](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Produzent, ProduzentId](produzent)
    }
  }

  def createTour(meta: EventMetadata, id: TourId, create: TourCreate)(implicit personId: PersonId = meta.originator) = {
    val tour = copyTo[TourCreate, Tour](
      create,
      "id" -> id,
      "anzahlAbonnenten" -> ZERO,
      "anzahlAbonnentenAktiv" -> ZERO,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Tour, TourId](tour)
    }
  }

  def createProjekt(meta: EventMetadata, id: ProjektId, create: ProjektModify)(implicit personId: PersonId = meta.originator) = {
    val projekt = copyTo[ProjektModify, Projekt](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Projekt, ProjektId](projekt)
    }
  }

  def createAbwesenheit(meta: EventMetadata, id: AbwesenheitId, create: AbwesenheitCreate)(implicit personId: PersonId = meta.originator) = {
    val abw = copyTo[AbwesenheitCreate, Abwesenheit](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.insertEntity[Abwesenheit, AbwesenheitId](abw)
    }
  }

  def createLieferplanung(meta: EventMetadata, lieferplanungId: LieferplanungId, lieferplanung: LieferplanungCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      val defaultAbotypDepotTour = ""
      val insert = copyTo[LieferplanungCreate, Lieferplanung](
        lieferplanung,
        "id" -> lieferplanungId,
        "status" -> Offen,
        "total" -> ZERO,
        "abotypDepotTour" -> defaultAbotypDepotTour,
        "erstelldat" -> meta.timestamp,
        "ersteller" -> meta.originator,
        "modifidat" -> meta.timestamp,
        "modifikator" -> meta.originator
      )
      //create lieferplanung
      stammdatenWriteRepository.insertEntity[Lieferplanung, LieferplanungId](insert) map { lieferplanung =>
        //alle nächsten Lieferungen alle Abotypen (wenn Flag es erlaubt)
        val abotypDepotTour = stammdatenWriteRepository.getLieferungenNext() map { lieferung =>
          logger.debug("createLieferplanung: Lieferung " + lieferung.id + ": " + lieferung)

          val lpId = Some(lieferplanung.id)

          val updatedLieferung = lieferung.copy(
            lieferplanungId = lpId,
            status = Offen,
            modifidat = lieferplanung.modifidat,
            modifikator = personId
          )

          //create koerbe
          val adjustedLieferung = createKoerbe(updatedLieferung)

          //update Lieferung
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](adjustedLieferung)

          adjustedLieferung.abotypBeschrieb
        }
        val abotypStr = abotypDepotTour.filter(_.nonEmpty).mkString(", ")
        val updatedObj = lieferplanung.copy(abotypDepotTour = abotypStr)

        //update lieferplanung
        stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](updatedObj)
      }
    }

    logger.debug(s"Lieferplanung created, notify clients:$lieferplanungId")
    stammdatenWriteRepository.publish(DataEvent(personId, LieferplanungCreated(lieferplanungId)))
  }

  def createKoerbe(lieferung: Lieferung)(implicit personId: PersonId, session: DBSession) = {
    logger.debug(s"Create Koerbe:${lieferung.id}")
    stammdatenWriteRepository.getById(abotypMapping, lieferung.abotypId) map { abotyp =>
      val abos = stammdatenWriteRepository.getAktiveAbos(lieferung.vertriebId, lieferung.datum)
      val statusL = (abos map { abo =>
        upsertKorb(lieferung, abo, abotyp)
      }).map(_._1).flatten map (_.status)
      val counts = statusL.groupBy { _.getClass }.mapValues(_.size)

      logger.debug(s"Update lieferung:$lieferung")
      val copy = lieferung.copy(
        anzahlKoerbeZuLiefern = counts.get(WirdGeliefert.getClass).getOrElse(0),
        anzahlAbwesenheiten = counts.get(FaelltAusAbwesend.getClass).getOrElse(0),
        anzahlSaldoZuTief = counts.get(FaelltAusSaldoZuTief.getClass).getOrElse(0)
      )
      copy
    } getOrElse (lieferung)
  }

  def addLieferungToPlanung(meta: EventMetadata, id: LieferungId, data: LieferungPlanungAdd)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(lieferplanungMapping, data.lieferplanungId) map { lieferplanung =>
        stammdatenWriteRepository.getById(lieferungMapping, data.id) map { lieferung =>
          val (newDurchschnittspreis, newAnzahlLieferungen) = stammdatenWriteRepository.getGeplanteLieferungVorher(lieferung.vertriebId, lieferung.datum) match {
            case Some(lieferungVorher) =>
              val sum = stammdatenWriteRepository.sumPreisTotalGeplanteLieferungenVorher(lieferung.vertriebId, lieferung.datum).getOrElse(BigDecimal(0))

              val durchschnittspreisBisher: BigDecimal = lieferungVorher.anzahlLieferungen match {
                case 0 => BigDecimal(0)
                case _ => sum / lieferungVorher.anzahlLieferungen
              }
              val anzahlLieferungenNeu = lieferungVorher.anzahlLieferungen + 1
              (durchschnittspreisBisher, anzahlLieferungenNeu)
            case None =>
              (BigDecimal(0), 1)
          }
          val lpId = Some(data.lieferplanungId)

          val updatedLieferung = lieferung.copy(
            lieferplanungId = lpId,
            status = Offen,
            durchschnittspreis = newDurchschnittspreis,
            anzahlLieferungen = newAnzahlLieferungen,
            modifidat = meta.timestamp,
            modifikator = personId
          )

          //create koerbe
          val adjustedLieferung = createKoerbe(updatedLieferung)

          //update Lieferung
          stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](adjustedLieferung)
        }
      }
    }
  }

  def createBestellungen(meta: EventMetadata, id: BestellungId, create: BestellungCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      stammdatenWriteRepository.getById(produzentMapping, create.produzentId) map { produzent =>
        val bestellung = Bestellung(
          id,
          create.produzentId,
          produzent.kurzzeichen,
          create.lieferplanungId,
          Abgeschlossen,
          create.datum,
          None,
          0,
          produzent.mwstSatz,
          0,
          0,
          None,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )
        stammdatenWriteRepository.insertEntity[Bestellung, BestellungId](bestellung) map { bestellung =>
          val anzahlKoerbeZuLiefern = stammdatenWriteRepository.getLieferungen(create.lieferplanungId).map(l => (l.id, l.anzahlKoerbeZuLiefern)).toMap
          val positionen = stammdatenWriteRepository.getLieferpositionenByLieferplanAndProduzent(create.lieferplanungId, create.produzentId).
            //group by same produkt, menge and preis
            groupBy(x => (x.produktId, x.menge, x.preis)).map {
              case ((produktId, menge, preis), positionen) =>
                positionen.map(lp => anzahlKoerbeZuLiefern.get(lp.lieferungId).getOrElse(0)).sum match {
                  case 0 => //don't add position
                    None
                  case anzahl =>
                    positionen.headOption map { lieferposition =>
                      Bestellposition(
                        BestellpositionId(IdUtil.positiveRandomId),
                        bestellung.id,
                        lieferposition.produktId,
                        lieferposition.produktBeschrieb,
                        lieferposition.preisEinheit,
                        lieferposition.einheit,
                        menge.getOrElse(0),
                        preis.map(_ * anzahl),
                        anzahl,
                        DateTime.now,
                        personId,
                        DateTime.now,
                        personId
                      )
                    }
                }
            }.flatten

          //delete all Bestellpositionen from Bestellungen (Bestellungen are maintained even if nothing is ordered/bestellt)
          stammdatenWriteRepository.getBestellpositionen(id) foreach {
            position => stammdatenWriteRepository.deleteEntity[Bestellposition, BestellpositionId](position.id)
          }

          positionen.map { bestellposition =>
            stammdatenWriteRepository.insertEntity[Bestellposition, BestellpositionId](bestellposition)
          }

          val total = positionen.map(_.preis).flatten.sum
          val mwst = bestellung.steuerSatz.map(_ / 100 * total).getOrElse(BigDecimal(0))
          val totalInkl = total + mwst

          //update total on bestellung, steuer and totalSteuer
          val copy = bestellung.copy(preisTotal = total, steuer = mwst, totalSteuer = totalInkl)
          stammdatenWriteRepository.updateEntity[Bestellung, BestellungId](copy)
        }
      }
    }
  }

  def createProjektVorlage(meta: EventMetadata, id: ProjektVorlageId, create: ProjektVorlageCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommit { implicit session =>
      val vorlage = copyTo[ProjektVorlageCreate, ProjektVorlage](create, "id" -> id,
        "fileStoreId" -> None,
        "erstelldat" -> meta.timestamp,
        "ersteller" -> meta.originator,
        "modifidat" -> meta.timestamp,
        "modifikator" -> meta.originator)

      stammdatenWriteRepository.insertEntity[ProjektVorlage, ProjektVorlageId](vorlage)
    }
  }
}
