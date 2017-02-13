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

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable
import ch.openolitor.core.JSONSerializable

case class AuslieferungId(id: Long) extends BaseId

sealed trait AuslieferungStatus

case object Erfasst extends AuslieferungStatus
case object Ausgeliefert extends AuslieferungStatus

object AuslieferungStatus {
  def apply(value: String): AuslieferungStatus = {
    Vector(Erfasst, Ausgeliefert) find (_.toString == value) getOrElse (Erfasst)
  }
}

/**
 * Die Auslieferung repräsentiert eine Sammlung von Körben zu einem Bestimmten Lieferzeitpunkt mit einem Ziel.
 */
trait Auslieferung extends BaseEntity[AuslieferungId] {
  val status: AuslieferungStatus
  val datum: DateTime
  val anzahlKoerbe: Int
}

trait AuslieferungDetail extends Auslieferung {
  val koerbe: Seq[KorbDetail]
}

trait AuslieferungModify extends JSONSerializable {
  val koerbe: Seq[KorbModify]
}

trait AuslieferungReport extends Auslieferung {
  val koerbe: Seq[KorbReport]
  val projekt: ProjektReport
}

case class MultiAuslieferungId(id: Long) extends BaseId

case class MultiAuslieferungReport(
  id: MultiAuslieferungId,
  entries: Seq[AuslieferungReportEntry],
  projekt: ProjektReport
) extends JSONSerializable

case class AuslieferungReportEntry(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  projekt: ProjektReport,
  korb: KorbReport,
  depot: Option[DepotReport],
  tour: Option[Tour]
) extends JSONSerializable

/**
 * Auslieferung pro Depot
 */
case class DepotAuslieferung(
  id: AuslieferungId,
  status: AuslieferungStatus,
  depotId: DepotId,
  depotName: String,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung

case class DepotAuslieferungDetail(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  koerbe: Seq[KorbDetail],
  depot: Depot,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungDetail with JSONSerializable

case class DepotAuslieferungReport(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  projekt: ProjektReport,
  koerbe: Seq[KorbReport],
  depot: DepotReport,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungReport with JSONSerializable

/**
 * Auslieferung pro Tour
 */
case class TourAuslieferung(
  id: AuslieferungId,
  status: AuslieferungStatus,
  tourId: TourId,
  tourName: String,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung

case class TourAuslieferungDetail(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  koerbe: Seq[KorbDetail],
  tour: Tour,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungDetail with JSONSerializable

case class TourAuslieferungModify(koerbe: Seq[KorbModify]) extends AuslieferungModify with JSONSerializable

case class TourAuslieferungReport(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  projekt: ProjektReport,
  koerbe: Seq[KorbReport],
  tour: Tour,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungReport with JSONSerializable

/**
 * Auslieferung zur Post
 */
case class PostAuslieferung(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends Auslieferung

case class PostAuslieferungDetail(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  koerbe: Seq[KorbDetail],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungDetail with JSONSerializable

case class PostAuslieferungReport(
  id: AuslieferungId,
  status: AuslieferungStatus,
  datum: DateTime,
  anzahlKoerbe: Int,
  projekt: ProjektReport,
  koerbe: Seq[KorbReport],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends AuslieferungReport with JSONSerializable

case class AuslieferungenAlsAusgeliefertMarkieren(
  ids: Seq[AuslieferungId]
) extends JSONSerializable
