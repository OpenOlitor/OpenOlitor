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

//DB Model bindig

trait BaseEntitySQLSyntaxSupport[E <: BaseEntity[_]] extends SQLSyntaxSupport[E] with LazyLogging {
  //override def columnNames 
  def apply(p: SyntaxProvider[E])(rs: WrappedResultSet): E = apply(p.resultName)(rs)

  def opt(e: SyntaxProvider[E])(rs: WrappedResultSet): Option[E] = try {
    rs.stringOpt(e.resultName.id).map(_ => apply(e)(rs))
  } catch {
    case e: IllegalArgumentException => None
  }

  def apply(rn: ResultName[E])(rs: WrappedResultSet): E

  /**
   * Declare parameter mappings for all parameters used on insert
   */
  def parameterMappings(entity: E): Seq[Any]

  /**
   * Declare update parameters for this entity used on update. Is by default an empty set
   */
  def updateParameters(entity: E): Seq[Tuple2[SQLSyntax, Any]] = Seq()
}

trait StammdatenDBMappings extends DBMappings {
  import TypeBinder._

  // DB type binders for read operations
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder(TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder(DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder(AbotypId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder(VertriebsartId.apply _)
  implicit val kundeIdBinder: TypeBinder[KundeId] = baseIdTypeBinder(KundeId.apply _)
  implicit val personIdBinder: TypeBinder[PersonId] = baseIdTypeBinder(PersonId.apply _)
  implicit val aboIdBinder: TypeBinder[AboId] = baseIdTypeBinder(AboId.apply _)
  implicit val lierferungIdBinder: TypeBinder[LieferungId] = baseIdTypeBinder(LieferungId.apply _)
  implicit val customKundentypIdBinder: TypeBinder[CustomKundentypId] = baseIdTypeBinder(CustomKundentypId.apply _)
  implicit val kundentypIdBinder: TypeBinder[KundentypId] = string.map(KundentypId)

  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val lieferungStatusTypeBinder: TypeBinder[LieferungStatus] = string.map(LieferungStatus.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetTypeBinder: TypeBinder[Set[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply).toSet)
  implicit val kundenTypIdSetBinder: TypeBinder[Set[KundentypId]] = string.map(s => s.split(",").map(KundentypId.apply).toSet)
  implicit val laufzeiteinheitTypeBinder: TypeBinder[Laufzeiteinheit] = string.map(Laufzeiteinheit.apply)

  //DB parameter binders for write and query operationsit
  implicit val rhytmusSqlBinder = toStringSqlBinder[Rhythmus]
  implicit val preiseinheitSqlBinder = toStringSqlBinder[Preiseinheit]
  implicit val waehrungSqlBinder = toStringSqlBinder[Waehrung]
  implicit val lieferungStatusSqlBinder = toStringSqlBinder[LieferungStatus]
  implicit val lieferzeipunktSqlBinder = toStringSqlBinder[Lieferzeitpunkt]
  implicit val lieferzeitpunktSetSqlBinder = setSqlBinder[Lieferzeitpunkt]
  implicit val laufzeiteinheitSqlBinder = toStringSqlBinder[Laufzeiteinheit]
  implicit val abotypIdSqlBinder = baseIdSqlBinder[AbotypId]
  implicit val depotIdSqlBinder = baseIdSqlBinder[DepotId]
  implicit val tourIdSqlBinder = baseIdSqlBinder[TourId]
  implicit val vertriebsartIdSqlBinder = baseIdSqlBinder[VertriebsartId]
  implicit val personIdSqlBinder = baseIdSqlBinder[PersonId]
  implicit val kundeIdSqlBinder = baseIdSqlBinder[KundeId]
  implicit val customKundentypIdSqlBinder = baseIdSqlBinder[CustomKundentypId]
  implicit val kundentypIdSqlBinder = new SqlBinder[KundentypId] { def apply(value: KundentypId): Any = value.id }
  implicit val kundentypIdSetSqlBinder = setSqlBinder[KundentypId]
  implicit val aboIdSqlBinder = baseIdSqlBinder[AboId]
  implicit val lieferungIdSqlBinder = baseIdSqlBinder[LieferungId]

  implicit val abotypMapping = new BaseEntitySQLSyntaxSupport[Abotyp] {
    override val tableName = "Abotyp"

    override lazy val columns = autoColumns[Abotyp]()

    def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Abotyp): Seq[Any] =
      parameters(Abotyp.unapply(entity).get)

    override def updateParameters(abotyp: Abotyp) = {
      Seq(column.name -> parameter(abotyp.name),
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
        column.waehrung -> parameter(abotyp.waehrung))
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
      Seq(column.kundentyp -> parameter(typ.kundentyp),
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
      Seq(column.bezeichnung -> parameter(kunde.bezeichnung),
        column.strasse -> parameter(kunde.strasse),
        column.hausNummer -> parameter(kunde.hausNummer),
        column.plz -> parameter(kunde.plz),
        column.ort -> parameter(kunde.ort),
        column.typen -> parameter(kunde.typen),
        column.bemerkungen -> parameter(kunde.bemerkungen),
        column.anzahlAbos -> parameter(kunde.anzahlAbos),
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
      Seq(column.kundeId -> parameter(person.kundeId),
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

  implicit val lieferungMapping = new BaseEntitySQLSyntaxSupport[Lieferung] {
    override val tableName = "Lieferung"

    override lazy val columns = autoColumns[Lieferung]()

    def apply(rn: ResultName[Lieferung])(rs: WrappedResultSet): Lieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Lieferung): Seq[Any] = parameters(Lieferung.unapply(entity).get)

    override def updateParameters(lieferung: Lieferung) = {
      Seq(column.anzahlAbwesenheiten -> parameter(lieferung.anzahlAbwesenheiten))
    }
  }

  implicit val tourMapping = new BaseEntitySQLSyntaxSupport[Tour] {
    override val tableName = "Tour"

    override lazy val columns = autoColumns[Tour]()

    def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Tour): Seq[Any] = parameters(Tour.unapply(entity).get)
  }

  implicit val depotMapping = new BaseEntitySQLSyntaxSupport[Depot] {
    override val tableName = "Depot"

    override lazy val columns = autoColumns[Depot]()

    def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depot): Seq[Any] = parameters(Depot.unapply(entity).get)

    override def updateParameters(depot: Depot) = {
      Seq(column.name -> parameter(depot.name),
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
  }

  implicit val depotlieferungMapping = new BaseEntitySQLSyntaxSupport[Depotlieferung] {
    override val tableName = "Depotlieferung"

    override lazy val columns = autoColumns[Depotlieferung]()

    def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Depotlieferung): Seq[Any] =
      parameters(Depotlieferung.unapply(entity).get)
  }

  implicit val postlieferungMapping = new BaseEntitySQLSyntaxSupport[Postlieferung] {
    override val tableName = "Postlieferung"

    override lazy val columns = autoColumns[Postlieferung]()

    def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Postlieferung): Seq[Any] = parameters(Postlieferung.unapply(entity).get)
  }

  implicit val depotlieferungAboMapping = new BaseEntitySQLSyntaxSupport[DepotlieferungAbo] {
    override val tableName = "DepotlieferungAbo"

    override lazy val columns = autoColumns[DepotlieferungAbo]()

    def apply(rn: ResultName[DepotlieferungAbo])(rs: WrappedResultSet): DepotlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: DepotlieferungAbo): Seq[Any] = parameters(DepotlieferungAbo.unapply(entity).get)

    override def updateParameters(depotlieferungAbo: DepotlieferungAbo) = {
      Seq(
        column.kundeId -> parameter(depotlieferungAbo.kundeId),
        column.kunde -> parameter(depotlieferungAbo.kunde),
        column.abotypId -> parameter(depotlieferungAbo.abotypId),
        column.abotypName -> parameter(depotlieferungAbo.abotypName),
        column.depotId -> parameter(depotlieferungAbo.depotId),
        column.depotName -> parameter(depotlieferungAbo.depotName),
        column.lieferzeitpunkt -> parameter(depotlieferungAbo.lieferzeitpunkt),
        column.saldo -> parameter(depotlieferungAbo.saldo))
    }
  }

  implicit val heimlieferungAboMapping = new BaseEntitySQLSyntaxSupport[HeimlieferungAbo] {
    override val tableName = "HeimlieferungAbo"

    override lazy val columns = autoColumns[HeimlieferungAbo]()

    def apply(rn: ResultName[HeimlieferungAbo])(rs: WrappedResultSet): HeimlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: HeimlieferungAbo): Seq[Any] = parameters(HeimlieferungAbo.unapply(entity).get)

    override def updateParameters(heimlieferungAbo: HeimlieferungAbo) = {
      Seq(
        column.kundeId -> parameter(heimlieferungAbo.kundeId),
        column.kunde -> parameter(heimlieferungAbo.kunde),
        column.abotypId -> parameter(heimlieferungAbo.abotypId),
        column.abotypName -> parameter(heimlieferungAbo.abotypName),
        column.tourId -> parameter(heimlieferungAbo.tourId),
        column.tourName -> parameter(heimlieferungAbo.tourName),
        column.lieferzeitpunkt -> parameter(heimlieferungAbo.lieferzeitpunkt),
        column.saldo -> parameter(heimlieferungAbo.saldo))
    }
  }

  implicit val postlieferungAboMapping = new BaseEntitySQLSyntaxSupport[PostlieferungAbo] {
    override val tableName = "PostlieferungAbo"

    override lazy val columns = autoColumns[PostlieferungAbo]()

    def apply(rn: ResultName[PostlieferungAbo])(rs: WrappedResultSet): PostlieferungAbo =
      autoConstruct(rs, rn)

    def parameterMappings(entity: PostlieferungAbo): Seq[Any] = parameters(PostlieferungAbo.unapply(entity).get)

    override def updateParameters(postlieferungAbo: PostlieferungAbo) = {
      Seq(
        column.kundeId -> parameter(postlieferungAbo.kundeId),
        column.kunde -> parameter(postlieferungAbo.kunde),
        column.abotypId -> parameter(postlieferungAbo.abotypId),
        column.abotypName -> parameter(postlieferungAbo.abotypName),
        column.lieferzeitpunkt -> parameter(postlieferungAbo.lieferzeitpunkt))
    }
  }
}
