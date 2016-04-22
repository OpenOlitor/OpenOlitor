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
package ch.openolitor.core.scalax

object Product27 {
  def unapply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24, T25, T26, T27](x: Product27[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24, T25, T26, T27]): Option[Product27[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23, T24, T25, T26, T27]] =
    Some(x)
}

trait Product27[+T1, +T2, +T3, +T4, +T5, +T6, +T7, +T8, +T9, +T10, +T11, +T12, +T13, +T14, +T15, +T16, +T17, +T18, +T19, +T20, +T21, +T22, +T23, +T24, +T25, +T26, +T27] extends Product {

  override def productArity = 27

  @throws(classOf[IndexOutOfBoundsException])
  override def productElement(n: Int) = n match {
    case 0 => _1
    case 1 => _2
    case 2 => _3
    case 3 => _4
    case 4 => _5
    case 5 => _6
    case 6 => _7
    case 7 => _8
    case 8 => _9
    case 9 => _10
    case 10 => _11
    case 11 => _12
    case 12 => _13
    case 13 => _14
    case 14 => _15
    case 15 => _16
    case 16 => _17
    case 17 => _18
    case 18 => _19
    case 19 => _20
    case 20 => _21
    case 21 => _22
    case 22 => _23
    case 23 => _24
    case 24 => _25
    case 25 => _26
    case 26 => _27
    case _ => throw new IndexOutOfBoundsException(n.toString())
  }

  def _1: T1
  def _2: T2
  def _3: T3
  def _4: T4
  def _5: T5
  def _6: T6
  def _7: T7
  def _8: T8
  def _9: T9
  def _10: T10
  def _11: T11
  def _12: T12
  def _13: T13
  def _14: T14
  def _15: T15
  def _16: T16
  def _17: T17
  def _18: T18
  def _19: T19
  def _20: T20
  def _21: T21
  def _22: T22
  def _23: T23
  def _24: T24
  def _25: T25
  def _26: T26
  def _27: T27

}