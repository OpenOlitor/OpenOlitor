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

import ch.openolitor.core.scalax._
import scalikejdbc._

trait Parameters24 extends BaseParameter {
  def parameters[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24](params: Tuple24[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24])(
    implicit
    binder1: Binders[T1],
    binder2: Binders[T2],
    binder3: Binders[T3],
    binder4: Binders[T4],
    binder5: Binders[T5],
    binder6: Binders[T6],
    binder7: Binders[T7],
    binder8: Binders[T8],
    binder9: Binders[T9],
    binder10: Binders[T10],
    binder11: Binders[T11],
    binder12: Binders[T12],
    binder13: Binders[T13],
    binder14: Binders[T14],
    binder15: Binders[T15],
    binder16: Binders[T16],
    binder17: Binders[T17],
    binder18: Binders[T18],
    binder19: Binders[T19],
    binder20: Binders[T20],
    binder21: Binders[T21],
    binder22: Binders[T22],
    binder23: Binders[T23],
    binder24: Binders[T24]
  ): Seq[ParameterBinder] = {
    Seq(
      binder1(params._1),
      binder2(params._2),
      binder3(params._3),
      binder4(params._4),
      binder5(params._5),
      binder6(params._6),
      binder7(params._7),
      binder8(params._8),
      binder9(params._9),
      binder10(params._10),
      binder11(params._11),
      binder12(params._12),
      binder13(params._13),
      binder14(params._14),
      binder15(params._15),
      binder16(params._16),
      binder17(params._17),
      binder18(params._18),
      binder19(params._19),
      binder20(params._20),
      binder21(params._21),
      binder22(params._22),
      binder23(params._23),
      binder24(params._24)    
     )    
  }
}