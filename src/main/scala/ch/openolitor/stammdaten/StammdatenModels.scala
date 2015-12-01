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

import java.time._
import ch.openolitor.core.models.BaseEntity
import java.util.UUID
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.models.BaseId
import scalikejdbc.SQLSyntaxSupportFeature._
import scalikejdbc._
import DBUtils._
import ch.openolitor.core.models.BaseId
import java.sql.ResultSet
import org.joda.time.DateTime
import ch.openolitor.core.models.BaseEntity
import ch.openolitor.core.models.BaseId
import ch.openolitor.core.models.BaseEntity
import scala.concurrent.ExecutionContext

sealed trait Lieferzeitpunkt extends Product
sealed trait Wochentag extends Lieferzeitpunkt

object Lieferzeitpunkt {
  def apply(value: String): Lieferzeitpunkt = Wochentag.apply(value).get
}

case object Montag extends Wochentag
case object Dienstag extends Wochentag
case object Mittwoch extends Wochentag
case object Donnerstag extends Wochentag
case object Freitag extends Wochentag
case object Samstag extends Wochentag
case object Sonntag extends Wochentag

object Wochentag {
  def apply(value: String): Option[Wochentag] = {
    Vector(Montag, Dienstag, Mittwoch, Donnerstag, Freitag, Samstag, Sonntag).find(_.toString == value)
  }
}

sealed trait Rhythmus
case object Woechentlich extends Rhythmus
case object Zweiwoechentlich extends Rhythmus
case object Monatlich extends Rhythmus

object Rhythmus {
  def apply(value: String): Rhythmus = {
    Vector(Woechentlich, Zweiwoechentlich, Monatlich).find(_.toString == value).getOrElse(Woechentlich)
  }
}

sealed trait Preiseinheit
case object Lieferung extends Preiseinheit
case object Monat extends Preiseinheit
case object Jahr extends Preiseinheit
case object Aboende extends Preiseinheit

object Preiseinheit {
  def apply(value: String): Preiseinheit = {
    Vector(Lieferung, Monat, Jahr, Aboende).find(_.toString == value).getOrElse(Jahr)
  }
}

case class VertriebsartId(id: UUID) extends BaseId
sealed trait Vertriebsart extends BaseEntity[VertriebsartId]
case class Depotlieferung(id: Option[VertriebsartId], abotypId: AbotypId, depotId: DepotId, liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsart
case class Heimlieferung(id: Option[VertriebsartId], abotypId: AbotypId, tourId: TourId, liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsart
case class Postlieferung(id: Option[VertriebsartId], abotypId: AbotypId, liefertage: Seq[Lieferzeitpunkt]) extends Vertriebsart

sealed trait Waehrung
case object CHF extends Waehrung
case object EUR extends Waehrung
case object USD extends Waehrung

object Waehrung {
  def apply(value: String): Waehrung = {
    Vector(CHF, EUR, USD).find(_.toString == value).getOrElse(CHF)
  }
}

trait IAbotyp {
  val id: Option[AbotypId]
  val name: String
  val beschreibung: Option[String]
  val lieferrhythmus: Rhythmus
  val enddatum: Option[DateTime]
  val anzahlLieferungen: Option[Int]
  val anzahlAbwesenheiten: Option[Int]
  val preis: BigDecimal
  val preisEinheit: Preiseinheit
  val aktiv: Boolean
}

case class AbotypId(id: UUID) extends BaseId
case class Abotyp(id: Option[AbotypId],
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime],
  anzahlLieferungen: Option[Int],
  anzahlAbwesenheiten: Option[Int],
  preis: BigDecimal,
  preisEinheit: Preiseinheit,
  aktiv: Boolean,
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  letzteLieferung: Option[DateTime]) extends BaseEntity[AbotypId] with IAbotyp

case class Projekt(id: UUID,
  name: String,
  waehrung: Waehrung)

sealed trait Vertriebskanal {
  val name: String
  val beschreibung: Option[String]
}

case class DepotId(id: UUID) extends BaseId
case class Depot(id: Option[DepotId], name: String, beschreibung: Option[String]) extends BaseEntity[DepotId] with Vertriebskanal

case class TourId(id: UUID) extends BaseId
case class Tour(id: Option[TourId], name: String, beschreibung: Option[String]) extends BaseEntity[TourId] with Vertriebskanal

//DB Model bindig

object Abotyp extends SQLSyntaxSupport[Abotyp] {
  override val tableName = "Abotyp"

  override def columnNames = Seq("id", "name", "beschreibung", "lieferrhythmus", "enddatum", "anzahl_lieferungen", "anzahl_abwesenheiten", "preis", "preisEinheit", "aktiv")

  def apply(p: SyntaxProvider[Abotyp])(rs: WrappedResultSet): Abotyp = apply(p.resultName)(rs)

  def apply(rn: ResultName[Abotyp])(rs: WrappedResultSet): Abotyp =
    autoConstruct(rs, rn)
}

object Tour extends SQLSyntaxSupport[Tour] {
  override val tableName = "Tour"

  override def columnNames = Seq("id", "name", "beschreibung")

  def apply(p: SyntaxProvider[Tour])(rs: WrappedResultSet): Tour = apply(p.resultName)(rs)

  def apply(rn: ResultName[Tour])(rs: WrappedResultSet): Tour =
    autoConstruct(rs, rn)

  def opt(m: SyntaxProvider[Tour])(rs: WrappedResultSet): Option[Tour] =
    rs.longOpt(m.resultName.id).map(_ => Tour(m)(rs))
}

object Depot extends SQLSyntaxSupport[Depot] {
  override val tableName = "Depot"

  override def columnNames = Seq("id", "name", "beschreibung")

  def apply(p: SyntaxProvider[Depot])(rs: WrappedResultSet): Depot = apply(p.resultName)(rs)

  def apply(rn: ResultName[Depot])(rs: WrappedResultSet): Depot =
    autoConstruct(rs, rn)

  def opt(m: SyntaxProvider[Depot])(rs: WrappedResultSet): Option[Depot] =
    rs.longOpt(m.resultName.id).map(_ => Depot(m)(rs))
}

object Heimlieferung extends SQLSyntaxSupport[Heimlieferung] {
  override val tableName = "Heimlieferung"

  override def columnNames = Seq("id", "abo_typ_id", "tour_id", "liefertage")

  def apply(p: SyntaxProvider[Heimlieferung])(rs: WrappedResultSet): Heimlieferung = apply(p.resultName)(rs)

  def apply(rn: ResultName[Heimlieferung])(rs: WrappedResultSet): Heimlieferung =
    autoConstruct(rs, rn)

  def opt(m: SyntaxProvider[Heimlieferung])(rs: WrappedResultSet): Option[Heimlieferung] =
    rs.longOpt(m.resultName.id).map(_ => Heimlieferung(m)(rs))
}

object Depotlieferung extends SQLSyntaxSupport[Depotlieferung] {
  override val tableName = "Depotlieferung"

  override def columnNames = Seq("id", "abo_typ_id", "depot_id", "liefertage")

  def apply(p: SyntaxProvider[Depotlieferung])(rs: WrappedResultSet): Depotlieferung = apply(p.resultName)(rs)

  def apply(rn: ResultName[Depotlieferung])(rs: WrappedResultSet): Depotlieferung =
    autoConstruct(rs, rn)

  def opt(m: SyntaxProvider[Depotlieferung])(rs: WrappedResultSet): Option[Depotlieferung] =
    rs.longOpt(m.resultName.id).map(_ => Depotlieferung(m)(rs))
}

object Postlieferung extends SQLSyntaxSupport[Postlieferung] {
  override val tableName = "Postlieferung"

  override def columnNames = Seq("id", "abo_typ_id", "liefertage")

  def apply(p: SyntaxProvider[Postlieferung])(rs: WrappedResultSet): Postlieferung = apply(p.resultName)(rs)

  def apply(rn: ResultName[Postlieferung])(rs: WrappedResultSet): Postlieferung =
    autoConstruct(rs, rn)

  def opt(m: SyntaxProvider[Postlieferung])(rs: WrappedResultSet): Option[Postlieferung] =
    rs.longOpt(m.resultName.id).map(_ => Postlieferung(m)(rs))
}

object DBUtils {
  import TypeBinder._

  // DB type binders
  implicit val tourIdBinder: TypeBinder[TourId] = baseIdTypeBinder[TourId](TourId.apply _)
  implicit val depotIdBinder: TypeBinder[DepotId] = baseIdTypeBinder[DepotId](DepotId.apply _)
  implicit val aboTypIdBinder: TypeBinder[AbotypId] = baseIdTypeBinder[AbotypId](AbotypId.apply _)
  implicit val vertriebsartIdBinder: TypeBinder[VertriebsartId] = baseIdTypeBinder[VertriebsartId](VertriebsartId.apply _)

  implicit val rhythmusTypeBinder: TypeBinder[Rhythmus] = string.map(Rhythmus.apply)
  implicit val waehrungTypeBinder: TypeBinder[Waehrung] = string.map(Waehrung.apply)
  implicit val preiseinheitTypeBinder: TypeBinder[Preiseinheit] = string.map(Preiseinheit.apply)
  implicit val lieferzeitpunktTypeBinder: TypeBinder[Lieferzeitpunkt] = string.map(Lieferzeitpunkt.apply)
  implicit val lieferzeitpunktSeqTypeBinder: TypeBinder[Seq[Lieferzeitpunkt]] = string.map(s => s.split(",").map(Lieferzeitpunkt.apply))

  def baseIdTypeBinder[T <: BaseId](implicit f: UUID => T): TypeBinder[T] = string.map(s => f(UUID.fromString(s)))
}

