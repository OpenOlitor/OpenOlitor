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
package ch.openolitor.buchhaltung.repositories

import ch.openolitor.core.{ AkkaEventStream, DefaultActorSystemReference }
import ch.openolitor.core.repositories.BaseUpdateRepositoryComponent

import akka.actor.ActorSystem
import ch.openolitor.core.EventStream

trait BuchhaltungUpdateRepositoryComponent extends BaseUpdateRepositoryComponent {
  val buchhaltungUpdateRepository: BuchhaltungUpdateRepository

  // implicitly expose the eventStream
  implicit def buchhaltungUpdateRepositoryImplicit = buchhaltungUpdateRepository
}

trait DefaultBuchhaltungUpdateRepositoryComponent extends BuchhaltungUpdateRepositoryComponent {
  val system: ActorSystem

  override val buchhaltungUpdateRepository: BuchhaltungUpdateRepository = new DefaultActorSystemReference(system) with BuchhaltungUpdateRepositoryImpl with AkkaEventStream
}