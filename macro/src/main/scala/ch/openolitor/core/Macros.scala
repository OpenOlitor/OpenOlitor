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

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import scalaz.IsEmpty

object Macros {

  def copyFrom[S, D](dest: S, from: D): S = macro copyFromImpl[S, D]

  def copyFromImpl[S: c.WeakTypeTag, D: c.WeakTypeTag](c: Context)(
    dest: c.Expr[S], from: c.Expr[D]): c.Expr[S] = {
    import c.universe._

    val fromTree = reify(from.splice).tree
    val tree = reify(dest.splice).tree
    val copy = dest.actualType.member(TermName("copy"))

    val params = copy match {
      case s: MethodSymbol if (s.paramLists.nonEmpty) => s.paramLists.head
      case _ => c.abort(c.enclosingPosition, "No eligible copy method!")
    }

    val copyParams = params.map {
      case p if from.actualType.decl(p.name).isTerm => Select(fromTree, p.name)
      case p => Select(tree, p.name)
    }

    c.Expr[S](Apply(
      Select(tree, copy),
      copyParams))
  }

  def copyTo[S, D](source: S, mapping: (String, Any)*): D = macro copyToImpl[S, D]

  def copyToImpl[S: c.WeakTypeTag, D: c.WeakTypeTag](c: Context)(
    source: c.Expr[S], mapping: c.Expr[(String, Any)]*): c.Expr[D] = {
    import c.universe._

    val sourceTree = reify(source.splice).tree

    val companioned = weakTypeOf[D].typeSymbol
    val companionObject = companioned.companion
    val companionType = companionObject.typeSignature
    val apply = companionType.decl(TermName("apply"))

    val params = apply match {
      case s: MethodSymbol if (s.paramLists.nonEmpty) => s.paramLists.head
      case _ => c.abort(c.enclosingPosition, "No eligible apply method!")
    }

    val keys: Map[String, Tree] = mapping.map(_.tree).flatMap {
      case n @ q"scala.this.Predef.ArrowAssoc[$typ]($name).->[$valueTyp]($value)" =>
        c.info(c.enclosingPosition, s"Found mapping for key typ:$typ:${typ.getClass}, key $name, ${name.getClass}, valueTyp $valueTyp, ${valueTyp.getClass} and value $value, ${value.getClass}", false)
        val Literal(Constant(key)) = name
        Some(key.asInstanceOf[String], n)
      case m =>
        c.error(c.enclosingPosition, s"You must use ArrowAssoc values for extended mapping names. $m could not resolve at compile time.")
        None
    }.toMap

    val applyParams = params.map {
      case p if source.actualType.decl(p.name).isTerm => Select(sourceTree, p.name)
      case p if keys.contains(p.name.decodedName.toString) =>
        keys.get(p.name.decodedName.toString).map {
          case n @ q"scala.this.Predef.ArrowAssoc[$typ]($name).->[$valueTyp]($value)" =>
            //must by instance of typeapply
            value.asInstanceOf[Ident]
        }.getOrElse {
          c.abort(c.enclosingPosition, s"No eligible param found $p!")
        }
      case p => c.abort(c.enclosingPosition, s"No eligible param found $p!")
    }
    c.info(c.enclosingPosition, s"CopyTo: $companionObject($applyParams)", false)

    c.Expr[D] { q"$companionObject(..$applyParams)" }
  }
}