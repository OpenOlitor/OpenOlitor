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
package ch.openolitor.util.parsing

import org.joda.time.DateTime

abstract class FilterExpr

trait Value extends FilterExpr

case class FilterAttributeList(value: List[FilterAttribute]) extends FilterExpr

case class FilterAttribute(attribute: Attribute, values: List[ValueComparison]) extends FilterExpr

case class ValueComparison(value: Value, operator: Option[ValueComparator]) extends FilterExpr

case class Attribute(value: String) extends FilterExpr {
  def getAttributePath: List[String] = value.split('.').toList
}

case class RangeValue(from: Value, to: Value) extends Value

case class DecimalNumberValue(value: BigDecimal) extends Value

case class LongNumberValue(value: Long) extends Value

case class RegexValue(value: String) extends Value

case class BooleanValue(value: Boolean) extends Value

case class DateValue(value: DateTime) extends Value

case class NullValue(value: Any) extends Value

case class ValueComparator(value: ComparatorFunction) extends FilterExpr

trait ComparatorFunction extends FilterExpr
case object GTE extends ComparatorFunction
case object GT extends ComparatorFunction
case object LTE extends ComparatorFunction
case object LT extends ComparatorFunction
case object NOT extends ComparatorFunction
