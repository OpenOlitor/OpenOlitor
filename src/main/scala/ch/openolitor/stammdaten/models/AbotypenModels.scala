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

case class AbotypId(id: UUID) extends BaseId

case class Abotyp(id: AbotypId,
                  name: String,
                  beschreibung: Option[String],
                  lieferrhythmus: Rhythmus,
                  enddatum: Option[DateTime],
                  anzahlLieferungen: Option[Int],
                  anzahlAbwesenheiten: Option[Int],
                  preis: BigDecimal,
                  preiseinheit: Preiseinheit,
                  aktiv: Boolean,
                  //Zusatzinformationen
                  anzahlAbonnenten: Int,
                  letzteLieferung: Option[DateTime],
                  waehrung: Waehrung = CHF) extends BaseEntity[AbotypId]

case class AbotypSummary(id: AbotypId, name: String) extends Product

case class AbotypModify(
  name: String,
  beschreibung: Option[String],
  lieferrhythmus: Rhythmus,
  enddatum: Option[DateTime] = None,
  anzahlLieferungen: Option[Int] = None,
  anzahlAbwesenheiten: Option[Int] = None,
  preis: BigDecimal,
  preiseinheit: Preiseinheit,
  vertriebsarten: Set[Vertriebsartdetail],
  aktiv: Boolean,
  waehrung: Waehrung = CHF)

case class AbotypDetail(id: AbotypId,
                        name: String,
                        beschreibung: Option[String],
                        lieferrhythmus: Rhythmus,
                        enddatum: Option[DateTime],
                        anzahlLieferungen: Option[Int],
                        anzahlAbwesenheiten: Option[Int],
                        preis: BigDecimal,
                        preiseinheit: Preiseinheit,
                        aktiv: Boolean,
                        vertriebsarten: Set[Vertriebsartdetail],
                        anzahlAbonnenten: Int,
                        letzteLieferung: Option[DateTime],
                        waehrung: Waehrung = CHF) extends BaseEntity[AbotypId]

case class VertriebsartId(id: UUID = UUID.randomUUID) extends BaseId
sealed trait Vertriebsart extends BaseEntity[VertriebsartId]
case class Depotlieferung(id: VertriebsartId, abotypId: AbotypId, depotId: DepotId, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsart
case class Heimlieferung(id: VertriebsartId, abotypId: AbotypId, tourId: TourId, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsart
case class Postlieferung(id: VertriebsartId, abotypId: AbotypId, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsart

sealed trait Vertriebsartdetail extends Product
case class DepotlieferungDetail(depot: DepotSummary, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail
case class HeimlieferungDetail(tour: Tour, liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail
case class PostlieferungDetail(liefertage: Set[Lieferzeitpunkt]) extends Vertriebsartdetail