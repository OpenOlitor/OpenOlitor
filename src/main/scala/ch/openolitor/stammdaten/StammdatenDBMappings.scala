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
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder[TourId](TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder[DepotId](DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder[AbotypId](AbotypId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder[VertriebsartId](VertriebsartId.apply _)
  implicit val personIdBinder: TypeBinder[PersonId] = baseIdTypeBinder[PersonId](PersonId.apply _)

  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSetTypeBinder: TypeBinder[Set[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply).toSet)
  implicit val personenTypBinder: TypeBinder[Option[Personentyp]] = string.map(Personentyp.apply)
  implicit val personenTypSetBinder: TypeBinder[Set[Personentyp]] = string.map(s => s.split(",").map(Personentyp.apply).toSet.flatten)

  //DB parameter binders for write and query operations
  implicit val rhytmusSqlBinder = toStringSqlBinder[Rhythmus]
  implicit val preiseinheitSqlBinder = toStringSqlBinder[Preiseinheit]
  implicit val waehrungSqlBinder = toStringSqlBinder[Waehrung]
  implicit val lieferzeipunktSqlBinder = toStringSqlBinder[Lieferzeitpunkt]
  implicit val lieferzeitpunktSetSqlBinder = setSqlBinder[Lieferzeitpunkt]
  implicit val abotypIdSqlBinder = baseIdSqlBinder[AbotypId]
  implicit val depotIdSqlBinder = baseIdSqlBinder[DepotId]
  implicit val tourIdSqlBinder = baseIdSqlBinder[TourId]
  implicit val vertriebsartIdSqlBinder = baseIdSqlBinder[VertriebsartId]
  implicit val personIdSqlBinder = baseIdSqlBinder[PersonId]
  implicit val personenTypSqlBinder = toStringSqlBinder[Personentyp]
  implicit val personenTypSetSqlBinder = setSqlBinder[Personentyp]

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
        column.enddatum -> parameter(abotyp.enddatum),
        column.anzahlLieferungen -> parameter(abotyp.anzahlLieferungen),
        column.anzahlAbwesenheiten -> parameter(abotyp.anzahlAbwesenheiten),
        column.preis -> parameter(abotyp.preis),
        column.preiseinheit -> parameter(abotyp.preiseinheit),
        column.aktiv -> parameter(abotyp.aktiv),
        column.anzahlAbonnenten -> parameter(abotyp.anzahlAbonnenten),
        column.letzteLieferung -> parameter(abotyp.letzteLieferung),
        column.waehrung -> parameter(abotyp.waehrung))
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
      Seq(column.name -> parameter(person.name),
        column.vorname -> parameter(person.vorname),
        column.strasse -> parameter(person.strasse),
        column.hausNummer -> parameter(person.hausNummer),
        column.plz -> parameter(person.plz),
        column.ort -> parameter(person.ort),
        column.typen -> parameter(person.typen),
        column.email -> parameter(person.email))
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
}
