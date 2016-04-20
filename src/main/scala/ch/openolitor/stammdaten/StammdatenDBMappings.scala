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

//DB Model bindig
trait StammdatenDBMappings extends DBMappings with LazyLogging {
  import TypeBinder._

  // DB type binders for read operations
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder(TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder(DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder(AbotypId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder(VertriebsartId.apply _)
  implicit val kundeIdBinder: TypeBinder[KundeId] = baseIdTypeBinder(KundeId.apply _)
  implicit val personIdBinder: TypeBinder[PersonId] = baseIdTypeBinder(PersonId.apply _)
  implicit val pendenzIdBinder: TypeBinder[PendenzId] = baseIdTypeBinder(PendenzId.apply _)
  implicit val aboIdBinder: TypeBinder[AboId] = baseIdTypeBinder(AboId.apply _)
  implicit val lierferungIdBinder: TypeBinder[LieferungId] = baseIdTypeBinder(LieferungId.apply _)
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

  implicit val pendenzStatusTypeBinder: TypeBinder[PendenzStatus] = string.map(PendenzStatus.apply)
  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val lieferungStatusTypeBinder: TypeBinder[LieferungStatus] = string.map(LieferungStatus.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetTypeBinder: TypeBinder[Set[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply).toSet)
  implicit val kundenTypIdSetBinder: TypeBinder[Set[KundentypId]] = string.map(s => s.split(",").map(KundentypId.apply).toSet)
  implicit val laufzeiteinheitTypeBinder: TypeBinder[Laufzeiteinheit] = string.map(Laufzeiteinheit.apply)
  implicit val liefereinheitypeBinder: TypeBinder[Liefereinheit] = string.map(Liefereinheit.apply)
  implicit val liefersaisonTypeBinder: TypeBinder[Liefersaison] = string.map(Liefersaison.apply)
  implicit val anredeTypeBinder: TypeBinder[Anrede] = string.map(Anrede.apply)

  implicit val baseProduktekategorieIdSetTypeBinder: TypeBinder[Set[BaseProduktekategorieId]] = string.map(s => s.split(",").map(BaseProduktekategorieId.apply).toSet)
  implicit val baseProduzentIdSetTypeBinder: TypeBinder[Set[BaseProduzentId]] = string.map(s => s.split(",").map(BaseProduzentId.apply).toSet)
  implicit val stringIntTreeMapTypeBinder: TypeBinder[TreeMap[String, Int]] = string.map(s => (TreeMap.empty[String, Int] /: s.split(",")) { (tree, str) =>
    str.split("=") match {
      case Array(left, right) =>
        tree + (left -> right.toInt)
      case _ =>
        tree
    }
  })

  implicit val stringSeqTypeBinder: TypeBinder[Seq[String]] = string.map(s => s.split(",").map(c => c).toSeq)

  //DB parameter binders for write and query operationsit
  implicit val pendenzStatusBinder = toStringSqlBinder[PendenzStatus]
  implicit val rhytmusSqlBinder = toStringSqlBinder[Rhythmus]
  implicit val preiseinheitSqlBinder = toStringSqlBinder[Preiseinheit]
  implicit val waehrungSqlBinder = toStringSqlBinder[Waehrung]
  implicit val lieferungStatusSqlBinder = toStringSqlBinder[LieferungStatus]
  implicit val lieferzeitpunktSqlBinder = toStringSqlBinder[Lieferzeitpunkt]
  implicit val lieferzeitpunktSetSqlBinder = setSqlBinder[Lieferzeitpunkt]
  implicit val laufzeiteinheitSqlBinder = toStringSqlBinder[Laufzeiteinheit]
  implicit val liefereinheitSqlBinder = toStringSqlBinder[Liefereinheit]
  implicit val liefersaisonSqlBinder = toStringSqlBinder[Liefersaison]
  implicit val anredeSqlBinder = toStringSqlBinder[Anrede]
  implicit val optionAnredeSqlBinder = optionSqlBinder[Anrede]

  implicit val abotypIdSqlBinder = baseIdSqlBinder[AbotypId]
  implicit val depotIdSqlBinder = baseIdSqlBinder[DepotId]
  implicit val tourIdSqlBinder = baseIdSqlBinder[TourId]
  implicit val vertriebsartIdSqlBinder = baseIdSqlBinder[VertriebsartId]
  implicit val personIdSqlBinder = baseIdSqlBinder[PersonId]
  implicit val kundeIdSqlBinder = baseIdSqlBinder[KundeId]
  implicit val pendenzIdSqlBinder = baseIdSqlBinder[PendenzId]
  implicit val customKundentypIdSqlBinder = baseIdSqlBinder[CustomKundentypId]
  implicit val kundentypIdSqlBinder = new SqlBinder[KundentypId] { def apply(value: KundentypId): Any = value.id }
  implicit val kundentypIdSetSqlBinder = setSqlBinder[KundentypId]
  implicit val aboIdSqlBinder = baseIdSqlBinder[AboId]
  implicit val lieferungIdSqlBinder = baseIdSqlBinder[LieferungId]
  implicit val produktIdSqlBinder = baseIdSqlBinder[ProduktId]
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
  implicit val stringIntTreeMapSqlBinder = treeMapSqlBinder[String, Int]

  implicit val stringSeqSqlBinder = seqSqlBinder[String]

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp = autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[Any] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(abotyp: Abotyp) = {
      super.updateParameters(abotyp) ++ Seq(column.name -> parameter(abotyp.name),
        column.beschreibung -> parameter(abotyp.beschreibung),
        column.lieferrhythmus -> parameter(abotyp.lieferrhythmus),
        column.aktivVon -> parameter(abotyp.aktivVon),
        column.aktivBis -> parameter(abotyp.aktivBis),
        column.laufzeit -> parameter(abotyp.laufzeit),
        column.laufzeiteinheit -> parameter(abotyp.laufzeiteinheit),
        column.anzahlAbwesenheiten -> parameter(abotyp.anzahlAbwesenheiten),
        column.preis -> parameter(abotyp.preis),
        column.preiseinheit -> parameter(abotyp.preiseinheit),
        column.farbCode -> parameter(abotyp.farbCode),
        column.zielpreis -> parameter(abotyp.zielpreis),
        column.anzahlAbonnenten -> parameter(abotyp.anzahlAbonnenten),
        column.letzteLieferung -> parameter(abotyp.letzteLieferung),
        column.waehrung -> parameter(abotyp.waehrung),
        column.saldoMindestbestand -> parameter(abotyp.saldoMindestbestand))
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
      super.updateParameters(typ) ++ Seq(column.kundentyp -> parameter(typ.kundentyp),
        column.beschreibung -> parameter(typ.beschreibung),
        column.anzahlVerknuepfungen -> parameter(typ.anzahlVerknuepfungen))
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
      super.updateParameters(kunde) ++ Seq(column.bezeichnung -> parameter(kunde.bezeichnung),
        column.strasse -> parameter(kunde.strasse),
        column.hausNummer -> parameter(kunde.hausNummer),
        column.adressZusatz -> parameter(kunde.adressZusatz),
        column.plz -> parameter(kunde.plz),
        column.ort -> parameter(kunde.ort),
        column.strasseLieferung -> parameter(kunde.strasseLieferung),
        column.hausNummerLieferung -> parameter(kunde.hausNummerLieferung),
        column.plzLieferung -> parameter(kunde.plzLieferung),
        column.ortLieferung -> parameter(kunde.ortLieferung),
        column.typen -> parameter(kunde.typen),
        column.bemerkungen -> parameter(kunde.bemerkungen),
        column.anzahlAbos -> parameter(kunde.anzahlAbos),
        column.anzahlPendenzen -> parameter(kunde.anzahlPendenzen),
        column.anzahlPersonen -> parameter(kunde.anzahlPersonen))
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
      super.updateParameters(person) ++ Seq(column.kundeId -> parameter(person.kundeId),
        column.anrede -> parameter(person.anrede),
        column.name -> parameter(person.name),
        column.vorname -> parameter(person.vorname),
        column.email -> parameter(person.email),
        column.emailAlternative -> parameter(person.emailAlternative),
        column.telefonMobil -> parameter(person.telefonMobil),
        column.telefonFestnetz -> parameter(person.telefonFestnetz),
        column.bemerkungen -> parameter(person.bemerkungen),
        column.sort -> parameter(person.sort))
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
      super.updateParameters(pendenz) ++ Seq(column.kundeId -> parameter(pendenz.kundeId),
        column.kundeBezeichnung -> parameter(pendenz.kundeBezeichnung),
        column.datum -> parameter(pendenz.datum),
        column.bemerkung -> parameter(pendenz.bemerkung),
        column.status -> parameter(pendenz.status),
        column.generiert -> parameter(pendenz.generiert))
    }
  }

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[Any] = parameters(Lieferung.unapply(entity).get)

    override def updateParameters(lieferung: Lieferung) = {
      super.updateParameters(lieferung) ++ Seq(column.anzahlAbwesenheiten -> parameter(lieferung.anzahlAbwesenheiten))
    }
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[Any] = parameters(Tour.unapply(entity).get)

    override def updateParameters(tour: Tour) = {
      super.updateParameters(tour) ++ Seq(column.name -> parameter(tour.name),
        column.beschreibung -> parameter(tour.beschreibung))
    }
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[Any] = parameters(Depot.unapply(entity).get)

    override def updateParameters(depot: Depot) = {
      super.updateParameters(depot) ++ Seq(column.name -> parameter(depot.name),
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
        //column.farbCode -> parameter(depot.farbCode),
        column.iban -> parameter(depot.iban),
        column.bank -> parameter(depot.bank),
        column.beschreibung -> parameter(depot.beschreibung),
        column.anzahlAbonnenten -> parameter(depot.anzahlAbonnenten),
        column.anzahlAbonnentenMax -> parameter(depot.anzahlAbonnentenMax))
    }
  }

  implicit val heimlieferungMapping = new BaseEntitySQLSyntaxSupport[Heimlieferung] {
    override val tableName = "Heimlieferung"

    override lazy val columns = autoColumns[Heimlieferung]()

    def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Heimlieferung): Seq[Any] = parameters(Heimlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Heimlieferung) = {
      super.updateParameters(lieferung) ++ Seq(column.liefertag -> parameter(lieferung.liefertag),
        column.tourId -> parameter(lieferung.tourId))
    }
  }

  implicit val depotlieferungMapping = new BaseEntitySQLSyntaxSupport[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[Any] =
      parameters(Depotlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Depotlieferung) = {
      super.updateParameters(lieferung) ++ Seq(column.liefertag -> parameter(lieferung.liefertag),
        column.depotId -> parameter(lieferung.depotId))
    }
  }

  implicit val postlieferungMapping = new BaseEntitySQLSyntaxSupport[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[Any] = parameters(Postlieferung.unapply(entity).get)

    override def updateParameters(lieferung: Postlieferung) = {
      super.updateParameters(lieferung) ++ Seq(column.liefertag -> parameter(lieferung.liefertag))
    }
  }

  implicit val depotlieferungAboMapping = new BaseEntitySQLSyntaxSupport[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo = autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[Any] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(depotlieferungAbo: DepotlieferungAbo) = {
      super.updateParameters(depotlieferungAbo) ++ Seq(
        column.kundeId -> parameter(depotlieferungAbo.kundeId),
        column.kunde -> parameter(depotlieferungAbo.kunde),
        column.abotypId -> parameter(depotlieferungAbo.abotypId),
        column.abotypName -> parameter(depotlieferungAbo.abotypName),
        column.depotId -> parameter(depotlieferungAbo.depotId),
        column.depotName -> parameter(depotlieferungAbo.depotName),
        column.liefertag -> parameter(depotlieferungAbo.liefertag),
        column.start -> parameter(depotlieferungAbo.start),
        column.ende -> parameter(depotlieferungAbo.ende),
        column.saldo -> parameter(depotlieferungAbo.saldo),
        column.saldoInRechnung -> parameter(depotlieferungAbo.saldoInRechnung),
        column.letzteLieferung -> parameter(depotlieferungAbo.letzteLieferung),
        column.anzahlAbwesenheiten -> parameter(depotlieferungAbo.anzahlAbwesenheiten),
        column.anzahlLieferungen -> parameter(depotlieferungAbo.anzahlAbwesenheiten))
    }
  }

  implicit val heimlieferungAboMapping = new BaseEntitySQLSyntaxSupport[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[Any] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(heimlieferungAbo: HeimlieferungAbo) = {
      super.updateParameters(heimlieferungAbo) ++ Seq(
        column.kundeId -> parameter(heimlieferungAbo.kundeId),
        column.kunde -> parameter(heimlieferungAbo.kunde),
        column.abotypId -> parameter(heimlieferungAbo.abotypId),
        column.abotypName -> parameter(heimlieferungAbo.abotypName),
        column.tourId -> parameter(heimlieferungAbo.tourId),
        column.tourName -> parameter(heimlieferungAbo.tourName),
        column.liefertag -> parameter(heimlieferungAbo.liefertag),
        column.start -> parameter(heimlieferungAbo.start),
        column.ende -> parameter(heimlieferungAbo.ende),
        column.saldo -> parameter(heimlieferungAbo.saldo),
        column.saldoInRechnung -> parameter(heimlieferungAbo.saldoInRechnung),
        column.letzteLieferung -> parameter(heimlieferungAbo.letzteLieferung),
        column.anzahlAbwesenheiten -> parameter(heimlieferungAbo.anzahlAbwesenheiten),
        column.anzahlLieferungen -> parameter(heimlieferungAbo.anzahlAbwesenheiten))
    }
  }

  implicit val postlieferungAboMapping = new BaseEntitySQLSyntaxSupport[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[Any] = parameters(PostlieferungAbo.unapply(entity).get)

    override def updateParameters(postlieferungAbo: PostlieferungAbo) = {
      super.updateParameters(postlieferungAbo) ++ Seq(
        column.kundeId -> parameter(postlieferungAbo.kundeId),
        column.kunde -> parameter(postlieferungAbo.kunde),
        column.abotypId -> parameter(postlieferungAbo.abotypId),
        column.abotypName -> parameter(postlieferungAbo.abotypName),
        column.liefertag -> parameter(postlieferungAbo.liefertag),
        column.start -> parameter(postlieferungAbo.start),
        column.ende -> parameter(postlieferungAbo.ende),
        column.saldo -> parameter(postlieferungAbo.saldo),
        column.saldoInRechnung -> parameter(postlieferungAbo.saldoInRechnung),
        column.letzteLieferung -> parameter(postlieferungAbo.letzteLieferung),
        column.anzahlAbwesenheiten -> parameter(postlieferungAbo.anzahlAbwesenheiten),
        column.anzahlLieferungen -> parameter(postlieferungAbo.anzahlAbwesenheiten))
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
        column.produzenten -> parameter(produkt.produzenten))
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
        column.aktiv -> parameter(produzent.aktiv))
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
        column.beschreibung -> parameter(produktekategorie.beschreibung))
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
        column.waehrung -> parameter(projekt.waehrung))
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
        column.produzentId -> parameter(projekt.produzentId))
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
        column.produktekategorieId -> parameter(produktkat.produktekategorieId))
    }
  }

  implicit val abwesenheiMapping = new BaseEntitySQLSyntaxSupport[Abwesenheit] {
    override val tableName = "Abwesenheit"

    override lazy val columns = autoColumns[Abwesenheit]()

    def apply(rn: ResultName[Abwesenheit])(rs: WrappedResultSet): Abwesenheit =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Abwesenheit): Seq[Any] = parameters(Abwesenheit.unapply(entity).get)

    override def updateParameters(entity: Abwesenheit) = {
      super.updateParameters(entity) ++ Seq(
        column.lieferung -> parameter(entity.lieferung),
        column.datum -> parameter(entity.datum),
        column.bemerkung -> parameter(entity.bemerkung))
    }
  }
}
