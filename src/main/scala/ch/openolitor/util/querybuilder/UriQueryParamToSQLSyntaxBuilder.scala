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
package ch.openolitor.util.querybuilder

import scalikejdbc._
import ch.openolitor.util.parsing._
import ch.openolitor.util.StringUtil
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.DBMappings

object UriQueryParamToSQLSyntaxBuilder extends LazyLogging with DBMappings {

  def build[T](maybeExpr: Option[FilterExpr], sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], exclude: Seq[String] = Seq()): Option[SQLSyntax] = {
    maybeExpr match {
      case None => None
      case Some(expr) => build(expr, sqlSyntax, exclude)
    }
  }

  def build[T](expr: FilterExpr, sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], exclude: Seq[String]): Option[SQLSyntax] = {
    expr match {
      case FilterAttributeList(filterAttributes) => sqls.toAndConditionOpt((filterAttributes map (build(_, sqlSyntax, exclude))): _*)
      case FilterAttribute(attribute, _) if exclude.contains(attribute.value) => None
      case FilterAttribute(attribute, ValueComparison(LongNumberValue(value), None) :: Nil) => usingEq(sqlSyntax, attribute, value)
      case FilterAttribute(attribute, ValueComparison(DecimalNumberValue(value), None) :: Nil) => usingEq(sqlSyntax, attribute, value)
      case FilterAttribute(attribute, ValueComparison(BooleanValue(value), None) :: Nil) => usingEq(sqlSyntax, attribute, value)
      case FilterAttribute(attribute, ValueComparison(DateValue(value), None) :: Nil) => usingEq(sqlSyntax, attribute, value)
      case FilterAttribute(attribute, ValueComparison(NullValue(value), None) :: Nil) => retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.isNull(c))
      case FilterAttribute(attribute, ValueComparison(RangeValue(LongNumberValue(from), LongNumberValue(to)), None) :: Nil) => usingBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(DecimalNumberValue(from), DecimalNumberValue(to)), None) :: Nil) => usingBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(DateValue(from), DateValue(to)), None) :: Nil) => usingBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(RegexValue(from), RegexValue(to)), None) :: Nil) => usingBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(from, to), None) :: Nil) => None
      case FilterAttribute(attribute, ValueComparison(RegexValue(value), None) :: Nil) => retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.like(c, toSqlLike(value)))
      case FilterAttribute(attribute, ValueComparison(RegexValue(value), Some(ValueComparator(NOT))) :: Nil) => retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.notLike(c, toSqlLike(value)))
      case FilterAttribute(attribute, ValueComparison(LongNumberValue(value), Some(ValueComparator(comparator))) :: Nil) => usingComparator(sqlSyntax, attribute, value, comparator)
      case FilterAttribute(attribute, ValueComparison(DecimalNumberValue(value), Some(ValueComparator(comparator))) :: Nil) => usingComparator(sqlSyntax, attribute, value, comparator)
      case FilterAttribute(attribute, ValueComparison(BooleanValue(value), Some(ValueComparator(NOT))) :: Nil) => usingComparator(sqlSyntax, attribute, value, NOT)
      case FilterAttribute(attribute, ValueComparison(DateValue(value), Some(ValueComparator(comparator))) :: Nil) => usingComparator(sqlSyntax, attribute, value, comparator)
      case FilterAttribute(attribute, ValueComparison(NullValue(value), Some(ValueComparator(NOT))) :: Nil) => retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.isNotNull(c))
      case FilterAttribute(attribute, ValueComparison(RangeValue(LongNumberValue(from), LongNumberValue(to)), Some(ValueComparator(NOT))) :: Nil) => usingNotBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(DecimalNumberValue(from), DecimalNumberValue(to)), Some(ValueComparator(NOT))) :: Nil) => usingNotBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(DateValue(from), DateValue(to)), Some(ValueComparator(NOT))) :: Nil) => usingNotBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(RegexValue(from), RegexValue(to)), Some(ValueComparator(NOT))) :: Nil) => usingNotBetween(sqlSyntax, attribute, from, to)
      case FilterAttribute(attribute, ValueComparison(RangeValue(from, to), Some(ValueComparator(NOT))) :: Nil) => None
      case FilterAttribute(attribute, values) => sqls.toOrConditionOpt((values map (v => build(FilterAttribute(attribute, List(v)), sqlSyntax, exclude)): _*))
    }
  }

  private def usingEq[T, V: ParameterBinderFactory](sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], attribute: Attribute, value: V) =
    retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.eq(c, value))

  private def usingComparator[T, V: ParameterBinderFactory](sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], attribute: Attribute, value: V, comparator: ComparatorFunction) = {
    retrieveColumn(sqlSyntax, attribute.value) map { c =>
      comparator match {
        case GTE => sqls.ge(c, value)
        case GT => sqls.gt(c, value)
        case LTE => sqls.le(c, value)
        case LT => sqls.lt(c, value)
        case NOT => sqls.ne(c, value)
      }
    }
  }

  private def usingBetween[T, V: ParameterBinderFactory](sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], attribute: Attribute, from: V, to: V) =
    retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.between(c, from, to))

  private def usingNotBetween[T, V: ParameterBinderFactory](sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], attribute: Attribute, from: V, to: V) =
    retrieveColumn(sqlSyntax, attribute.value) map (c => sqls.between(c, from, to))

  private def retrieveColumn[T](sqlSyntax: QuerySQLSyntaxProvider[SQLSyntaxSupport[T], T], attribute: String): Option[SQLSyntax] = {
    try {
      Some(sqlSyntax.column(StringUtil.toUnderscore(attribute)))
    } catch {
      case _: Exception => None
    }
  }

  /**
   * Simplified implementation replacing * with % for sql like. Should use native mySql REGEXP
   */
  private def toSqlLike(regex: String) = {
    s"%${regex replaceAll ("\\*", "%")}%"
  }

}