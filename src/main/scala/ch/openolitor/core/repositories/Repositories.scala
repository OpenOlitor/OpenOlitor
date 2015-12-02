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
package ch.openolitor.core.repositories

import ch.openolitor.core.models._
import java.util.UUID
import scalikejdbc._
import ch.openolitor.stammdaten.BaseEntitySQLSyntaxSupport

case class ParameterBindMapping[A](cl: Class[A], binder: ParameterBinder[A])

trait ParameterBinderMapping[A] {
  def bind(value: A): ParameterBinder[A]
}

trait BaseWriteRepository {

  def getById[E <: BaseEntity[I], I <: BaseId](syntax: BaseEntitySQLSyntaxSupport[E], id: I)(implicit session: DBSession): Option[E]

  def insertEntity(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession)
  def updateEntity(entity: BaseEntity[_ <: BaseId])(implicit session: DBSession)
  def deleteEntity(id: BaseId)(implicit session: DBSession)

  def parameters(entity: BaseEntity[_ <: BaseId])(implicit mapping: Map[Class[_], ParameterBinderMapping[_]]): Seq[Any] = {
    import DBUtils._

    val products = entity.productIterator.toSeq
    products.map(parameter(_))
  }

  def parameter(value: Any)(implicit mapping: Map[Class[_], ParameterBinderMapping[_]]): Any = {
    import DBUtils._
    value match {
      case p: BaseId => baseIdParameterBinder(p)
      //case custom if mapping.contains(custom.getClass) => mapping.get(custom.getClass).get.bind(custom).asInstanceOf[ParameterBinder[A]]
      case x => x
    }
  }
}
