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
import ch.openolitor.stammdaten.models._
import scalikejdbc._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.models.PendenzStatus
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import scala.collection.immutable.TreeMap
import ch.openolitor.core.filestore.VorlageRechnung
import scala.math.Ordering.StringOrdering
import ch.openolitor.core.repositories.BaseParameter

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging with BaseParameter {

  val fristeinheitPattern = """(\d+)(M|W)""".r

  // DB type binders for read operations
  implicit val tourIdBinder: Binders[TourId] = baseIdBinders(TourId.apply _)
  implicit val depotIdBinder: Binders[DepotId] = baseIdBinders(DepotId.apply _)
  implicit val aboTypIdBinder: Binders[AbotypId] = baseIdBinders(AbotypId.apply _)
  implicit val vertriebIdBinder: Binders[VertriebId] = baseIdBinders(VertriebId.apply _)
  implicit val vertriebsartIdBinder: Binders[VertriebsartId] = baseIdBinders(VertriebsartId.apply _)
  implicit val vertriebsartIdSetBinder: Binders[Set[VertriebsartId]] = setBaseIdBinders(VertriebsartId.apply _)
  implicit val vertriebsartIdSeqBinder: Binders[Seq[VertriebsartId]] = seqBaseIdBinders(VertriebsartId.apply _)
  implicit val kundeIdBinder: Binders[KundeId] = baseIdBinders(KundeId.apply _)
  implicit val pendenzIdBinder: Binders[PendenzId] = baseIdBinders(PendenzId.apply _)
  implicit val aboIdBinder: Binders[AboId] = baseIdBinders(AboId.apply _)
  implicit val lierferungIdBinder: Binders[LieferungId] = baseIdBinders(LieferungId.apply _)
  implicit val lieferplanungIdBinder: Binders[LieferplanungId] = baseIdBinders(LieferplanungId.apply _)
  implicit val optionLieferplanungIdBinder: Binders[Option[LieferplanungId]] = optionBaseIdBinders(LieferplanungId.apply _)
  implicit val lieferpositionIdBinder: Binders[LieferpositionId] = baseIdBinders(LieferpositionId.apply _)
  implicit val bestellungIdBinder: Binders[BestellungId] = baseIdBinders(BestellungId.apply _)
  implicit val sammelbestellungIdBinder: Binders[SammelbestellungId] = baseIdBinders(SammelbestellungId.apply _)
  implicit val bestellpositionIdBinder: Binders[BestellpositionId] = baseIdBinders(BestellpositionId.apply _)
  implicit val customKundentypIdBinder: Binders[CustomKundentypId] = baseIdBinders(CustomKundentypId.apply _)
  implicit val kundentypIdBinder: Binders[KundentypId] = Binders.string.xmap(KundentypId.apply _, _.id)
  implicit val produktekategorieIdBinder: Binders[ProduktekategorieId] = baseIdBinders(ProduktekategorieId.apply _)
  implicit val baseProduktekategorieIdBinder: Binders[BaseProduktekategorieId] = Binders.string.xmap(BaseProduktekategorieId.apply _, _.id)
  implicit val produktIdBinder: Binders[ProduktId] = baseIdBinders(ProduktId.apply _)
  implicit val optionProduktIdBinder: Binders[Option[ProduktId]] = optionBaseIdBinders(ProduktId.apply _)
  implicit val produzentIdBinder: Binders[ProduzentId] = baseIdBinders(ProduzentId.apply _)
  implicit val baseProduzentIdBinder: Binders[BaseProduzentId] = Binders.string.xmap(BaseProduzentId.apply _, _.id)
  implicit val projektIdBinder: Binders[ProjektId] = baseIdBinders(ProjektId.apply _)
  implicit val produktProduzentIdBinder: Binders[ProduktProduzentId] = baseIdBinders(ProduktProduzentId.apply _)
  implicit val produktProduktekategorieIdBinder: Binders[ProduktProduktekategorieId] = baseIdBinders(ProduktProduktekategorieId.apply _)
  implicit val abwesenheitIdBinder: Binders[AbwesenheitId] = baseIdBinders(AbwesenheitId.apply _)
  implicit val korbIdBinder: Binders[KorbId] = baseIdBinders(KorbId.apply _)
  implicit val auslieferungIdBinder: Binders[AuslieferungId] = baseIdBinders(AuslieferungId.apply _)
  implicit val projektVorlageIdBinder: Binders[ProjektVorlageId] = baseIdBinders(ProjektVorlageId.apply _)
  implicit val optionAuslieferungIdBinder: Binders[Option[AuslieferungId]] = optionBaseIdBinders(AuslieferungId.apply _)
  implicit val einladungIdBinder: Binders[EinladungId] = baseIdBinders(EinladungId.apply _)
  implicit val kontoDatenIdBinder: Binders[KontoDatenId] = baseIdBinders(KontoDatenId.apply _)

  implicit val pendenzStatusBinders: Binders[PendenzStatus] = toStringBinder(PendenzStatus.apply)
  implicit val rhythmusBinders: Binders[Rhythmus] = toStringBinder(Rhythmus.apply)
  implicit val waehrungBinders: Binders[Waehrung] = toStringBinder(Waehrung.apply)
  implicit val lieferungStatusBinders: Binders[LieferungStatus] = toStringBinder(LieferungStatus.apply)
  implicit val korbStatusBinders: Binders[KorbStatus] = toStringBinder(KorbStatus.apply)
  implicit val auslieferungStatusBinders: Binders[AuslieferungStatus] = toStringBinder(AuslieferungStatus.apply)
  implicit val preiseinheitBinders: Binders[Preiseinheit] = toStringBinder(Preiseinheit.apply)
  implicit val lieferzeitpunktBinders: Binders[Lieferzeitpunkt] = toStringBinder(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetBinders: Binders[Set[Lieferzeitpunkt]] = setSqlBinder(Lieferzeitpunkt.apply, _.toString)
  implicit val kundenTypIdSetBinder: Binders[Set[KundentypId]] = setSqlBinder(KundentypId.apply, _.toString)
  implicit val laufzeiteinheitBinders: Binders[Laufzeiteinheit] = toStringBinder(Laufzeiteinheit.apply)
  implicit val liefereinheiBinders: Binders[Liefereinheit] = toStringBinder(Liefereinheit.apply)
  implicit val liefersaisonBinders: Binders[Liefersaison] = toStringBinder(Liefersaison.apply)
  implicit val vorlageTypeBinders: Binders[VorlageTyp] = toStringBinder(VorlageTyp.apply)
  implicit val anredeBinders: Binders[Option[Anrede]] = toStringBinder(Anrede.apply)
  implicit val fristBinders: Binders[Option[Frist]] = Binders.string.xmap(_ match {
    case fristeinheitPattern(wert, "W") => Some(Frist(wert.toInt, Wochenfrist))
    case fristeinheitPattern(wert, "M") => Some(Frist(wert.toInt, Monatsfrist))
    case _ => None
  }, {
    _ match {
      case None => ""
      case Some(frist) =>
        val einheit = frist.einheit match {
          case Wochenfrist => "W"
          case Monatsfrist => "M"
        }
        s"${frist.wert}$einheit"
    }
  })

  implicit val baseProduktekategorieIdSetBinders: Binders[Set[BaseProduktekategorieId]] = setBaseStringIdBinders(BaseProduktekategorieId.apply _)
  implicit val baseProduzentIdSetBinders: Binders[Set[BaseProduzentId]] = setBaseStringIdBinders(BaseProduzentId.apply _)
  implicit val stringIntTreeMapBinders: Binders[TreeMap[String, Int]] = treeMapBinders[String, Int](identity, _.toInt, identity, _.toString)
  implicit val stringBigDecimalTreeMapBinders: Binders[TreeMap[String, BigDecimal]] = treeMapBinders(identity, BigDecimal(_), identity, _.toString)
  implicit val rolleMapBinders: Binders[Map[Rolle, Boolean]] = mapBinders(r => Rolle(r).getOrElse(KundenZugang), _.toBoolean, _.toString, _.toString)
  implicit val rolleBinders: Binders[Option[Rolle]] = toStringBinder(Rolle.apply)

  // declare parameterbinderfactories for enum type to allow dynamic type convertion of enum subtypes
  implicit def pendenzStatusParameterBinderFactory[A <: PendenzStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def rhythmusParameterBinderFactory[A <: Rhythmus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def waehrungParameterBinderFactory[A <: Waehrung]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def lieferungStatusParameterBinderFactory[A <: LieferungStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def korbStatusParameterBinderFactory[A <: KorbStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def auslieferungStatusParameterBinderFactory[A <: AuslieferungStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def preiseinheitParameterBinderFactory[A <: Preiseinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def lieferzeitpunktParameterBinderFactory[A <: Lieferzeitpunkt]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def laufzeiteinheitParameterBinderFactory[A <: Laufzeiteinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def liefereinheitParameterBinderFactory[A <: Liefereinheit]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def liefersaisonParameterBinderFactory[A <: Liefersaison]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def vorlageParameterBinderFactory[A <: VorlageTyp]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[ParameterBinder] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(abotyp: Abotyp) = {
      super.updateParameters(abotyp) ++ Seq(
        column.name -> abotyp.name,
        column.beschreibung -> abotyp.beschreibung,
        column.lieferrhythmus -> abotyp.lieferrhythmus,
        column.aktivVon -> abotyp.aktivVon,
        column.aktivBis -> abotyp.aktivBis,
        column.preis -> abotyp.preis,
        column.preiseinheit -> abotyp.preiseinheit,
        column.laufzeit -> abotyp.laufzeit,
        column.laufzeiteinheit -> abotyp.laufzeiteinheit,
        column.vertragslaufzeit -> abotyp.vertragslaufzeit,
        column.kuendigungsfrist -> abotyp.kuendigungsfrist,
        column.anzahlAbwesenheiten -> abotyp.anzahlAbwesenheiten,
        column.farbCode -> abotyp.farbCode,
        column.zielpreis -> abotyp.zielpreis,
        column.guthabenMindestbestand -> abotyp.guthabenMindestbestand,
        column.adminProzente -> abotyp.adminProzente,
        column.wirdGeplant -> abotyp.wirdGeplant,
        column.anzahlAbonnenten -> abotyp.anzahlAbonnenten,
        column.anzahlAbonnentenAktiv -> abotyp.anzahlAbonnentenAktiv,
        column.letzteLieferung -> abotyp.letzteLieferung,
        column.waehrung -> abotyp.waehrung
      )
    }
  }

  implicit val zusatzAbotypMapping = new BaseEntitySQLSyntaxSupport[ZusatzAbotyp] {
    override val tableName = "ZusatzAbotyp"

    override lazy val columns = autoColumns[ZusatzAbotyp]()

    def apply(rn: ResultName[ZusatzAbotyp])(rs: WrappedResultSet): ZusatzAbotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: ZusatzAbotyp): Seq[ParameterBinder] =
      parameters(ZusatzAbotyp.unapply(entity).get)

    override def updateParameters(zusatzabotyp: ZusatzAbotyp) = {
      super.updateParameters(zusatzabotyp) ++ Seq(
        column.name -> zusatzabotyp.name,
        column.beschreibung -> zusatzabotyp.beschreibung,
        column.aktivVon -> zusatzabotyp.aktivVon,
        column.aktivBis -> zusatzabotyp.aktivBis,
        column.preis -> zusatzabotyp.preis,
        column.preiseinheit -> zusatzabotyp.preiseinheit,
        column.laufzeit -> zusatzabotyp.laufzeit,
        column.laufzeiteinheit -> zusatzabotyp.laufzeiteinheit,
        column.vertragslaufzeit -> zusatzabotyp.vertragslaufzeit,
        column.kuendigungsfrist -> zusatzabotyp.kuendigungsfrist,
        column.anzahlAbwesenheiten -> zusatzabotyp.anzahlAbwesenheiten,
        column.farbCode -> zusatzabotyp.farbCode,
        column.zielpreis -> zusatzabotyp.zielpreis,
        column.guthabenMindestbestand -> zusatzabotyp.guthabenMindestbestand,
        column.adminProzente -> zusatzabotyp.adminProzente,
        column.wirdGeplant -> zusatzabotyp.wirdGeplant,
        column.anzahlAbonnenten -> zusatzabotyp.anzahlAbonnenten,
        column.anzahlAbonnentenAktiv -> zusatzabotyp.anzahlAbonnentenAktiv,
        column.letzteLieferung -> zusatzabotyp.letzteLieferung,
        column.waehrung -> zusatzabotyp.waehrung
      )
    }
  }

  implicit val customKundentypMapping = new BaseEntitySQLSyntaxSupport[CustomKundentyp] {
    override val tableName = "Kundentyp"

    override lazy val columns = autoColumns[CustomKundentyp]()

    def apply(rn: ResultName[CustomKundentyp])(rs: WrappedResultSet): CustomKundentyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: CustomKundentyp): Seq[ParameterBinder] =
      parameters(CustomKundentyp.unapply(entity).get)

    override def updateParameters(typ: CustomKundentyp) = {
      super.updateParameters(typ) ++ Seq(
        column.kundentyp -> typ.kundentyp,
        column.beschreibung -> typ.beschreibung,
        column.anzahlVerknuepfungen -> typ.anzahlVerknuepfungen
      )
    }
  }

  implicit val kundeMapping = new BaseEntitySQLSyntaxSupport[Kunde] {
    override val tableName = "Kunde"

    override lazy val columns = autoColumns[Kunde]()

    def apply(rn: ResultName[Kunde])(rs: WrappedResultSet): Kunde =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Kunde): Seq[ParameterBinder] =
      parameters(Kunde.unapply(entity).get)

    override def updateParameters(kunde: Kunde) = {
      super.updateParameters(kunde) ++ Seq(
        column.bezeichnung -> kunde.bezeichnung,
        column.strasse -> kunde.strasse,
        column.hausNummer -> kunde.hausNummer,
        column.adressZusatz -> kunde.adressZusatz,
        column.plz -> kunde.plz,
        column.ort -> kunde.ort,
        column.abweichendeLieferadresse -> kunde.abweichendeLieferadresse,
        column.bezeichnungLieferung -> kunde.bezeichnungLieferung,
        column.strasseLieferung -> kunde.strasseLieferung,
        column.hausNummerLieferung -> kunde.hausNummerLieferung,
        column.plzLieferung -> kunde.plzLieferung,
        column.ortLieferung -> kunde.ortLieferung,
        column.adressZusatzLieferung -> kunde.adressZusatzLieferung,
        column.zusatzinfoLieferung -> kunde.zusatzinfoLieferung,
        column.typen -> kunde.typen,
        column.bemerkungen -> kunde.bemerkungen,
        column.anzahlAbos -> kunde.anzahlAbos,
        column.anzahlAbosAktiv -> kunde.anzahlAbosAktiv,
        column.anzahlPendenzen -> kunde.anzahlPendenzen,
        column.anzahlPersonen -> kunde.anzahlPersonen
      )
    }
  }

  implicit val personMapping = new BaseEntitySQLSyntaxSupport[Person] {
    override val tableName = "Person"

    override lazy val columns = autoColumns[Person]()

    def apply(rn: ResultName[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Person): Seq[ParameterBinder] =
      parameters(Person.unapply(entity).get)

    override def updateParameters(person: Person) = {
      super.updateParameters(person) ++ Seq(
        column.kundeId -> person.kundeId,
        column.anrede -> person.anrede,
        column.name -> person.name,
        column.vorname -> person.vorname,
        column.email -> person.email,
        column.emailAlternative -> person.emailAlternative,
        column.telefonMobil -> person.telefonMobil,
        column.telefonFestnetz -> person.telefonFestnetz,
        column.bemerkungen -> person.bemerkungen,
        column.sort -> person.sort,
        column.loginAktiv -> person.loginAktiv,
        column.passwort -> person.passwort,
        column.passwortWechselErforderlich -> person.passwortWechselErforderlich,
        column.rolle -> person.rolle,
        column.letzteAnmeldung -> person.letzteAnmeldung
      )
    }
  }

  implicit val pendenzMapping = new BaseEntitySQLSyntaxSupport[Pendenz] {
    override val tableName = "Pendenz"

    override lazy val columns = autoColumns[Pendenz]()

    def apply(rn: ResultName[Pendenz])(rs: WrappedResultSet): Pendenz =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Pendenz): Seq[ParameterBinder] =
      parameters(Pendenz.unapply(entity).get)

    override def updateParameters(pendenz: Pendenz) = {
      super.updateParameters(pendenz) ++ Seq(
        column.kundeId -> pendenz.kundeId,
        column.kundeBezeichnung -> pendenz.kundeBezeichnung,
        column.datum -> pendenz.datum,
        column.bemerkung -> pendenz.bemerkung,
        column.status -> pendenz.status,
        column.generiert -> pendenz.generiert
      )
    }
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[ParameterBinder] =
      parameters(Lieferung.unapply(entity).get)

    override def updateParameters(lieferung: Lieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.abotypId -> lieferung.abotypId,
        column.abotypBeschrieb -> lieferung.abotypBeschrieb,
        column.vertriebId -> lieferung.vertriebId,
        column.vertriebBeschrieb -> lieferung.vertriebBeschrieb,
        column.status -> lieferung.status,
        column.datum -> lieferung.datum,
        column.durchschnittspreis -> lieferung.durchschnittspreis,
        column.anzahlLieferungen -> lieferung.anzahlLieferungen,
        column.anzahlKoerbeZuLiefern -> lieferung.anzahlKoerbeZuLiefern,
        column.anzahlAbwesenheiten -> lieferung.anzahlAbwesenheiten,
        column.anzahlSaldoZuTief -> lieferung.anzahlSaldoZuTief,
        column.zielpreis -> lieferung.zielpreis,
        column.preisTotal -> lieferung.preisTotal,
        column.lieferplanungId -> lieferung.lieferplanungId
      )
    }
  }

  implicit val lieferplanungMapping = new BaseEntitySQLSyntaxSupport[Lieferplanung] {
    override val tableName = "Lieferplanung"

    override lazy val columns = autoColumns[Lieferplanung]()

    def apply(rn: ResultName[Lieferplanung])(rs: WrappedResultSet): Lieferplanung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferplanung): Seq[ParameterBinder] = parameters(Lieferplanung.unapply(entity).get)

    override def updateParameters(lieferplanung: Lieferplanung) = {
      super.updateParameters(lieferplanung) ++ Seq(
        column.bemerkungen -> lieferplanung.bemerkungen,
        column.abotypDepotTour -> lieferplanung.abotypDepotTour,
        column.status -> lieferplanung.status
      )
    }
  }

  implicit val lieferpositionMapping = new BaseEntitySQLSyntaxSupport[Lieferposition] {
    override val tableName = "Lieferposition"

    override lazy val columns = autoColumns[Lieferposition]()

    def apply(rn: ResultName[Lieferposition])(rs: WrappedResultSet): Lieferposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferposition): Seq[ParameterBinder] = parameters(Lieferposition.unapply(entity).get)

    override def updateParameters(lieferposition: Lieferposition) = {
      super.updateParameters(lieferposition) ++ Seq(
        column.produktId -> lieferposition.produktId,
        column.produktBeschrieb -> lieferposition.produktBeschrieb,
        column.produzentId -> lieferposition.produzentId,
        column.produzentKurzzeichen -> lieferposition.produzentKurzzeichen,
        column.preisEinheit -> lieferposition.preisEinheit,
        column.einheit -> lieferposition.einheit,
        column.menge -> lieferposition.menge,
        column.preis -> lieferposition.preis,
        column.anzahl -> lieferposition.anzahl
      )
    }
  }

  implicit val sammelbestellungMapping = new BaseEntitySQLSyntaxSupport[Sammelbestellung] {
    override val tableName = "Sammelbestellung"

    override lazy val columns = autoColumns[Sammelbestellung]()

    def apply(rn: ResultName[Sammelbestellung])(rs: WrappedResultSet): Sammelbestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Sammelbestellung): Seq[ParameterBinder] = parameters(Sammelbestellung.unapply(entity).get)

    override def updateParameters(sammelbestellung: Sammelbestellung) = {
      super.updateParameters(sammelbestellung) ++ Seq(
        column.produzentId -> sammelbestellung.produzentId,
        column.produzentKurzzeichen -> sammelbestellung.produzentKurzzeichen,
        column.lieferplanungId -> sammelbestellung.lieferplanungId,
        column.status -> sammelbestellung.status,
        column.datum -> sammelbestellung.datum,
        column.datumAbrechnung -> sammelbestellung.datumAbrechnung,
        column.preisTotal -> sammelbestellung.preisTotal,
        column.steuerSatz -> sammelbestellung.steuerSatz,
        column.steuer -> sammelbestellung.steuer,
        column.totalSteuer -> sammelbestellung.totalSteuer,
        column.datumVersendet -> sammelbestellung.datumVersendet
      )
    }
  }

  implicit val bestellungMapping = new BaseEntitySQLSyntaxSupport[Bestellung] {
    override val tableName = "Bestellung"

    override lazy val columns = autoColumns[Bestellung]()

    def apply(rn: ResultName[Bestellung])(rs: WrappedResultSet): Bestellung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellung): Seq[ParameterBinder] = parameters(Bestellung.unapply(entity).get)

    override def updateParameters(bestellung: Bestellung) = {
      super.updateParameters(bestellung) ++ Seq(
        column.sammelbestellungId -> bestellung.sammelbestellungId,
        column.preisTotal -> bestellung.preisTotal,
        column.steuerSatz -> bestellung.steuerSatz,
        column.steuer -> bestellung.steuer,
        column.totalSteuer -> bestellung.totalSteuer,
        column.adminProzente -> bestellung.adminProzente,
        column.adminProzenteAbzug -> bestellung.adminProzenteAbzug,
        column.totalNachAbzugAdminProzente -> bestellung.totalNachAbzugAdminProzente
      )
    }
  }

  implicit val bestellpositionMapping = new BaseEntitySQLSyntaxSupport[Bestellposition] {
    override val tableName = "Bestellposition"

    override lazy val columns = autoColumns[Bestellposition]()

    def apply(rn: ResultName[Bestellposition])(rs: WrappedResultSet): Bestellposition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Bestellposition): Seq[ParameterBinder] = parameters(Bestellposition.unapply(entity).get)

    override def updateParameters(bestellposition: Bestellposition) = {
      super.updateParameters(bestellposition) ++ Seq(
        column.produktId -> bestellposition.produktId,
        column.produktBeschrieb -> bestellposition.produktBeschrieb,
        column.preisEinheit -> bestellposition.preisEinheit,
        column.einheit -> bestellposition.einheit,
        column.menge -> bestellposition.menge,
        column.preis -> bestellposition.preis,
        column.anzahl -> bestellposition.anzahl
      )
    }
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[ParameterBinder] = parameters(Tour.unapply(entity).get)

    override def updateParameters(tour: Tour) = {
      super.updateParameters(tour) ++ Seq(
        column.name -> tour.name,
        column.beschreibung -> tour.beschreibung,
        column.anzahlAbonnenten -> tour.anzahlAbonnenten,
        column.anzahlAbonnentenAktiv -> tour.anzahlAbonnentenAktiv
      )
    }
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[ParameterBinder] = parameters(Depot.unapply(entity).get)

    override def updateParameters(depot: Depot) = {
      super.updateParameters(depot) ++ Seq(
        column.name -> depot.name,
        column.kurzzeichen -> depot.kurzzeichen,
        column.apName -> depot.apName,
        column.apVorname -> depot.apVorname,
        column.apTelefon -> depot.apTelefon,
        column.apEmail -> depot.apEmail,
        column.vName -> depot.vName,
        column.vVorname -> depot.vVorname,
        column.vTelefon -> depot.vTelefon,
        column.vEmail -> depot.vEmail,
        column.strasse -> depot.strasse,
        column.hausNummer -> depot.hausNummer,
        column.plz -> depot.plz,
        column.ort -> depot.ort,
        column.aktiv -> depot.aktiv,
        column.oeffnungszeiten -> depot.oeffnungszeiten,
        column.farbCode -> depot.farbCode,
        column.iban -> depot.iban,
        column.bank -> depot.bank,
        column.beschreibung -> depot.beschreibung,
        column.anzahlAbonnenten -> depot.anzahlAbonnenten,
        column.anzahlAbonnentenAktiv -> depot.anzahlAbonnentenAktiv,
        column.anzahlAbonnentenMax -> depot.anzahlAbonnentenMax
      )
    }
  }

  implicit val vertriebMapping = new BaseEntitySQLSyntaxSupport[Vertrieb] {
    override val tableName = "Vertrieb"

    override lazy val columns = autoColumns[Vertrieb]()

    def apply(rn: ResultName[Vertrieb])(rs: WrappedResultSet): Vertrieb = autoConstruct(rs, rn)

    def parameterMappings(entity: Vertrieb): Seq[ParameterBinder] = parameters(Vertrieb.unapply(entity).get)

    override def updateParameters(vertrieb: Vertrieb) = {
      super.updateParameters(vertrieb) ++ Seq(
        column.abotypId -> vertrieb.abotypId,
        column.liefertag -> vertrieb.liefertag,
        column.beschrieb -> vertrieb.beschrieb,
        column.anzahlAbos -> vertrieb.anzahlAbos,
        column.durchschnittspreis -> vertrieb.durchschnittspreis,
        column.anzahlLieferungen -> vertrieb.anzahlLieferungen,
        column.anzahlAbosAktiv -> vertrieb.anzahlAbosAktiv
      )
    }
  }

  trait LieferungMapping[E <: Vertriebsart] extends BaseEntitySQLSyntaxSupport[E] {
    override def updateParameters(lieferung: E) = {
      super.updateParameters(lieferung) ++ Seq(
        column.vertriebId -> lieferung.vertriebId,
        column.anzahlAbos -> lieferung.anzahlAbos,
        column.anzahlAbosAktiv -> lieferung.anzahlAbosAktiv
      )
    }
  }

  implicit val heimlieferungMapping = new LieferungMapping[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[ParameterBinder] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Heimlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.tourId -> lieferung.tourId
      )
    }
  }

  implicit val depotlieferungMapping = new LieferungMapping[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[ParameterBinder] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Depotlieferung) = {
      super.updateParameters(lieferung) ++ Seq(
        column.depotId -> lieferung.depotId
      )
    }
  }

  implicit val postlieferungMapping = new LieferungMapping[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[ParameterBinder] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Postlieferung) = {
      super.updateParameters(lieferung)
    }
  }

  trait BaseAboMapping[A <: Abo] extends BaseEntitySQLSyntaxSupport[A] {
    override def updateParameters(abo: A) = {
      super.updateParameters(abo) ++ Seq(
        column.kundeId -> abo.kundeId,
        column.kunde -> abo.kunde,
        column.vertriebId -> abo.vertriebId,
        column.vertriebsartId -> abo.vertriebsartId,
        column.abotypId -> abo.abotypId,
        column.abotypName -> abo.abotypName,
        column.start -> abo.start,
        column.ende -> abo.ende,
        column.guthabenVertraglich -> abo.guthabenVertraglich,
        column.guthaben -> abo.guthaben,
        column.guthabenInRechnung -> abo.guthabenInRechnung,
        column.letzteLieferung -> abo.letzteLieferung,
        column.anzahlAbwesenheiten -> abo.anzahlAbwesenheiten,
        column.anzahlLieferungen -> abo.anzahlLieferungen,
        column.aktiv -> abo.aktiv
      )
    }
  }

  implicit val depotlieferungAboMapping = new BaseAboMapping[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[ParameterBinder] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(depotlieferungAbo: DepotlieferungAbo) = {
      super.updateParameters(depotlieferungAbo) ++ Seq(
        column.depotId -> depotlieferungAbo.depotId,
        column.depotName -> depotlieferungAbo.depotName
      )
    }
  }

  implicit val heimlieferungAboMapping = new BaseAboMapping[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[ParameterBinder] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(heimlieferungAbo: HeimlieferungAbo) = {
      super.updateParameters(heimlieferungAbo) ++ Seq(
        column.tourId -> heimlieferungAbo.tourId,
        column.tourName -> heimlieferungAbo.tourName,
        column.vertriebBeschrieb -> heimlieferungAbo.vertriebBeschrieb
      )
    }
  }

  implicit val postlieferungAboMapping = new BaseAboMapping[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[ParameterBinder] = parameters(PostlieferungAbo.unapply(entity).get)
  }

  implicit val zusatzAboMapping = new BaseEntitySQLSyntaxSupport[ZusatzAbo] {
    override val tableName = "ZusatzAbo"

    override lazy val columns = autoColumns[ZusatzAbo]()

    def apply(rn: ResultName[ZusatzAbo])(rs: WrappedResultSet): ZusatzAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: ZusatzAbo): Seq[ParameterBinder] = {
      parameters(ZusatzAbo.unapply(entity).get)
    }

    override def updateParameters(zusatzAbo: ZusatzAbo) = {
      super.updateParameters(zusatzAbo) ++ Seq(
        column.hauptAboId -> zusatzAbo.hauptAboId,
        column.hauptAbotypId -> zusatzAbo.hauptAbotypId,
        column.kundeId -> zusatzAbo.kundeId,
        column.kunde -> zusatzAbo.kunde,
        column.vertriebsartId -> zusatzAbo.vertriebsartId,
        column.vertriebId -> zusatzAbo.vertriebId,
        column.vertriebBeschrieb -> zusatzAbo.vertriebBeschrieb,
        column.abotypId -> zusatzAbo.abotypId,
        column.abotypName -> zusatzAbo.abotypName,
        column.start -> zusatzAbo.start,
        column.ende -> zusatzAbo.ende,
        column.guthabenVertraglich -> zusatzAbo.guthabenVertraglich,
        column.guthaben -> zusatzAbo.guthaben,
        column.guthabenInRechnung -> zusatzAbo.guthabenInRechnung,
        column.letzteLieferung -> zusatzAbo.letzteLieferung,
        column.anzahlAbwesenheiten -> zusatzAbo.anzahlAbwesenheiten,
        column.anzahlLieferungen -> zusatzAbo.anzahlLieferungen,
        column.aktiv -> zusatzAbo.aktiv
      )
    }
  }

  implicit val produktMapping = new BaseEntitySQLSyntaxSupport[Produkt] {
    override val tableName = "Produkt"

    override lazy val columns = autoColumns[Produkt]()

    def apply(rn: ResultName[Produkt])(rs: WrappedResultSet): Produkt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produkt): Seq[ParameterBinder] = parameters(Produkt.unapply(entity).get)

    override def updateParameters(produkt: Produkt) = {
      super.updateParameters(produkt) ++ Seq(
        column.name -> produkt.name,
        column.verfuegbarVon -> produkt.verfuegbarVon,
        column.verfuegbarBis -> produkt.verfuegbarBis,
        column.kategorien -> produkt.kategorien,
        column.standardmenge -> produkt.standardmenge,
        column.einheit -> produkt.einheit,
        column.preis -> produkt.preis,
        column.produzenten -> produkt.produzenten
      )
    }
  }

  implicit val produzentMapping = new BaseEntitySQLSyntaxSupport[Produzent] {
    override val tableName = "Produzent"

    override lazy val columns = autoColumns[Produzent]()

    def apply(rn: ResultName[Produzent])(rs: WrappedResultSet): Produzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produzent): Seq[ParameterBinder] = parameters(Produzent.unapply(entity).get)

    override def updateParameters(produzent: Produzent) = {
      super.updateParameters(produzent) ++ Seq(
        column.name -> produzent.name,
        column.vorname -> produzent.vorname,
        column.kurzzeichen -> produzent.kurzzeichen,
        column.strasse -> produzent.strasse,
        column.hausNummer -> produzent.hausNummer,
        column.adressZusatz -> produzent.adressZusatz,
        column.plz -> produzent.plz,
        column.ort -> produzent.ort,
        column.bemerkungen -> produzent.bemerkungen,
        column.email -> produzent.email,
        column.telefonMobil -> produzent.telefonMobil,
        column.telefonFestnetz -> produzent.telefonFestnetz,
        column.iban -> produzent.iban,
        column.bank -> produzent.bank,
        column.mwst -> produzent.mwst,
        column.mwstSatz -> produzent.mwstSatz,
        column.mwstNr -> produzent.mwstNr,
        column.aktiv -> produzent.aktiv
      )
    }
  }

  implicit val produktekategorieMapping = new BaseEntitySQLSyntaxSupport[Produktekategorie] {
    override val tableName = "Produktekategorie"

    override lazy val columns = autoColumns[Produktekategorie]()

    def apply(rn: ResultName[Produktekategorie])(rs: WrappedResultSet): Produktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Produktekategorie): Seq[ParameterBinder] = parameters(Produktekategorie.unapply(entity).get)

    override def updateParameters(produktekategorie: Produktekategorie) = {
      super.updateParameters(produktekategorie) ++ Seq(
        column.beschreibung -> produktekategorie.beschreibung
      )
    }
  }

  implicit val projektMapping = new BaseEntitySQLSyntaxSupport[Projekt] {
    override val tableName = "Projekt"

    override lazy val columns = autoColumns[Projekt]()

    def apply(rn: ResultName[Projekt])(rs: WrappedResultSet): Projekt =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Projekt): Seq[ParameterBinder] = parameters(Projekt.unapply(entity).get)

    override def updateParameters(projekt: Projekt) = {
      super.updateParameters(projekt) ++ Seq(
        column.bezeichnung -> projekt.bezeichnung,
        column.strasse -> projekt.strasse,
        column.hausNummer -> projekt.hausNummer,
        column.adressZusatz -> projekt.adressZusatz,
        column.plz -> projekt.plz,
        column.ort -> projekt.ort,
        column.preiseSichtbar -> projekt.preiseSichtbar,
        column.preiseEditierbar -> projekt.preiseEditierbar,
        column.emailErforderlich -> projekt.emailErforderlich,
        column.waehrung -> projekt.waehrung,
        column.geschaeftsjahrMonat -> projekt.geschaeftsjahrMonat,
        column.geschaeftsjahrTag -> projekt.geschaeftsjahrTag,
        column.twoFactorAuthentication -> projekt.twoFactorAuthentication,
        column.sprache -> projekt.sprache,
        column.welcomeMessage1 -> projekt.welcomeMessage1,
        column.welcomeMessage2 -> projekt.welcomeMessage2,
        column.maintenanceMode -> projekt.maintenanceMode
      )
    }
  }

  implicit val produktProduzentMapping = new BaseEntitySQLSyntaxSupport[ProduktProduzent] {
    override val tableName = "ProduktProduzent"

    override lazy val columns = autoColumns[ProduktProduzent]()

    def apply(rn: ResultName[ProduktProduzent])(rs: WrappedResultSet): ProduktProduzent =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduzent): Seq[ParameterBinder] = parameters(ProduktProduzent.unapply(entity).get)

    override def updateParameters(projekt: ProduktProduzent) = {
      super.updateParameters(projekt) ++ Seq(
        column.produktId -> projekt.produktId,
        column.produzentId -> projekt.produzentId
      )
    }
  }

  implicit val produktProduktekategorieMapping = new BaseEntitySQLSyntaxSupport[ProduktProduktekategorie] {
    override val tableName = "ProduktProduktekategorie"

    override lazy val columns = autoColumns[ProduktProduktekategorie]()

    def apply(rn: ResultName[ProduktProduktekategorie])(rs: WrappedResultSet): ProduktProduktekategorie =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProduktProduktekategorie): Seq[ParameterBinder] = parameters(ProduktProduktekategorie.unapply(entity).get)

    override def updateParameters(produktkat: ProduktProduktekategorie) = {
      super.updateParameters(produktkat) ++ Seq(
        column.produktId -> produktkat.produktId,
        column.produktekategorieId -> produktkat.produktekategorieId
      )
    }
  }

  implicit val abwesenheitMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit = autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[ParameterBinder] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit) = {
      super.updateParameters(entity) ++ Seq(
        column.aboId -> entity.aboId,
        column.lieferungId -> entity.lieferungId,
        column.datum -> entity.datum,
        column.bemerkung -> entity.bemerkung
      )
    }
  }

  implicit val tourlieferungMapping = new BaseEntitySQLSyntaxSupport[Tourlieferung] {
    override val tableName = "Tourlieferung"

    override lazy val columns = autoColumns[Tourlieferung]()

    def apply(rn: ResultName[Tourlieferung])(rs: WrappedResultSet): Tourlieferung = autoConstruct(rs, rn)

    def parameterMappings(entity: Tourlieferung): Seq[ParameterBinder] = parameters(Tourlieferung.unapply(entity).get)

    override def updateParameters(entity: Tourlieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.sort -> entity.sort
      )
    }
  }

  implicit val korbMapping = new BaseEntitySQLSyntaxSupport[Korb] {
    override val tableName = "Korb"

    override lazy val columns = autoColumns[Korb]()

    def apply(rn: ResultName[Korb])(rs: WrappedResultSet): Korb =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Korb): Seq[ParameterBinder] = parameters(Korb.unapply(entity).get)

    override def updateParameters(entity: Korb) = {
      super.updateParameters(entity) ++ Seq(
        column.lieferungId -> entity.lieferungId,
        column.aboId -> entity.aboId,
        column.status -> entity.status,
        column.auslieferungId -> entity.auslieferungId,
        column.guthabenVorLieferung -> entity.guthabenVorLieferung,
        column.sort -> entity.sort
      )
    }
  }

  trait AuslieferungMapping[E <: Auslieferung] extends BaseEntitySQLSyntaxSupport[E] {
    override def updateParameters(auslieferung: E) = {
      super.updateParameters(auslieferung) ++ Seq(
        column.status -> auslieferung.status,
        column.datum -> auslieferung.datum,
        column.anzahlKoerbe -> auslieferung.anzahlKoerbe
      )
    }
  }

  implicit val depotAuslieferungMapping = new AuslieferungMapping[DepotAuslieferung] {
    override val tableName = "DepotAuslieferung"

    override lazy val columns = autoColumns[DepotAuslieferung]()

    def apply(rn: ResultName[DepotAuslieferung])(rs: WrappedResultSet): DepotAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DepotAuslieferung): Seq[ParameterBinder] =
      parameters(DepotAuslieferung.unapply(entity).get)

    override def updateParameters(entity: DepotAuslieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.depotId -> entity.depotId,
        column.depotName -> entity.depotName
      )
    }
  }

  implicit val tourAuslieferungMapping = new AuslieferungMapping[TourAuslieferung] {
    override val tableName = "TourAuslieferung"

    override lazy val columns = autoColumns[TourAuslieferung]()

    def apply(rn: ResultName[TourAuslieferung])(rs: WrappedResultSet): TourAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: TourAuslieferung): Seq[ParameterBinder] =
      parameters(TourAuslieferung.unapply(entity).get)

    override def updateParameters(entity: TourAuslieferung) = {
      super.updateParameters(entity) ++ Seq(
        column.tourId -> entity.tourId,
        column.tourName -> entity.tourName
      )
    }
  }

  implicit val postAuslieferungMapping = new AuslieferungMapping[PostAuslieferung] {
    override val tableName = "PostAuslieferung"

    override lazy val columns = autoColumns[PostAuslieferung]()

    def apply(rn: ResultName[PostAuslieferung])(rs: WrappedResultSet): PostAuslieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostAuslieferung): Seq[ParameterBinder] =
      parameters(PostAuslieferung.unapply(entity).get)
  }

  implicit val projektVorlageMapping = new BaseEntitySQLSyntaxSupport[ProjektVorlage] {
    override val tableName = "ProjektVorlage"

    override lazy val columns = autoColumns[ProjektVorlage]()

    def apply(rn: ResultName[ProjektVorlage])(rs: WrappedResultSet): ProjektVorlage =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ProjektVorlage): Seq[ParameterBinder] =
      parameters(ProjektVorlage.unapply(entity).get)

    override def updateParameters(entity: ProjektVorlage) = {
      super.updateParameters(entity) ++ Seq(
        column.name -> entity.name,
        column.beschreibung -> entity.beschreibung,
        column.fileStoreId -> entity.fileStoreId
      )
    }
  }

  implicit val einladungMapping = new BaseEntitySQLSyntaxSupport[Einladung] {
    override val tableName = "Einladung"

    override lazy val columns = autoColumns[Einladung]()

    def apply(rn: ResultName[Einladung])(rs: WrappedResultSet): Einladung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Einladung): Seq[ParameterBinder] =
      parameters(Einladung.unapply(entity).get)

    override def updateParameters(entity: Einladung) = {
      super.updateParameters(entity) ++ Seq(
        column.expires -> entity.expires,
        column.datumVersendet -> entity.datumVersendet
      )
    }
  }

  implicit val kontoDatenMapping = new BaseEntitySQLSyntaxSupport[KontoDaten] {
    override val tableName = "KontoDaten"

    override lazy val columns = autoColumns[KontoDaten]()

    def apply(rn: ResultName[KontoDaten])(rs: WrappedResultSet): KontoDaten =
      autoConstruct(rs, rn)

    def parameterMappings(entity: KontoDaten): Seq[ParameterBinder] =
      parameters(KontoDaten.unapply(entity).get)

    override def updateParameters(entity: KontoDaten) = {
      super.updateParameters(entity) ++ Seq(
        column.iban -> entity.iban,
        column.teilnehmerNummer -> entity.teilnehmerNummer,
        column.referenzNummerPrefix -> entity.referenzNummerPrefix
      )
    }
  }
}
