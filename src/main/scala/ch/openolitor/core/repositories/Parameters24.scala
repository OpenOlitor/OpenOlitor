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

trait Parameters24 extends BaseParameter {
  def parameters[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24](params: Tuple24[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24])(
    implicit
    binder1: SqlBinder[T1],
    binder2: SqlBinder[T2],
    binder3: SqlBinder[T3],
    binder4: SqlBinder[T4],
    binder5: SqlBinder[T5],
    binder6: SqlBinder[T6],
    binder7: SqlBinder[T7],
    binder8: SqlBinder[T8],
    binder9: SqlBinder[T9],
    binder10: SqlBinder[T10],
    binder11: SqlBinder[T11],
    binder12: SqlBinder[T12],
    binder13: SqlBinder[T13],
    binder14: SqlBinder[T14],
    binder15: SqlBinder[T15],
    binder16: SqlBinder[T16],
    binder17: SqlBinder[T17],
    binder18: SqlBinder[T18],
    binder19: SqlBinder[T19],
    binder20: SqlBinder[T20],
    binder21: SqlBinder[T21],
    binder22: SqlBinder[T22],
    binder23: SqlBinder[T23],
    binder24: SqlBinder[T24]
  ) = {
    Tuple24(
      parameter(params._1),
      parameter(params._2),
      parameter(params._3),
      parameter(params._4),
      parameter(params._5),
      parameter(params._6),
      parameter(params._7),
      parameter(params._8),
      parameter(params._9),
      parameter(params._10),
      parameter(params._11),
      parameter(params._12),
      parameter(params._13),
      parameter(params._14),
      parameter(params._15),
      parameter(params._16),
      parameter(params._17),
      parameter(params._18),
      parameter(params._19),
      parameter(params._20),
      parameter(params._21),
      parameter(params._22),
      parameter(params._23),
      parameter(params._24)
    ).productIterator.toSeq
  }
}