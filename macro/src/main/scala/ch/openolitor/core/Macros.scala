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
package ch.openolitor.core

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros

object Macros {

  def copyFrom[S, D](dest: S, from: D): S = macro copyFromImpl[S, D]

  def copyFromImpl[S: c.WeakTypeTag, D : c.WeakTypeTag](c: Context)(
    dest: c.Expr[S], from: c.Expr[D]): c.Expr[S] = {
    import c.universe._

    val fromTree = reify(from.splice).tree
    val tree = reify(dest.splice).tree
    val copy = dest.actualType.member(TermName("copy"))

    val params = copy match {
      case s: MethodSymbol if (s.paramLists.nonEmpty) => s.paramLists.head
      case _ => c.abort(c.enclosingPosition, "No eligible copy method!")
    }

    def isTermSymbol(s: Symbol): Boolean = {
      s match {
        case s: TermSymbol => true
        case _ => false
      }
    }

    c.Expr[S](Apply(
      Select(tree, copy),
      params.map {
        case p if isTermSymbol(from.actualType.decl(p.name)) => Select(fromTree, p.name)
        case p => Select(tree, p.name)
      }))
  }
}