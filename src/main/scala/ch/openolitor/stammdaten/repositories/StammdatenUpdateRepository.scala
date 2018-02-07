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
package ch.openolitor.stammdaten.repositories

import ch.openolitor.core.models._
import scalikejdbc._
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.EventStream

trait StammdatenUpdateRepository extends BaseUpdateRepository
    with StammdatenReadRepositorySync
    with EventStream {

  def updateHauptAboAddZusatzabo(add: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo]

  def updateHauptAboRemoveZusatzabo(remove: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo]

  def updateHauptAboWithZusatzabo(hauptAboId: AboId, add: ZusatzAbo, remove: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo]
}

trait StammdatenUpdateRepositoryImpl extends StammdatenReadRepositorySyncImpl with StammdatenUpdateRepository with LazyLogging {

  def updateHauptAboAddZusatzabo(add: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo] = {
    updateHauptAboWithZusatzabos(add.hauptAboId, Some(add), None)
  }

  def updateHauptAboRemoveZusatzabo(remove: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo] = {
    updateHauptAboWithZusatzabos(remove.hauptAboId, None, Some(remove))
  }

  def updateHauptAboWithZusatzabo(hauptAboId: AboId, add: ZusatzAbo, remove: ZusatzAbo)(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo] = {
    updateHauptAboWithZusatzabos(add.hauptAboId, Some(add), Some(remove))
  }

  /**
   * Adding and/or removing a ZusatzAbo to a HauptAbo.
   * If add and remove are provided we assume that the ZusatzAbo has been updated (the zusatzAbotypName).
   */
  private def updateHauptAboWithZusatzabos(hauptAboId: AboId, add: Option[ZusatzAbo], remove: Option[ZusatzAbo])(implicit session: DBSession, publisher: EventPublisher, personId: PersonId): Option[HauptAbo] = {
    modifyEntity[DepotlieferungAbo, AboId](hauptAboId) { abo =>
      val zusatzAbos = withHauptAbo(abo, add, remove)
      Map(
        depotlieferungAboMapping.column.zusatzAboIds -> zusatzAbos._1,
        depotlieferungAboMapping.column.zusatzAbotypNames -> zusatzAbos._2
      )
    } orElse modifyEntity[HeimlieferungAbo, AboId](hauptAboId) { abo =>
      val zusatzAbos = withHauptAbo(abo, add, remove)
      Map(
        heimlieferungAboMapping.column.zusatzAboIds -> zusatzAbos._1,
        heimlieferungAboMapping.column.zusatzAbotypNames -> zusatzAbos._2
      )
    } orElse modifyEntity[PostlieferungAbo, AboId](hauptAboId) { abo =>
      val zusatzAbos = withHauptAbo(abo, add, remove)
      Map(
        postlieferungAboMapping.column.zusatzAboIds -> zusatzAbos._1,
        postlieferungAboMapping.column.zusatzAbotypNames -> zusatzAbos._2
      )
    }
  }

  private def withHauptAbo(hauptAbo: HauptAbo, add: Option[ZusatzAbo], remove: Option[ZusatzAbo]): (Set[AboId], Seq[String]) = {
    val removed = remove map { r =>
      (hauptAbo.zusatzAboIds - r.id, dropFirstMatch(hauptAbo.zusatzAbotypNames, r.abotypName))
    } getOrElse {
      (hauptAbo.zusatzAboIds, hauptAbo.zusatzAbotypNames)
    }

    add map { a =>
      (removed._1 + a.id, removed._2 :+ a.abotypName)
    } getOrElse {
      removed
    }
  }

  private def dropFirstMatch[A](ls: Seq[A], value: A): Seq[A] = {
    val index = ls.indexOf(value) //index is -1 if there is no match
    if (index < 0) {
      ls
    } else if (index == 0) {
      ls.tail
    } else {
      // splitAt keeps the matching element in the second group
      val (a, b) = ls.splitAt(index)
      a ++ b.tail
    }
  }
}
