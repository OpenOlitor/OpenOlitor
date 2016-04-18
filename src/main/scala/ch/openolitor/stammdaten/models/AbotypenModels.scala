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
package ch.openolitor.stammdaten.models

import ch.openolitor.stammdaten._
import org.joda.time.DateTime
import ch.openolitor.core.models._
import scalikejdbc._
import java.util.UUID
import ch.openolitor.core.JSONSerializable

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
case object Unregelmaessig extends Rhythmus

object Rhythmus {
  def apply(value: String): Rhythmus = {
    Vector(Woechentlich, Zweiwoechentlich, Monatlich, Unregelmaessig).find(_.toString == value).getOrElse(Woechentlich)
  }
}

sealed trait Preiseinheit extends Product
case object ProLieferung extends Preiseinheit
case object ProMonat extends Preiseinheit
case object ProQuartal extends Preiseinheit
case object ProJahr extends Preiseinheit
case object ProAbo extends Preiseinheit

object Preiseinheit {
  def apply(value: String): Preiseinheit = {
    Vector(ProLieferung, ProMonat, ProMonat, ProJahr, ProAbo).find(_.toString == value).getOrElse(ProLieferung)
  }
}

sealed trait Laufzeiteinheit
case object Lieferungen extends Laufzeiteinheit
case object Monate extends Laufzeiteinheit
case object Unbeschraenkt extends Laufzeiteinheit

object Laufzeiteinheit {
  def apply(value: String): Laufzeiteinheit = {
    Vector(Unbeschraenkt, Lieferungen, Monate).find(_.toString == value).getOrElse(Lieferungen)
  }
}

trait AktivRange {
  val aktivVon: Option[DateTime]
  val aktivBis: Option[DateTime]

  def aktiv = {
    val now = DateTime.now();
    aktivVon.map(_.isBefore(now)).getOrElse(true) &&
      aktivBis.map(_.isAfter(now)).getOrElse(true)
  }
}

case class AbotypId(id: UUID) extends BaseId

case class Abotyp(id: AbotypId,
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  aktivVon: Option[DateTime],
  aktivBis: Option[DateTime],
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  laufzeit: Option[Int],
  laufzeiteinheit: Laufzeiteinheit,
  anzahlAbwesenheiten: Option[Int],
  farbCode: String,
  zielpreis: Option[BigDecimal],
  saldoMindestbestand: Int,
  adminProzente: BigDecimal,
  //Zusatzinformationen
  anzahlAbonnenten: Int,
  letzteLieferung: Option[DateTime],
  waehrung: Waehrung = CHF,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId) extends BaseEntity[AbotypId] with AktivRange with Product

case class AbotypSummary(id: AbotypId, name: String) extends JSONSerializable

case class AbotypModify(
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  aktivVon: Option[DateTime],
  aktivBis: Option[DateTime],
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  laufzeit: Option[Int],
  laufzeiteinheit: Laufzeiteinheit,
  anzahlAbwesenheiten: Option[Int],
  farbCode: String,
  zielpreis: Option[BigDecimal],
  saldoMindestbestand: Int,
  adminProzente: BigDecimal) extends AktivRange with JSONSerializable
