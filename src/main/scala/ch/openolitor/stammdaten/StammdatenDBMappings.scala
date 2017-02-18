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

import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.core.models.VorlageTyp
import ch.openolitor.core.repositories.BaseRepository
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.core.repositories.ParameterBinderMapping
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.stammdaten.models.PendenzStatus
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import scala.collection.immutable.TreeMap
import ch.openolitor.core.filestore.VorlageRechnung

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging {
  import TypeBinder._

  val fristeinheitPattern = """(\d+)(M|W)""".r

  // DB type binders for read operations
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder(TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder(DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder(AbotypId.apply _)
  implicit val vertriebIdBinder: TypeBinder[VertriebId] = baseIdTypeBinder(VertriebId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder(VertriebsartId.apply _)
  implicit val vertriebsartIdSetBinder: TypeBinder[Set[VertriebsartId]] = string.map(s => s.split(",").map(_.toLong).map(VertriebsartId.apply _).toSet)
  implicit val vertriebsartIdSeqBinder: TypeBinder[Seq[VertriebsartId]] = string.map(s => if (s != null) s.split(",").map(_.toLong).map(VertriebsartId.apply _).toSeq else Nil)
  implicit val kundeIdBinder: TypeBinder[KundeId] = baseIdTypeBinder(KundeId.apply _)
  implicit val pendenzIdBinder: TypeBinder[PendenzId] = baseIdTypeBinder(PendenzId.apply _)
  implicit val aboIdBinder: TypeBinder[AboId] = baseIdTypeBinder(AboId.apply _)
  implicit val lierferungIdBinder: TypeBinder[LieferungId] = baseIdTypeBinder(LieferungId.apply _)
  implicit val lieferplanungIdBinder: TypeBinder[LieferplanungId] = baseIdTypeBinder(LieferplanungId.apply _)
  implicit val optionLieferplanungIdBinder: TypeBinder[Option[LieferplanungId]] = optionBaseIdTypeBinder(LieferplanungId.apply _)
  implicit val lieferpositionIdBinder: TypeBinder[LieferpositionId] = baseIdTypeBinder(LieferpositionId.apply _)
  implicit val bestellungIdBinder: TypeBinder[BestellungId] = baseIdTypeBinder(BestellungId.apply _)
  implicit val sammelbestellungIdBinder: TypeBinder[SammelbestellungId] = baseIdTypeBinder(SammelbestellungId.apply _)
  implicit val bestellpositionIdBinder: TypeBinder[BestellpositionId] = baseIdTypeBinder(BestellpositionId.apply _)
  implicit val customKundentypIdBinder: TypeBinder[CustomKundentypId] = baseIdTypeBinder(CustomKundentypId.apply _)
  implicit val kundentypIdBinder: TypeBinder[KundentypId] = string.map(KundentypId)
  implicit val produktekategorieIdBinder: TypeBinder[ProduktekategorieId] = baseIdTypeBinder(ProduktekategorieId.apply _)
  implicit val baseProduktekategorieIdBinder: TypeBinder[BaseProduktekategorieId] = string.map(BaseProduktekategorieId)
  implicit val produktIdBinder: TypeBinder[ProduktId] = baseIdTypeBinder(ProduktId.apply _)
  implicit val produzentIdBinder: TypeBinder[ProduzentId] = baseIdTypeBinder(ProduzentId.apply _)
  implicit val baseProduzentIdBinder: TypeBinder[BaseProduzentId] = string.map(BaseProduzentId)
  implicit val projektIdBinder: TypeBinder[ProjektId] = baseIdTypeBinder(ProjektId.apply _)
  implicit val produktProduzentIdBinder: TypeBinder[ProduktProduzentId] = baseIdTypeBinder(ProduktProduzentId.apply _)
  implicit val produktProduktekategorieIdBinder: TypeBinder[ProduktProduktekategorieId] = baseIdTypeBinder(ProduktProduktekategorieId.apply _)
  implicit val abwesenheitIdBinder: TypeBinder[AbwesenheitId] = baseIdTypeBinder(AbwesenheitId.apply _)
  implicit val korbIdBinder: TypeBinder[KorbId] = baseIdTypeBinder(KorbId.apply _)
  implicit val auslieferungIdBinder: TypeBinder[AuslieferungId] = baseIdTypeBinder(AuslieferungId.apply _)
  implicit val projektVorlageIdBinder: TypeBinder[ProjektVorlageId] = baseIdTypeBinder(ProjektVorlageId.apply _)
  implicit val optionAuslieferungIdBinder: TypeBinder[Option[AuslieferungId]] = optionBaseIdTypeBinder(AuslieferungId.apply _)
  implicit val einladungIdBinder: TypeBinder[EinladungId] = baseIdTypeBinder(EinladungId.apply _)

  implicit val pendenzStatusTypeBinder: TypeBinder[PendenzStatus] = string.map(PendenzStatus.apply)
  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val lieferungStatusTypeBinder: TypeBinder[LieferungStatus] = string.map(LieferungStatus.apply)
  implicit val korbStatusTypeBinder: TypeBinder[KorbStatus] = string.map(KorbStatus.apply)
  implicit val auslieferungStatusTypeBinder: TypeBinder[AuslieferungStatus] = string.map(AuslieferungStatus.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetTypeBinder: TypeBinder[Set[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply).toSet)
  implicit val kundenTypIdSetBinder: TypeBinder[Set[KundentypId]] = string.map(s => s.split(",").map(KundentypId.apply).toSet)
  implicit val laufzeiteinheitTypeBinder: TypeBinder[Laufzeiteinheit] = string.map(Laufzeiteinheit.apply)
  implicit val liefereinheitypeBinder: TypeBinder[Liefereinheit] = string.map(Liefereinheit.apply)
  implicit val liefersaisonTypeBinder: TypeBinder[Liefersaison] = string.map(Liefersaison.apply)
  implicit val vorlageTypeTypeBinder: TypeBinder[VorlageTyp] = string.map(VorlageTyp.apply)
  implicit val anredeTypeBinder: TypeBinder[Option[Anrede]] = string.map(Anrede.apply)
  implicit val fristOptionTypeBinder: TypeBinder[Option[Frist]] = string.map {
    case fristeinheitPattern(wert, "W") => Some(Frist(wert.toInt, Wochenfrist))
    case fristeinheitPattern(wert, "M") => Some(Frist(wert.toInt, Monatsfrist))
    case _ => None
  }

  implicit val baseProduktekategorieIdSetTypeBinder: TypeBinder[Set[BaseProduktekategorieId]] = string.map(s => s.split(",").map(BaseProduktekategorieId.apply).toSet)
  implicit val baseProduzentIdSetTypeBinder: TypeBinder[Set[BaseProduzentId]] = string.map(s => s.split(",").map(BaseProduzentId.apply).toSet)
  implicit val stringIntTreeMapTypeBinder: TypeBinder[TreeMap[String, Int]] = treeMapTypeBinder(identity, _.toInt)
  implicit val stringBigDecimalTreeMapTypeBinder: TypeBinder[TreeMap[String, BigDecimal]] = treeMapTypeBinder(identity, BigDecimal(_))
  implicit val rolleMapTypeBinder: TypeBinder[Map[Rolle, Boolean]] = mapTypeBinder(r => Rolle(r).getOrElse(KundenZugang), _.toBoolean)
  implicit val rolleTypeBinder: TypeBinder[Option[Rolle]] = string.map(Rolle.apply)

  implicit val stringSeqTypeBinder: TypeBinder[Seq[String]] = string.map(s => if (s != null) s.split(",").toSeq else Nil)
  implicit val stringSetTypeBinder: TypeBinder[Set[String]] = string.map(s => if (s != null) s.split(",").toSet else Set())

  //DB parameter binders for write and query operationsit
  implicit val pendenzStatusBinder = toStringSqlBinder[PendenzStatus]
  implicit val rhytmusSqlBinder = toStringSqlBinder[Rhythmus]
  implicit val preiseinheitSqlBinder = toStringSqlBinder[Preiseinheit]
  implicit val waehrungSqlBinder = toStringSqlBinder[Waehrung]
  implicit val lieferungStatusSqlBinder = toStringSqlBinder[LieferungStatus]
  implicit val korbStatusSqlBinder = toStringSqlBinder[KorbStatus]
  implicit val auslieferungStatusSqlBinder = toStringSqlBinder[AuslieferungStatus]
  implicit val lieferzeitpunktSqlBinder = toStringSqlBinder[Lieferzeitpunkt]
  implicit val lieferzeitpunktSetSqlBinder = setSqlBinder[Lieferzeitpunkt]
  implicit val laufzeiteinheitSqlBinder = toStringSqlBinder[Laufzeiteinheit]
  implicit val liefereinheitSqlBinder = toStringSqlBinder[Liefereinheit]
  implicit val liefersaisonSqlBinder = toStringSqlBinder[Liefersaison]
  implicit val anredeSqlBinder = toStringSqlBinder[Anrede]
  implicit val optionAnredeSqlBinder = optionSqlBinder[Anrede]
  implicit val vorlageTypeSqlBinder = toStringSqlBinder[VorlageTyp]

  implicit val abotypIdSqlBinder = baseIdSqlBinder[AbotypId]
  implicit val depotIdSqlBinder = baseIdSqlBinder[DepotId]
  implicit val tourIdSqlBinder = baseIdSqlBinder[TourId]
  implicit val vertriebIdSqlBinder = baseIdSqlBinder[VertriebId]
  implicit val vertriebsartIdSqlBinder = baseIdSqlBinder[VertriebsartId]
  implicit val vertriebsartIdSetSqlBinder = setSqlBinder[VertriebsartId]
  implicit val vertriebsartIdSeqSqlBinder = seqSqlBinder[VertriebsartId]
  implicit val kundeIdSqlBinder = baseIdSqlBinder[KundeId]
  implicit val pendenzIdSqlBinder = baseIdSqlBinder[PendenzId]
  implicit val customKundentypIdSqlBinder = baseIdSqlBinder[CustomKundentypId]
  implicit val kundentypIdSqlBinder = new SqlBinder[KundentypId] { def apply(value: KundentypId): Any = value.id }
  implicit val kundentypIdSetSqlBinder = setSqlBinder[KundentypId]
  implicit val aboIdSqlBinder = baseIdSqlBinder[AboId]
  implicit val lieferungIdSqlBinder = baseIdSqlBinder[LieferungId]
  implicit val lieferplanungIdSqlBinder = baseIdSqlBinder[LieferplanungId]
  implicit val lieferpositionIdSqlBinder = baseIdSqlBinder[LieferpositionId]
  implicit val bestellungIdSqlBinder = baseIdSqlBinder[BestellungId]
  implicit val sammelbestellungIdSqlBinder = baseIdSqlBinder[SammelbestellungId]
  implicit val bestellpositionIdSqlBinder = baseIdSqlBinder[BestellpositionId]
  implicit val korbIdSqlBinder = baseIdSqlBinder[KorbId]
  implicit val auslieferungIdSqlBinder = baseIdSqlBinder[AuslieferungId]
  implicit val projektVorlageIdSqlBinder = baseIdSqlBinder[ProjektVorlageId]
  implicit val auslieferungIdOptionSqlBinder = optionSqlBinder[AuslieferungId]
  implicit val produktIdSqlBinder = baseIdSqlBinder[ProduktId]
  implicit val produktIdOptionBinder = optionSqlBinder[ProduktId]
  implicit val produktekategorieIdSqlBinder = baseIdSqlBinder[ProduktekategorieId]
  implicit val baseProduktekategorieIdSqlBinder = new SqlBinder[BaseProduktekategorieId] { def apply(value: BaseProduktekategorieId): Any = value.id }
  implicit val baseProduktekategorieIdSetSqlBinder = setSqlBinder[BaseProduktekategorieId]
  implicit val produzentIdSqlBinder = baseIdSqlBinder[ProduzentId]
  implicit val baseProduzentIdSqlBinder = new SqlBinder[BaseProduzentId] { def apply(value: BaseProduzentId): Any = value.id }
  implicit val baseProduzentIdSetSqlBinder = setSqlBinder[BaseProduzentId]
  implicit val projektIdSqlBinder = baseIdSqlBinder[ProjektId]
  implicit val abwesenheitIdSqlBinder = baseIdSqlBinder[AbwesenheitId]
  implicit val produktProduzentIdIdSqlBinder = baseIdSqlBinder[ProduktProduzentId]
  implicit val produktProduktekategorieIdIdSqlBinder = baseIdSqlBinder[ProduktProduktekategorieId]
  implicit val lieferplanungIdOptionBinder = optionSqlBinder[LieferplanungId]
  implicit val einladungIdSqlBinder = baseIdSqlBinder[EinladungId]

  implicit val stringIntTreeMapSqlBinder = treeMapSqlBinder[String, Int]
  implicit val stringBigDecimalTreeMapSqlBinder = treeMapSqlBinder[String, BigDecimal]
  implicit val rolleSqlBinder = toStringSqlBinder[Rolle]
  implicit val optionRolleSqlBinder = optionSqlBinder[Rolle]
  implicit val rolleMapSqlBinder = mapSqlBinder[Rolle, Boolean]
  implicit val fristeSqlBinder = new SqlBinder[Frist] {
    def apply(frist: Frist): Any = {
      val einheit = frist.einheit match {
        case Wochenfrist => "W"
        case Monatsfrist => "M"
      }
      s"${frist.wert}$einheit"
    }
  }
  implicit val fristOptionSqlBinder = optionSqlBinder[Frist]

  implicit val stringSeqSqlBinder = seqSqlBinder[String]
  implicit val stringSetSqlBinder = setSqlBinder[String]

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[Any] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(abotyp: Abotyp) = {
      super.updateParameters(abotyp) ++ Seq(
        column.name -> parameter(abotyp.name),
        column.beschreibung -> parameter(abotyp.beschreibung),
        column.lieferrhythmus -> parameter(abotyp.lieferrhythmus),
        column.aktivVon -> parameter(abotyp.aktivVon),
        column.aktivBis -> parameter(abotyp.aktivBis),
        column.preis -> parameter(abotyp.preis),
        column.preiseinheit -> parameter(abotyp.preiseinheit),
        column.laufzeit -> parameter(abotyp.laufzeit),
        column.laufzeiteinheit -> parameter(abotyp.laufzeiteinheit),
        column.vertragslaufzeit -> parameter(abotyp.vertragslaufzeit),
        column.kuendigungsfrist -> parameter(abotyp.kuendigungsfrist),
        column.anzahlAbwesenheiten -> parameter(abotyp.anzahlAbwesenheiten),
        column.farbCode -> parameter(abotyp.farbCode),
        column.zielpreis -> parameter(abotyp.zielpreis),
        column.guthabenMindestbestand -> parameter(abotyp.guthabenMindestbestand),
        column.adminProzente -> parameter(abotyp.adminProzente),
        column.wirdGeplant -> parameter(abotyp.wirdGeplant),
        column.anzahlAbonnenten -> parameter(abotyp.anzahlAbonnenten),
        column.anzahlAbonnentenAktiv -> parameter(abotyp.anzahlAbonnentenAktiv),
        column.letzteLieferung -> parameter(abotyp.letzteLieferung),
        column.waehrung -> parameter(abotyp.waehrung)
      )
    }
  }

  implicit val customKundentypMapping = new BaseEntitySQLSyntaxSupport[CustomKundentyp] {
    override val tableName = "Kundentyp"

    override lazy val columns = autoColumns[CustomKundentyp]()

    def apply(rn: ResultName[CustomKundentyp])(rs: WrappedResultSet): CustomKundentyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: CustomKundentyp): Seq[Any] =
      parameters(CustomKundentyp.unapply(entity).get)

    override def updateParameters(typ: CustomKundentyp) = {
      super.updateParameters(typ) ++ Seq(
        column.kundentyp -> parameter(typ.kundentyp),
        column.beschreibung -> parameter(typ.beschreibung),
        column.anzahlVerknuepfungen -> parameter(typ.anzahlVerknuepfungen)
      )
    }
  }

  implicit val kundeMapping = new BaseEntitySQLSyntaxSupport[Kunde] {
    override val tableName = "Kunde"

    override lazy val columns = autoColumns[Kunde]()

    def apply(rn: ResultName[Kunde])(rs: WrappedResultSet): Kunde =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Kunde): Seq[Any] =
      parameters(Kunde.unapply(entity).get)

    override def updateParameters(kunde: Kunde) = {
      super.updateParameters(kunde) ++ Seq(
        column.bezeichnung -> parameter(kunde.bezeichnung),
        column.strasse -> parameter(kunde.strasse),
        column.hausNummer -> parameter(kunde.hausNummer),
        column.adressZusatz -> parameter(kunde.adressZusatz),
        column.plz -> parameter(kunde.plz),
        column.ort -> parameter(kunde.ort),
        column.abweichendeLieferadresse -> parameter(kunde.abweichendeLieferadresse),
        column.bezeichnungLieferung -> parameter(kunde.bezeichnungLieferung),
        column.strasseLieferung -> parameter(kunde.strasseLieferung),
        column.hausNummerLieferung -> parameter(kunde.hausNummerLieferung),
        column.plzLieferung -> parameter(kunde.plzLieferung),
        column.ortLieferung -> parameter(kunde.ortLieferung),
        column.adressZusatzLieferung -> parameter(kunde.adressZusatzLieferung),
        column.zusatzinfoLieferung -> parameter(kunde.zusatzinfoLieferung),
        column.typen -> parameter(kunde.typen),
        column.bemerkungen -> parameter(kunde.bemerkungen),
        column.anzahlAbos -> parameter(kunde.anzahlAbos),
        column.anzahlAbosAktiv -> parameter(kunde.anzahlAbosAktiv),
        column.anzahlPendenzen -> parameter(kunde.anzahlPendenzen),
        column.anzahlPersonen -> parameter(kunde.anzahlPersonen)
      )
    }
  }

  implicit val personMapping = new BaseEntitySQLSyntaxSupport[Person] {
    override val tableName = "Person"

    override lazy val columns = autoColumns[Person]()

    def apply(rn: ResultName[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Person): Seq[Any] =
      parameters(Person.unapply(entity).get)

    override def updateParameters(person: Person) = {
      super.updateParameters(person) ++ Seq(
        column.kundeId -> parameter(person.kundeId),
        column.anrede -> parameter(person.anrede),
        column.name -> parameter(person.name),
        column.vorname -> parameter(person.vorname),
        column.email -> parameter(person.email),
        column.emailAlternative -> parameter(person.emailAlternative),
        column.telefonMobil -> parameter(person.telefonMobil),
        column.telefonFestnetz -> parameter(person.telefonFestnetz),
        column.bemerkungen -> parameter(person.bemerkungen),
        column.sort -> parameter(person.sort),
        column.loginAktiv -> parameter(person.loginAktiv),
        column.passwort -> parameter(person.passwort),
        column.passwortWechselErforderlich -> parameter(person.passwortWechselErforderlich),
        column.rolle -> parameter(person.rolle),
        column.letzteAnmeldung -> parameter(person.letzteAnmeldung)
      )
    }
  }

  implicit val pendenzMapping = new BaseEntitySQLSyntaxSupport[Pendenz] {
    override val tableName = "Pendenz"

    override lazy val columns = autoColumns[Pendenz]()

    def apply(rn: ResultName[Pendenz])(rs: WrappedResultSet): Pendenz =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Pendenz): Seq[Any] =
      parameters(Pendenz.unapply(entity).get)

    override def updateParameters(pendenz: Pendenz) = {
      super.updateParameters(pendenz) ++ Seq(
        column.kundeId -> parameter(pendenz.kundeId),
        column.kundeBezeichnung -> parameter(pendenz.kundeBezeichnung),
        column.datum -> parameter(pendenz.datum),
        column.bemerkung -> parameter(pendenz.bemerkung),
        column.status -> parameter(pendenz.status),
        column.generiert -> parameter(pendenz.generiert)
      )
    }
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[Any] =
      parameters(Lieferung.unapply(entity).get)

    override def updateParameters(lieferung: Lieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.abotypId -> parameter(lieferung.abotypId),
        column.abotypBeschrieb -> parameter(lieferung.abotypBeschrieb),
        column.vertriebId -> parameter(lieferung.vertriebId),
        column.vertriebBeschrieb -> parameter(lieferung.vertriebBeschrieb),
        column.status -> parameter(lieferung.status),
        column.datum -> parameter(lieferung.datum),
        column.durchschnittspreis -> parameter(lieferung.durchschnittspreis),
        column.anzahlLieferungen -> parameter(lieferung.anzahlLieferungen),
        column.anzahlKoerbeZuLiefern -> parameter(lieferung.anzahlKoerbeZuLiefern),
        column.anzahlAbwesenheiten -> parameter(lieferung.anzahlAbwesenheiten),
        column.anzahlSaldoZuTief -> parameter(lieferung.anzahlSaldoZuTief),
        column.zielpreis -> parameter(lieferung.zielpreis),
        column.preisTotal -> parameter(lieferung.preisTotal),
        column.lieferplanungId -> parameter(lieferung.lieferplanungId)
      )
    }
  }

  implicit val lieferplanungMapping = new BaseEntitySQLSyntaxSupport[Lieferplanung] {
    override val tableName = "Lieferplanung"

    override lazy val columns = autoColumns[Lieferplanung]()

    def apply(rn: ResultName[Lieferplanung])(rs: WrappedResultSet): Lieferplanung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferplanung): Seq[Any] = parameters(Lieferplanung.unapply(entity).get)

    override def updateParameters(lieferplanung: Lieferplanung) = {
      super.updateParameters(lieferplanung) ++ Seq(
        column.bemerkungen -> parameter(lieferplanung.bemerkungen),
        column.abotypDepotTour -> parameter(lieferplanung.abotypDepotTour),
        column.status -> parameter(lieferplanung.status)
      )
    }
  }

  implicit val lieferpositionMapping = new BaseEntitySQLSyntaxSupport[Lieferposition] {
    override val tableName = "Lieferposition"

    override lazy val columns = autoColumns[Lieferposition]()

    def apply(rn: ResultName[Lieferposition])(rs: WrappedResultSet): Lieferposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferposition): Seq[Any] = parameters(Lieferposition.unapply(entity).get)

    override def updateParameters(lieferposition: Lieferposition) = {
      super.updateParameters(lieferposition) ++ Seq(
        column.produktId -> parameter(lieferposition.produktId),
        column.produktBeschrieb -> parameter(lieferposition.produktBeschrieb),
        column.produzentId -> parameter(lieferposition.produzentId),
        column.produzentKurzzeichen -> parameter(lieferposition.produzentKurzzeichen),
        column.preisEinheit -> parameter(lieferposition.preisEinheit),
        column.einheit -> parameter(lieferposition.einheit),
        column.menge -> parameter(lieferposition.menge),
        column.preis -> parameter(lieferposition.preis),
        column.anzahl -> parameter(lieferposition.anzahl)
      )
    }
  }

  implicit val sammelbestellungMapping = new BaseEntitySQLSyntaxSupport[Sammelbestellung] {
    override val tableName = "Sammelbestellung"

    override lazy val columns = autoColumns[Sammelbestellung]()

    def apply(rn: ResultName[Sammelbestellung])(rs: WrappedResultSet): Sammelbestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Sammelbestellung): Seq[Any] = parameters(Sammelbestellung.unapply(entity).get)

    override def updateParameters(sammelbestellung: Sammelbestellung) = {
      super.updateParameters(sammelbestellung) ++ Seq(
        column.produzentId -> parameter(sammelbestellung.produzentId),
        column.produzentKurzzeichen -> parameter(sammelbestellung.produzentKurzzeichen),
        column.lieferplanungId -> parameter(sammelbestellung.lieferplanungId),
        column.status -> parameter(sammelbestellung.status),
        column.datum -> parameter(sammelbestellung.datum),
        column.datumAbrechnung -> parameter(sammelbestellung.datumAbrechnung),
        column.preisTotal -> parameter(sammelbestellung.preisTotal),
        column.steuerSatz -> parameter(sammelbestellung.steuerSatz),
        column.steuer -> parameter(sammelbestellung.steuer),
        column.totalSteuer -> parameter(sammelbestellung.totalSteuer),
        column.datumVersendet -> parameter(sammelbestellung.datumVersendet)
      )
    }
  }

  implicit val bestellungMapping = new BaseEntitySQLSyntaxSupport[Bestellung] {
    override val tableName = "Bestellung"

    override lazy val columns = autoColumns[Bestellung]()

    def apply(rn: ResultName[Bestellung])(rs: WrappedResultSet): Bestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellung): Seq[Any] = parameters(Bestellung.unapply(entity).get)

    override def updateParameters(bestellung: Bestellung) = {
      super.updateParameters(bestellung) ++ Seq(
        column.sammelbestellungId -> parameter(bestellung.sammelbestellungId),
        column.preisTotal -> parameter(bestellung.preisTotal),
        column.steuerSatz -> parameter(bestellung.steuerSatz),
        column.steuer -> parameter(bestellung.steuer),
        column.totalSteuer -> parameter(bestellung.totalSteuer),
        column.adminProzente -> parameter(bestellung.adminProzente),
        column.adminProzenteAbzug -> parameter(bestellung.adminProzenteAbzug),
        column.totalNachAbzugAdminProzente -> parameter(bestellung.totalNachAbzugAdminProzente)
      )
    }
  }

  implicit val bestellpositionMapping = new BaseEntitySQLSyntaxSupport[Bestellposition] {
    override val tableName = "Bestellposition"

    override lazy val columns = autoColumns[Bestellposition]()

    def apply(rn: ResultName[Bestellposition])(rs: WrappedResultSet): Bestellposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellposition): Seq[Any] = parameters(Bestellposition.unapply(entity).get)

    override def updateParameters(bestellposition: Bestellposition) = {
      super.updateParameters(bestellposition) ++ Seq(
        column.produktId -> parameter(bestellposition.produktId),
        column.produktBeschrieb -> parameter(bestellposition.produktBeschrieb),
        column.preisEinheit -> parameter(bestellposition.preisEinheit),
        column.einheit -> parameter(bestellposition.einheit),
        column.menge -> parameter(bestellposition.menge),
        column.preis -> parameter(bestellposition.preis),
        column.anzahl -> parameter(bestellposition.anzahl)
      )
    }
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[Any] = parameters(Tour.unapply(entity).get)

    override def updateParameters(tour: Tour) = {
      super.updateParameters(tour) ++ Seq(
        column.name -> parameter(tour.name),
        column.beschreibung -> parameter(tour.beschreibung),
        column.anzahlAbonnenten -> parameter(tour.anzahlAbonnenten),
        column.anzahlAbonnentenAktiv -> parameter(tour.anzahlAbonnentenAktiv)
      )
    }
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[Any] = parameters(Depot.unapply(entity).get)

    override def updateParameters(depot: Depot) = {
      super.updateParameters(depot) ++ Seq(
        column.name -> parameter(depot.name),
        column.kurzzeichen -> parameter(depot.kurzzeichen),
        column.apName -> parameter(depot.apName),
        column.apVorname -> parameter(depot.apVorname),
        column.apTelefon -> parameter(depot.apTelefon),
        column.apEmail -> parameter(depot.apEmail),
        column.vName -> parameter(depot.vName),
        column.vVorname -> parameter(depot.vVorname),
        column.vTelefon -> parameter(depot.vTelefon),
        column.vEmail -> parameter(depot.vEmail),
        column.strasse -> parameter(depot.strasse),
        column.hausNummer -> parameter(depot.hausNummer),
        column.plz -> parameter(depot.plz),
        column.ort -> parameter(depot.ort),
        column.aktiv -> parameter(depot.aktiv),
        column.oeffnungszeiten -> parameter(depot.oeffnungszeiten),
        column.farbCode -> parameter(depot.farbCode),
        column.iban -> parameter(depot.iban),
        column.bank -> parameter(depot.bank),
        column.beschreibung -> parameter(depot.beschreibung),
        column.anzahlAbonnenten -> parameter(depot.anzahlAbonnenten),
        column.anzahlAbonnentenAktiv -> parameter(depot.anzahlAbonnentenAktiv),
        column.anzahlAbonnentenMax -> parameter(depot.anzahlAbonnentenMax)
      )
    }
  }

  implicit val vertriebMapping = new BaseEntitySQLSyntaxSupport[Vertrieb] {
    override val tableName = "Vertrieb"

    override lazy val columns = autoColumns[Vertrieb]()

    def apply(rn: ResultName[Vertrieb])(rs: WrappedResultSet): Vertrieb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Vertrieb): Seq[Any] = parameters(Vertrieb.unapply(entity).get)

    override def updateParameters(vertrieb: Vertrieb) = {
      super.updateParameters(vertrieb) ++ Seq(
        column.abotypId -> parameter(vertrieb.abotypId),
        column.liefertag -> parameter(vertrieb.liefertag),
        column.beschrieb -> parameter(vertrieb.beschrieb),
        column.anzahlAbos -> parameter(vertrieb.anzahlAbos),
        column.durchschnittspreis -> parameter(vertrieb.durchschnittspreis),
        column.anzahlLieferungen -> parameter(vertrieb.anzahlLieferungen),
        column.anzahlAbosAktiv -> parameter(vertrieb.anzahlAbosAktiv)
      )
    }
  }

  trait LieferungMapping[E <: Vertriebsart] extends BaseEntitySQLSyntaxSupport[E] {
    override def updateParameters(lieferung: E) = {
      super.updateParameters(lieferung) ++ Seq(
        column.vertriebId -> parameter(lieferung.vertriebId),
        column.anzahlAbos -> parameter(lieferung.anzahlAbos),
        column.anzahlAbosAktiv -> parameter(lieferung.anzahlAbosAktiv)
      )
    }
  }

  implicit val heimlieferungMapping = new LieferungMapping[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[Any] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Heimlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.tourId -> parameter(lieferung.tourId)
      )
    }
  }

  implicit val depotlieferungMapping = new LieferungMapping[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[Any] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Depotlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.depotId -> parameter(lieferung.depotId)
      )
    }
  }

  implicit val postlieferungMapping = new LieferungMapping[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[Any] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Postlieferung) = {
      super.updateParameters(lieferung)
    }
  }

  trait BaseAboMapping[A <: Abo] extends BaseEntitySQLSyntaxSupport[A] {
    override def updateParameters(abo: A) = {
      super.updateParameters(abo) ++ Seq(
        column.kundeId -> parameter(abo.kundeId),
        column.kunde -> parameter(abo.kunde),
        column.vertriebId -> parameter(abo.vertriebId),
        column.vertriebsartId -> parameter(abo.vertriebsartId),
        column.abotypId -> parameter(abo.abotypId),
        column.abotypName -> parameter(abo.abotypName),
        column.start -> parameter(abo.start),
        column.ende -> parameter(abo.ende),
        column.guthabenVertraglich -> parameter(abo.guthabenVertraglich),
        column.guthaben -> parameter(abo.guthaben),
        column.guthabenInRechnung -> parameter(abo.guthabenInRechnung),
        column.letzteLieferung -> parameter(abo.letzteLieferung),
        column.anzahlAbwesenheiten -> parameter(abo.anzahlAbwesenheiten),
        column.anzahlLieferungen -> parameter(abo.anzahlLieferungen),
        column.aktiv -> parameter(abo.aktiv)
      )
    }
  }

  implicit val depotlieferungAboMapping = new BaseAboMapping[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[Any] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(depotlieferungAbo: DepotlieferungAbo) = {
      super.updateParameters(depotlieferungAbo) ++ Seq(
        column.depotId -> parameter(depotlieferungAbo.depotId),
        column.depotName -> parameter(depotlieferungAbo.depotName)
      )
    }
  }

  implicit val heimlieferungAboMapping = new BaseAboMapping[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[Any] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(heimlieferungAbo: HeimlieferungAbo) = {
      super.updateParameters(heimlieferungAbo) ++ Seq(
        column.tourId -> parameter(heimlieferungAbo.tourId),
        column.tourName -> parameter(heimlieferungAbo.tourName),
        column.vertriebBeschrieb -> parameter(heimlieferungAbo.vertriebBeschrieb)
      )
    }
  }

  implicit val postlieferungAboMapping = new BaseAboMapping[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[Any] = parameters(PostlieferungAbo.unapply(entity).get)

    override def updateParameters(postlieferungAbo: PostlieferungAbo) = {
      super.updateParameters(postlieferungAbo)
    }
  }

  implicit val produktMapping = new BaseEntitySQLSyntaxSupport[Produkt] {
    override val tableName = "Produkt"

    override lazy val columns = autoColumns[Produkt]()

    def apply(rn: ResultName[Produkt])(rs: WrappedResultSet): Produkt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produkt): Seq[Any] = parameters(Produkt.unapply(entity).get)

    override def updateParameters(produkt: Produkt) = {
      super.updateParameters(produkt) ++ Seq(
        column.name -> parameter(produkt.name),
        column.verfuegbarVon -> parameter(produkt.verfuegbarVon),
        column.verfuegbarBis -> parameter(produkt.verfuegbarBis),
        column.kategorien -> parameter(produkt.kategorien),
        column.standardmenge -> parameter(produkt.standardmenge),
        column.einheit -> parameter(produkt.einheit),
        column.preis -> parameter(produkt.preis),
        column.produzenten -> parameter(produkt.produzenten)
      )
    }
  }

  implicit val produzentMapping = new BaseEntitySQLSyntaxSupport[Produzent] {
    override val tableName = "Produzent"

    override lazy val columns = autoColumns[Produzent]()

    def apply(rn: ResultName[Produzent])(rs: WrappedResultSet): Produzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produzent): Seq[Any] = parameters(Produzent.unapply(entity).get)

    override def updateParameters(produzent: Produzent) = {
      super.updateParameters(produzent) ++ Seq(
        column.name -> parameter(produzent.name),
        column.vorname -> parameter(produzent.vorname),
        column.kurzzeichen -> parameter(produzent.kurzzeichen),
        column.strasse -> parameter(produzent.strasse),
        column.hausNummer -> parameter(produzent.hausNummer),
        column.adressZusatz -> parameter(produzent.adressZusatz),
        column.plz -> parameter(produzent.plz),
        column.ort -> parameter(produzent.ort),
        column.bemerkungen -> parameter(produzent.bemerkungen),
        column.email -> parameter(produzent.email),
        column.telefonMobil -> parameter(produzent.telefonMobil),
        column.telefonFestnetz -> parameter(produzent.telefonFestnetz),
        column.iban -> parameter(produzent.iban),
        column.bank -> parameter(produzent.bank),
        column.mwst -> parameter(produzent.mwst),
        column.mwstSatz -> parameter(produzent.mwstSatz),
        column.mwstNr -> parameter(produzent.mwstNr),
        column.aktiv -> parameter(produzent.aktiv)
      )
    }
  }

  implicit val produktekategorieMapping = new BaseEntitySQLSyntaxSupport[Produktekategorie] {
    override val tableName = "Produktekategorie"

    override lazy val columns = autoColumns[Produktekategorie]()

    def apply(rn: ResultName[Produktekategorie])(rs: WrappedResultSet): Produktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produktekategorie): Seq[Any] = parameters(Produktekategorie.unapply(entity).get)

    override def updateParameters(produktekategorie: Produktekategorie) = {
      super.updateParameters(produktekategorie) ++ Seq(
        column.beschreibung -> parameter(produktekategorie.beschreibung)
      )
    }
  }

  implicit val projektMapping = new BaseEntitySQLSyntaxSupport[Projekt] {
    override val tableName = "Projekt"

    override lazy val columns = autoColumns[Projekt]()

    def apply(rn: ResultName[Projekt])(rs: WrappedResultSet): Projekt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Projekt): Seq[Any] = parameters(Projekt.unapply(entity).get)

    override def updateParameters(projekt: Projekt) = {
      super.updateParameters(projekt) ++ Seq(
        column.bezeichnung -> parameter(projekt.bezeichnung),
        column.strasse -> parameter(projekt.strasse),
        column.hausNummer -> parameter(projekt.hausNummer),
        column.adressZusatz -> parameter(projekt.adressZusatz),
        column.plz -> parameter(projekt.plz),
        column.ort -> parameter(projekt.ort),
        column.preiseSichtbar -> parameter(projekt.preiseSichtbar),
        column.preiseEditierbar -> parameter(projekt.preiseEditierbar),
        column.emailErforderlich -> parameter(projekt.emailErforderlich),
        column.waehrung -> parameter(projekt.waehrung),
        column.geschaeftsjahrMonat -> parameter(projekt.geschaeftsjahrMonat),
        column.geschaeftsjahrTag -> parameter(projekt.geschaeftsjahrTag),
        column.twoFactorAuthentication -> parameter(projekt.twoFactorAuthentication),
        column.sprache -> parameter(projekt.sprache)
      )
    }
  }

  implicit val produktProduzentMapping = new BaseEntitySQLSyntaxSupport[ProduktProduzent] {
    override val tableName = "ProduktProduzent"

    override lazy val columns = autoColumns[ProduktProduzent]()

    def apply(rn: ResultName[ProduktProduzent])(rs: WrappedResultSet): ProduktProduzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduzent): Seq[Any] = parameters(ProduktProduzent.unapply(entity).get)

    override def updateParameters(projekt: ProduktProduzent) = {
      super.updateParameters(projekt) ++ Seq(
        column.produktId -> parameter(projekt.produktId),
        column.produzentId -> parameter(projekt.produzentId)
      )
    }
  }

  implicit val produktProduktekategorieMapping = new BaseEntitySQLSyntaxSupport[ProduktProduktekategorie] {
    override val tableName = "ProduktProduktekategorie"

    override lazy val columns = autoColumns[ProduktProduktekategorie]()

    def apply(rn: ResultName[ProduktProduktekategorie])(rs: WrappedResultSet): ProduktProduktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduktekategorie): Seq[Any] = parameters(ProduktProduktekategorie.unapply(entity).get)

    override def updateParameters(produktkat: ProduktProduktekategorie) = {
      super.updateParameters(produktkat) ++ Seq(
        column.produktId -> parameter(produktkat.produktId),
        column.produktekategorieId -> parameter(produktkat.produktekategorieId)
      )
    }
  }

  implicit val abwesenheitMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[Any] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit) = {
      super.updateParameters(entity) ++ Seq(
        column.aboId -> parameter(entity.aboId),
        column.lieferungId -> parameter(entity.lieferungId),
        column.datum -> parameter(entity.datum),
        column.bemerkung -> parameter(entity.bemerkung)
      )
    }
  }

  implicit val tourlieferungMapping = new BaseEntitySQLSyntaxSupport[Tourlieferung] {
    override val tableName = "Tourlieferung"

    override lazy val columns = autoColumns[Tourlieferung]()

    def apply(rn: ResultName[Tourlieferung])(rs: WrappedResultSet): Tourlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tourlieferung): Seq[Any] = parameters(Tourlieferung.unapply(entity).get)

    override def updateParameters(entity: Tourlieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.sort -> parameter(entity.sort)
      )
    }
  }

  implicit val korbMapping = new BaseEntitySQLSyntaxSupport[Korb] {
    override val tableName = "Korb"

    override lazy val columns = autoColumns[Korb]()

    def apply(rn: ResultName[Korb])(rs: WrappedResultSet): Korb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Korb): Seq[Any] = parameters(Korb.unapply(entity).get)

    override def updateParameters(entity: Korb) = {
      super.updateParameters(entity) ++ Seq(
        column.lieferungId -> parameter(entity.lieferungId),
        column.aboId -> parameter(entity.aboId),
        column.status -> parameter(entity.status),
        column.auslieferungId -> parameter(entity.auslieferungId),
        column.guthabenVorLieferung -> parameter(entity.guthabenVorLieferung),
        column.sort -> parameter(entity.sort)
      )
    }
  }

  trait AuslieferungMapping[E <: Auslieferung] extends BaseEntitySQLSyntaxSupport[E] {
    override def updateParameters(auslieferung: E) = {
      super.updateParameters(auslieferung) ++ Seq(
        column.status -> parameter(auslieferung.status),
        column.datum -> parameter(auslieferung.datum),
        column.anzahlKoerbe -> parameter(auslieferung.anzahlKoerbe)
      )
    }
  }

  implicit val depotAuslieferungMapping = new AuslieferungMapping[DepotAuslieferung] {
    override val tableName = "DepotAuslieferung"

    override lazy val columns = autoColumns[DepotAuslieferung]()

    def apply(rn: ResultName[DepotAuslieferung])(rs: WrappedResultSet): DepotAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DepotAuslieferung): Seq[Any] =
      parameters(DepotAuslieferung.unapply(entity).get)

    override def updateParameters(entity: DepotAuslieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.depotId -> parameter(entity.depotId),
        column.depotName -> parameter(entity.depotName)
      )
    }
  }

  implicit val tourAuslieferungMapping = new AuslieferungMapping[TourAuslieferung] {
    override val tableName = "TourAuslieferung"

    override lazy val columns = autoColumns[TourAuslieferung]()

    def apply(rn: ResultName[TourAuslieferung])(rs: WrappedResultSet): TourAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: TourAuslieferung): Seq[Any] =
      parameters(TourAuslieferung.unapply(entity).get)

    override def updateParameters(entity: TourAuslieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.tourId -> parameter(entity.tourId),
        column.tourName -> parameter(entity.tourName)
      )
    }
  }

  implicit val postAuslieferungMapping = new AuslieferungMapping[PostAuslieferung] {
    override val tableName = "PostAuslieferung"

    override lazy val columns = autoColumns[PostAuslieferung]()

    def apply(rn: ResultName[PostAuslieferung])(rs: WrappedResultSet): PostAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostAuslieferung): Seq[Any] =
      parameters(PostAuslieferung.unapply(entity).get)
  }

  implicit val projektVorlageMapping = new BaseEntitySQLSyntaxSupport[ProjektVorlage] {
    override val tableName = "ProjektVorlage"

    override lazy val columns = autoColumns[ProjektVorlage]()

    def apply(rn: ResultName[ProjektVorlage])(rs: WrappedResultSet): ProjektVorlage =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProjektVorlage): Seq[Any] =
      parameters(ProjektVorlage.unapply(entity).get)

    override def updateParameters(entity: ProjektVorlage) = {
      super.updateParameters(entity) ++ Seq(
        column.name -> parameter(entity.name),
        column.beschreibung -> parameter(entity.beschreibung),
        column.fileStoreId -> parameter(entity.fileStoreId)
      )
    }
  }

  implicit val einladungMapping = new BaseEntitySQLSyntaxSupport[Einladung] {
    override val tableName = "Einladung"

    override lazy val columns = autoColumns[Einladung]()

    def apply(rn: ResultName[Einladung])(rs: WrappedResultSet): Einladung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Einladung): Seq[Any] =
      parameters(Einladung.unapply(entity).get)
  }
}
