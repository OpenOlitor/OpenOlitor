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
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import scalikejdbc.DB
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.domain.EntityStore._
import akka.actor.ActorSystem
import ch.openolitor.core.models._
import org.joda.time.{ DateTime, LocalDate }
import ch.openolitor.core.Macros._

import scala.collection.immutable.TreeMap
import scalikejdbc.DBSession
import org.joda.time.format.DateTimeFormat
import ch.openolitor.core.repositories.EventPublishingImplicits._
import ch.openolitor.core.repositories.EventPublisher

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
    with SammelbestellungenHandler
    with LieferungHandler {
  self: StammdatenWriteRepositoryComponent =>

  val dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")

  val ZERO = 0
  val FALSE = false

  val handle: Handle = {
    case EntityInsertedEvent(meta, id: AbotypId, abotyp: AbotypModify) =>
      createAbotyp(meta, id, abotyp)
    case EntityInsertedEvent(meta, id: AbotypId, zusatzabotyp: ZusatzAbotypModify) =>
      createZusatzAbotyp(meta, id, zusatzabotyp)
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
    case EntityInsertedEvent(meta, id: AboId, zusatzAbo: ZusatzAboCreate) =>
      createZusatzAbo(meta, id, zusatzAbo)
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
    case EntityInsertedEvent(meta, id: KontoDatenId, kontoDaten: KontoDatenModify) =>
      createKontoDaten(meta, id, kontoDaten)
    case EntityInsertedEvent(meta, id: AbwesenheitId, abw: AbwesenheitCreate) =>
      createAbwesenheit(meta, id, abw)
    case EntityInsertedEvent(meta, id: LieferplanungId, lieferplanungCreateData: LieferplanungCreate) =>
      createLieferplanung(meta, id, lieferplanungCreateData)
    case EntityInsertedEvent(meta, id: LieferungId, entity: LieferungPlanungAdd) =>
      addLieferungToPlanung(meta, id, entity)
    case EntityInsertedEvent(meta, id: SammelbestellungId, entity: SammelbestellungModify) =>
      createSammelbestellungen(meta, id, entity)
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      //create abotyp
      stammdatenWriteRepository.insertEntity[Abotyp, AbotypId](typ)
    }
  }

  def createZusatzAbotyp(meta: EventMetadata, id: AbotypId, zusatzabotyp: ZusatzAbotypModify)(implicit personId: PersonId = meta.originator) = {
    val typ = copyTo[ZusatzAbotypModify, ZusatzAbotyp](
      zusatzabotyp,
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      //create abotyp
      stammdatenWriteRepository.insertEntity[ZusatzAbotyp, AbotypId](typ)
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.insertEntity[Postlieferung, VertriebsartId](insert)
    }
  }

  def createLieferung(meta: EventMetadata, id: LieferungId, lieferung: LieferungAbotypCreate)(implicit personId: PersonId = meta.originator): Option[Lieferung] = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.getAbotypDetail(lieferung.abotypId) match {
        case Some(abotyp) =>
          (stammdatenWriteRepository.getById(vertriebMapping, lieferung.vertriebId) map {
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

              stammdatenWriteRepository.insertEntity[Lieferung, LieferungId](insert)
          }).flatten
        case _ =>
          logger.error(s"Abotyp with id ${lieferung.abotypId} not found.")
          None
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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

    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.insertEntity[Person, PersonId](person)
    }
  }

  def createPendenz(meta: EventMetadata, id: PendenzId, create: PendenzCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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

  private def aboById(aboId: AboId)(implicit session: DBSession): Option[Abo] = {
    stammdatenWriteRepository.getById(depotlieferungAboMapping, aboId) orElse
      stammdatenWriteRepository.getById(heimlieferungAboMapping, aboId) orElse
      stammdatenWriteRepository.getById(postlieferungAboMapping, aboId)
  }

  private def zusatzAboTypById(zusatzAbotypId: AbotypId)(implicit session: DBSession): Option[ZusatzAbotyp] = {
    stammdatenWriteRepository.getById(zusatzAbotypMapping, zusatzAbotypId)
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
    DB localTxPostPublish { implicit session => implicit publisher =>
      val emptyMap: TreeMap[String, Int] = TreeMap()
      abotypByVertriebartId(create.vertriebsartId) map {
        case (vertriebsart, vertrieb, abotyp) =>
          aboParameters(create)(abotyp) match {
            case (guthaben, ende, aktiv) =>
              val maybeAbo: Option[Abo] = create match {
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
                  )) map { heimlieferungAbo =>
                    // create the corresponding tourlieferung as well
                    stammdatenWriteRepository.getById(kundeMapping, heimlieferungAbo.kundeId) map { kunde =>
                      stammdatenWriteRepository.insertEntity[Tourlieferung, AboId](Tourlieferung(heimlieferungAbo, kunde, personId))
                    }
                    heimlieferungAbo
                  }
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

              // create required Koerbe for abo
              maybeAbo map (abo => modifyKoerbeForAboDatumChange(abo, None))
          }
      }
    }
  }

  def createZusatzAbo(meta: EventMetadata, newId: AboId, create: ZusatzAboCreate)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val hauptAbo = aboById(create.hauptAboId)
      val zusatzAbotyp = zusatzAboTypById(create.abotypId)
      hauptAbo match {
        case Some(h) => {
          zusatzAbotyp match {
            case Some(z) => {
              val startDate = defaultDateStart(h.start, z.aktivVon)
              val endDate = defaultDateEnd(h.ende, z.aktivBis)
              val zusatzAbo = copyTo[ZusatzAboCreate, ZusatzAbo](
                create,
                "id" -> newId,
                "hauptAbotypId" -> h.abotypId,
                "kunde" -> h.kunde,
                "vertriebsartId" -> h.vertriebsartId,
                "vertriebId" -> h.vertriebId,
                "vertriebBeschrieb" -> h.vertriebBeschrieb,
                "abotypName" -> z.name,
                "start" -> startDate,
                "ende" -> endDate,
                "guthabenVertraglich" -> h.guthabenVertraglich,
                "guthaben" -> h.guthaben,
                "guthabenInRechnung" -> h.guthabenInRechnung,
                "letzteLieferung" -> h.letzteLieferung,
                "anzahlAbwesenheiten" -> h.anzahlAbwesenheiten,
                "anzahlLieferungen" -> h.anzahlLieferungen,
                "aktiv" -> h.aktiv,
                "erstelldat" -> meta.timestamp,
                "ersteller" -> meta.originator,
                "modifidat" -> meta.timestamp,
                "modifikator" -> meta.originator
              )
              DB autoCommitSinglePublish { implicit session => implicit publisher =>
                stammdatenWriteRepository.insertEntity[ZusatzAbo, AboId](zusatzAbo)
              }
            }
            case None => throw new RuntimeException("The id provided does not corresponde to any zusatzabotyp");
          }
        }
        case None => throw new RuntimeException("The id provided does not corresponde to any zusatzabo");
      }
    }
  }

  def defaultDateStart(date1: LocalDate, date2: Option[LocalDate]): LocalDate = {
    (date1, date2) match {
      case (d1, None) => d1
      case (d1, Some(d2)) if (d1 compareTo d2) > 0 => d1
      case (_, date2) => date2.get
    }
  }

  def defaultDateEnd(date1: Option[LocalDate], date2: Option[LocalDate]): Option[LocalDate] = {
    (date1, date2) match {
      case (Some(d1), None) => date1
      case (None, Some(d2)) => date2
      case (Some(d1), Some(d2)) if (d1 compareTo d2) > 0 => date1
      case (_, d2) => d2
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.insertEntity[Projekt, ProjektId](projekt)
    }
  }

  def createKontoDaten(meta: EventMetadata, id: KontoDatenId, create: KontoDatenModify)(implicit personId: PersonId = meta.originator) = {
    val kontoDaten = copyTo[KontoDatenModify, KontoDaten](
      create,
      "id" -> id,
      "erstelldat" -> meta.timestamp,
      "ersteller" -> meta.originator,
      "modifidat" -> meta.timestamp,
      "modifikator" -> meta.originator
    )
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.insertEntity[KontoDaten, KontoDatenId](kontoDaten)
    }
  }

  def createAbwesenheit(meta: EventMetadata, id: AbwesenheitId, create: AbwesenheitCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.countAbwesend(create.lieferungId, create.aboId) match {
        case Some(0) =>
          val abw = copyTo[AbwesenheitCreate, Abwesenheit](
            create,
            "id" -> id,
            "erstelldat" -> meta.timestamp,
            "ersteller" -> meta.originator,
            "modifidat" -> meta.timestamp,
            "modifikator" -> meta.originator
          )
          stammdatenWriteRepository.insertEntity[Abwesenheit, AbwesenheitId](abw)
        case _ =>
          logger.debug("Eine Abwesenheit kann nur einmal erfasst werden")
      }
    }
  }

  def createLieferplanung(meta: EventMetadata, lieferplanungId: LieferplanungId, lieferplanung: LieferplanungCreate)(implicit personId: PersonId = meta.originator) = {
    println(s"\nXXXXXXXXXXXXX createLieferplanung XXXXXXXXXXXXXXXXXXXXXXXX")
    DB localTxPostPublish { implicit session => implicit publisher =>
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
      val project = stammdatenWriteRepository.getProjekt

      //create lieferplanung
      stammdatenWriteRepository.insertEntity[Lieferplanung, LieferplanungId](insert) map { lieferplanung =>
        println(s"\nXXXXXXXXXXXXX createLieferplanung:insertEntity XXXXXXXXXXXXXXXXXXXXXXXX")
        //alle nächsten Lieferungen alle Abotypen (wenn Flag es erlaubt)
        val abotypDepotTour = stammdatenWriteRepository.getLieferungenNext() map { lieferung =>
          println(s"\nXXXXXXXXXXXXX createLieferplanung:next XXXXXXXXXXXXXXXXXXXXXXXX")
          logger.debug("createLieferplanung: Lieferung " + lieferung.id + ": " + lieferung)

          val adjustedLieferung: Lieferung = updateLieferungUndZusatzLieferung(meta, lieferplanungId, project, lieferung)

          (dateFormat.print(adjustedLieferung.datum), adjustedLieferung.abotypBeschrieb)
        }

        val abotypDates = (abotypDepotTour.groupBy(_._1).mapValues(_ map { _._2 }) map {
          case (datum, abotypBeschrieb) =>
            datum + ": " + abotypBeschrieb.mkString(", ")
        }).mkString("; ")

        //update lieferplanung
        stammdatenWriteRepository.updateEntity[Lieferplanung, LieferplanungId](lieferplanung.id)(
          lieferplanungMapping.column.abotypDepotTour -> abotypDates
        )
      }
    }

    logger.debug(s"Lieferplanung created, notify clients:$lieferplanungId")
    stammdatenWriteRepository.publish(DataEvent(personId, LieferplanungCreated(lieferplanungId)))
  }

  def createKoerbe(lieferung: Lieferung)(implicit personId: PersonId, session: DBSession, publisher: EventPublisher): Lieferung = {
    logger.debug(s"Create Koerbe:${lieferung.id}")
    val ret: Option[Option[Lieferung]] = stammdatenWriteRepository.getById(abotypMapping, lieferung.abotypId) map { (abotyp: Abotyp) =>
      lieferung.lieferplanungId.map { lieferplanungId =>
        val abos: List[Abo] = stammdatenWriteRepository.getAktiveAbos(lieferung.abotypId, lieferung.vertriebId, lieferung.datum, lieferplanungId)
        val koerbe: List[(Option[Korb], Option[Korb])] = abos map { abo =>
          upsertKorb(lieferung, abo, abotyp)
        }
        recalculateNumbersLieferung(lieferung)

      }
    }
    ret.flatten.getOrElse(lieferung)
  }

  // TODO zusammenführen mit createLieferplanung!!!
  def addLieferungToPlanung(meta: EventMetadata, id: LieferungId, lieferungPlanungAdd: LieferungPlanungAdd)(implicit personId: PersonId = meta.originator) = {
    DB localTxPostPublish { implicit session => implicit publisher =>
      val project = stammdatenWriteRepository.getProjekt
      stammdatenWriteRepository.getById(lieferplanungMapping, lieferungPlanungAdd.lieferplanungId) map { lieferplanung =>
        stammdatenWriteRepository.getById(lieferungMapping, lieferungPlanungAdd.id) map { lieferung =>
          updateLieferungUndZusatzLieferung(meta, lieferungPlanungAdd.lieferplanungId, project, lieferung)
        }
      }
    }
  }

  private def updateLieferungUndZusatzLieferung(meta: EventMetadata, lieferplanungId: LieferplanungId, project: Option[Projekt], lieferung: Lieferung)(implicit personId: PersonId = meta.originator, session: DBSession, publisher: EventPublisher): Lieferung = {

    println(s"XXXXXXXXXXXX updateLieferungUndZusatzLieferung XXXXXXXXXXXXXXXXXXXXXX")

    val adjustedLieferung = updateLieferung(meta, lieferplanungId, project, lieferung)

    stammdatenWriteRepository.getExistingZusatzAbotypen(adjustedLieferung.id).map { zTyp =>

      println(s"XXXXXXXXXXXX existingZusatzAbotypen: $zTyp XXXXXXXXXXXXXXXXXXXXXX")

      val asdf = stammdatenWriteRepository.getExistingZusatzaboLieferung(zTyp.id, lieferplanungId)
      asdf match {
        case None => {
          createLieferung(meta, LieferungId(DateTime.now().getMillis()), LieferungAbotypCreate(zTyp.id, adjustedLieferung.vertriebId, adjustedLieferung.datum)).map { zusatzLieferung =>
            val updatedLieferung: Lieferung = updateLieferung(meta, lieferplanungId, project, zusatzLieferung)
            addLieferungToPlanung(meta, updatedLieferung.id, LieferungPlanungAdd(updatedLieferung.id, lieferplanungId))
          }
        }
        case _ => //macht nichts
      }
    }

    adjustedLieferung
  }

  private def updateLieferung(meta: EventMetadata, lieferplanungId: LieferplanungId, project: Option[Projekt], lieferung: Lieferung)(implicit personId: PersonId = meta.originator, session: DBSession, publisher: EventPublisher): Lieferung = {
    val (newDurchschnittspreis, newAnzahlLieferungen) = stammdatenWriteRepository.getGeplanteLieferungVorher(lieferung.vertriebId, lieferung.datum) match {
      case Some(lieferungVorher) if project.get.geschaftsjahr.isInSame(lieferungVorher.datum.toLocalDate(), lieferung.datum.toLocalDate()) =>
        val sum = stammdatenWriteRepository.sumPreisTotalGeplanteLieferungenVorher(lieferung.vertriebId, lieferung.datum, project.get.geschaftsjahr.start(lieferung.datum.toLocalDate()).toDateTimeAtCurrentTime()).getOrElse(BigDecimal(0))

        val durchschnittspreisBisher: BigDecimal = lieferungVorher.anzahlLieferungen match {
          case 0 => BigDecimal(0)
          case _ => sum / lieferungVorher.anzahlLieferungen
        }
        val anzahlLieferungenNeu = lieferungVorher.anzahlLieferungen + 1
        (durchschnittspreisBisher, anzahlLieferungenNeu)
      case _ =>
        (BigDecimal(0), 1)
    }

    val updatedLieferung = lieferung.copy(
      lieferplanungId = Some(lieferplanungId),
      status = Offen,
      durchschnittspreis = newDurchschnittspreis,
      anzahlLieferungen = newAnzahlLieferungen,
      modifidat = meta.timestamp,
      modifikator = personId
    )

    //create koerbe
    val adjustedLieferung = createKoerbe(updatedLieferung)

    //update Lieferung
    stammdatenWriteRepository.updateEntity[Lieferung, LieferungId](adjustedLieferung.id)(
      lieferungMapping.column.lieferplanungId -> adjustedLieferung.lieferplanungId,
      lieferungMapping.column.status -> adjustedLieferung.status,
      lieferungMapping.column.durchschnittspreis -> adjustedLieferung.durchschnittspreis,
      lieferungMapping.column.anzahlLieferungen -> adjustedLieferung.anzahlLieferungen,
      lieferungMapping.column.anzahlKoerbeZuLiefern -> adjustedLieferung.anzahlKoerbeZuLiefern,
      lieferungMapping.column.anzahlAbwesenheiten -> adjustedLieferung.anzahlAbwesenheiten,
      lieferungMapping.column.anzahlSaldoZuTief -> adjustedLieferung.anzahlSaldoZuTief
    )

    adjustedLieferung
  }

  def createSammelbestellungen(meta: EventMetadata, id: SammelbestellungId, create: SammelbestellungModify)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      createOrUpdateSammelbestellungen(id, create)
    }
  }

  def createProjektVorlage(meta: EventMetadata, id: ProjektVorlageId, create: ProjektVorlageCreate)(implicit personId: PersonId = meta.originator) = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
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
