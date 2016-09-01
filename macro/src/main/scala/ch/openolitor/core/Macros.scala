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

  def copyFrom[S, D](dest: S, from: D, mapping: (String, Any)*): S = macro copyFromImpl[S, D]

  def copyFromImpl[S: c.WeakTypeTag, D: c.WeakTypeTag](c: Context)(
    dest: c.Expr[S], from: c.Expr[D], mapping: c.Expr[(String, Any)]*
  ): c.Expr[S] = {
    import c.universe._

    val fromTree = from.tree
    val tree = dest.tree
    val copy = dest.actualType.member(TermName("copy"))

    val params = copy match {
      case s: MethodSymbol if (s.paramLists.nonEmpty) => s.paramLists.head
      case _ => c.abort(c.enclosingPosition, "No eligible copy method!")
    }

    val keys: Map[String, Tree] = mapping.map(_.tree).flatMap {
      case n @ q"scala.this.Predef.ArrowAssoc[$typ]($name).->[$valueTyp]($value)" =>
        val Literal(Constant(key)) = name
        Some(key.asInstanceOf[String], n)
      case m =>
        c.error(c.enclosingPosition, s"You must use ArrowAssoc values for extended mapping names. $m could not resolve at compile time.")
        None
    }.toMap

    val copyParams = params.map {
      case p if keys.contains(p.name.decodedName.toString) =>
        keys.get(p.name.decodedName.toString).map {
          case n @ q"scala.this.Predef.ArrowAssoc[$typ]($name).->[$valueTyp]($value)" =>
            //convert to accordingly tree type
            if (value.isInstanceOf[TypeApply]) {
              value.asInstanceOf[TypeApply]
            } else if (value.isInstanceOf[RefTree]) {
              value.asInstanceOf[RefTree]
            } else {
              c.abort(c.enclosingPosition, s"Unknown value type found for value $value -> ${value.getClass}!")
            }
        }.getOrElse {
          c.abort(c.enclosingPosition, s"No eligible param found $p!")
        }
      case p if from.actualType.decl(p.name).isTerm => Select(fromTree, p.name)
      case p => Select(tree, p.name)
    }

    c.Expr[S](Apply(
      Select(tree, copy),
      copyParams
    ))
  }

  def copyTo[S, D](source: S, mapping: (String, Any)*): D = macro copyToImpl[S, D]

  def copyToImpl[S: c.WeakTypeTag, D: c.WeakTypeTag](c: Context)(
    source: c.Expr[S], mapping: c.Expr[(String, Any)]*
  ): c.Expr[D] = {
    import c.universe._

    val sourceTree = source.tree

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
        val Literal(Constant(key)) = name
        Some(key.asInstanceOf[String], n)
      case m =>
        c.error(c.enclosingPosition, s"You must use ArrowAssoc values for extended mapping names. $m could not resolve at compile time.")
        None
    }.toMap

    val applyParams = params.map {
      case p if keys.contains(p.name.decodedName.toString) =>
        keys.get(p.name.decodedName.toString).map {
          case n @ q"scala.this.Predef.ArrowAssoc[$typ]($name).->[$valueTyp]($value)" =>
            //convert to accordingly tree type
            if (value.isInstanceOf[TypeApply]) {
              value.asInstanceOf[TypeApply]
            } else if (value.isInstanceOf[RefTree]) {
              value.asInstanceOf[RefTree]
            } else {
              c.abort(c.enclosingPosition, s"Unknown value type found for value $value -> ${value.getClass}!")
            }
        }.getOrElse {
          c.abort(c.enclosingPosition, s"No eligible param found $p!")
        }
      case p if source.actualType.member(p.name).isTerm => Select(sourceTree, p.name)
      case p => c.abort(c.enclosingPosition, s"No eligible param found $p in ${source.actualType}!")
    }

    c.Expr[D] { q"$companionObject(..$applyParams)" }
  }
}