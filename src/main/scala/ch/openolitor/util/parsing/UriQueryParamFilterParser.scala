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

import scala.util.parsing.combinator._
import org.joda.time.DateTime

object UriQueryParamFilterParser extends RegexParsers {
  private def separator = ";"

  private def assignment = "="

  private def comparator = "~gte" | "~gt" | "~lte" | "~lt" | "~!"

  private def date = """(\d{4}-\d{2}-\d{2})""".r

  private def number = """(\d+)""".r | """(\d+\.\d)""".r

  private def regexLiteral = """([^=]*)""".r

  def parse(input: String) = parseAll(filterExpression, input)

  def filterExpression: Parser[FilterExpr] =
    repsep(filterAttribute, separator) ^^ { case l => FilterAttributeList(l) }

  private def filterAttribute: Parser[FilterAttribute] =
    attribute ~ assignment ~ valueComparison ~ rep("," ~> valueComparison) ^^ { case a ~ _ ~ head ~ rest => FilterAttribute(a, head :: rest) }

  private def valueComparison: Parser[ValueComparison] =
    valueComparator ~ "(" ~ value ~ ")" ^^ { case c ~ _ ~ v ~ _ => ValueComparison(v, Some(c)) } |
      value ~ "-" ~ value ^^ { case from ~ _ ~ to => ValueComparison(RangeValue(from, to), None) } |
      value ^^ (v => ValueComparison(v, None))

  private def valueComparator: Parser[ValueComparator] =
    comparator ^^ (c => ValueComparator(c))

  private def value: Parser[Value] =
    date ^^ (x => DateValue(DateTime.parse(x))) |
      "true" ^^^ BooleanValue(true) |
      "false" ^^^ BooleanValue(false) |
      "null" ^^^ NullValue(null) |
      number ^^ (x => NumberValue(BigDecimal(x))) |
      regexLiteral ^^ (x => RegexValue(x))

  private def attribute: Parser[Attribute] =
    """([^=]*)""".r ^^ { case value => Attribute(value) }

}