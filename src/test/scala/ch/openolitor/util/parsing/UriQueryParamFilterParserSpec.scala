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

import org.specs2.mutable._
import org.joda.time.DateTime

class UriQueryParamFilterParserSpec extends Specification {
  "UriQueryParamFilterParser" should {

    "parse single attribute number expression" in {
      UriQueryParamFilterParser.parse("attribute=123.4").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("attribute"), List(ValueComparison(DecimalNumberValue(123.4), None)))))
    }

    "parse single attribute number expression ignoring spaces" in {
      UriQueryParamFilterParser.parse("attribute = 12").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("attribute"), List(ValueComparison(LongNumberValue(12), None)))))
    }

    "parse single attribute number expression with ending ';'" in {
      UriQueryParamFilterParser.parse("attribute=123.4;").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("attribute"), List(ValueComparison(DecimalNumberValue(123.4), None)))))
    }

    "parse single attribute regex expression" in {
      UriQueryParamFilterParser.parse("attribute=ab*ba").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("attribute"), List(ValueComparison(RegexValue("ab*ba"), None)))))
    }

    "parse single attribute date expression" in {
      UriQueryParamFilterParser.parse("datumVon=2016-05-30").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("datumVon"), List(ValueComparison(DateValue(
          new DateTime("2016-05-30")
        ), None)))))
    }

    "parse single attribute null expression" in {
      UriQueryParamFilterParser.parse("attribute=null").get ===
        FilterAttributeList(List(FilterAttribute(Attribute("attribute"), List(ValueComparison(NullValue(null), None)))))
    }

    "parse attributes with boolean expressions" in {
      UriQueryParamFilterParser.parse("a=null;b=null").get ===
        FilterAttributeList(List(
          FilterAttribute(Attribute("a"), List(ValueComparison(NullValue(null), None))),
          FilterAttribute(Attribute("b"), List(ValueComparison(NullValue(null), None)))
        ))
    }

    "parse multiple attributes with ending ';'" in {
      UriQueryParamFilterParser.parse("a=null;b=test;").get ===
        FilterAttributeList(List(
          FilterAttribute(Attribute("a"), List(ValueComparison(NullValue(null), None))),
          FilterAttribute(Attribute("b"), List(ValueComparison(RegexValue("test"), None)))
        ))
    }

    "parse attribute with listing" in {
      UriQueryParamFilterParser.parse("a=1,20,300").get ===
        FilterAttributeList(List(
          FilterAttribute(Attribute("a"), List(
            ValueComparison(LongNumberValue(1), None),
            ValueComparison(LongNumberValue(20), None),
            ValueComparison(LongNumberValue(300), None)
          ))
        ))
    }

    "parse attribute with range" in {
      UriQueryParamFilterParser.parse("a=1-20,300").get ===
        FilterAttributeList(List(
          FilterAttribute(Attribute("a"), List(
            ValueComparison(RangeValue(LongNumberValue(1), LongNumberValue(20)), None),
            ValueComparison(LongNumberValue(300), None)
          ))
        ))
    }

    "parse attribute with operator modifiers" in {
      UriQueryParamFilterParser.parse("a=~gt(300);b=~!(30);c=~!(1-20)").get ===
        FilterAttributeList(List(
          FilterAttribute(Attribute("a"), List(
            ValueComparison(LongNumberValue(300), Some(ValueComparator(GT)))
          )),
          FilterAttribute(Attribute("b"), List(ValueComparison(LongNumberValue(30), Some(ValueComparator(NOT))))),
          FilterAttribute(Attribute("c"), List(ValueComparison(RangeValue(LongNumberValue(1), LongNumberValue(20)), Some(ValueComparator(NOT)))))
        ))
    }
  }
}