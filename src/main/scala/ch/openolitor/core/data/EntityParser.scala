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
package ch.openolitor.core.data

import java.util.Date
import scala.collection.immutable.TreeMap
import scala.collection.JavaConversions._
import scala.util._
import scala.reflect.runtime.universe.{ Try => UTry, _ }
import akka.event.LoggingAdapter
import java.io.InputStream
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.odftoolkit.simple._
import org.odftoolkit.simple.table._
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._

trait EntityParser {
  import EntityParser._

  val modifyColumns = Seq("erstelldat", "ersteller", "modifidat", "modifikator")

  def parseTreeMap[K: Ordering, V](value: String)(kf: String => K, vf: String => V): TreeMap[K, V] = {
    (TreeMap.empty[K, V] /: value.split(",")) { (tree, str) =>
      str.split("=") match {
        case Array(left, right) =>
          tree + (kf(left) -> vf(right))
        case _ =>
          tree
      }
    }
  }

  def parseMap[K, V](value: String)(kf: String => K, vf: String => V): Map[K, V] = {
    (Map.empty[K, V] /: value.split(",")) { (tree, str) =>
      str.split("=") match {
        case Array(left, right) =>
          tree + (kf(left) -> vf(right))
        case _ =>
          tree
      }
    }
  }

  def parseEntity[E <: BaseEntity[I], I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Long => Seq[Int] => Row => E)(implicit loggingAdapter: LoggingAdapter) = { name: String => table: Table =>
    var idMapping = Map[Long, I]()
    val parseResult = parseImpl(name, table, idCol, colNames)(entityFactory) {
      case (id, entity) =>
        val entityId = entity.id
        idMapping = idMapping + (id -> entityId)
        Some(entity)
    }
    (parseResult, idMapping)
  }

  def parseImpl[E <: BaseEntity[_], P, R](name: String, table: Table, idCol: String, colNames: Seq[String])(entityFactory: Long => Seq[Int] => Row => P)(resultHandler: (Long, P) => Option[R])(implicit loggingAdapter: LoggingAdapter): List[R] = {
    loggingAdapter.debug(s"Parse $name")
    val header = table.getRowByIndex(0)
    val data = table.getRowIterator().toStream drop (1)

    //match column indexes
    val indexes = columnIndexes(header, name, Seq(idCol) ++ colNames)
    val indexId = indexes.head
    val otherIndexes = indexes.tail

    (data takeWhile { row =>
      row.value[Option[Long]](indexId).isDefined
    } flatMap { row =>
      val id = row.value[Long](indexId)
      val result = entityFactory(id)(otherIndexes)(row)
      resultHandler(id, result)
    }).toList
  }

  def columnIndexes(header: Row, sheet: String, names: Seq[String], maxCols: Option[Int] = None)(implicit loggingAdapter: LoggingAdapter) = {
    loggingAdapter.debug(s"columnIndexes for:$names")
    val headerMap = headerMappings(header, names, maxCols getOrElse (names.size * 2))
    names map { name =>
      headerMap.get(name.toLowerCase.trim) getOrElse (throw ParseException(s"Missing column '$name' in sheet '$sheet'"))
    }
  }

  def headerMappings(header: Row, names: Seq[String], maxCols: Int = 30, map: Map[String, Int] = Map(), index: Int = 0)(implicit loggingAdapter: LoggingAdapter): Map[String, Int] = {
    if (map.size < maxCols && map.size < names.size) {
      val cell = header.getCellByIndex(index)
      val name = cell.getStringValue().toLowerCase.trim
      name match {
        case n if n.isEmpty =>
          loggingAdapter.debug(s"Found no cell value at:$index, result:$map")
          map //break if no column name was found anymore
        case n =>
          val newMap = names.find(_.toLowerCase.trim == name) map (x => map + (name -> index)) getOrElse (map)
          headerMappings(header, names, maxCols, newMap, index + 1)
      }
    } else {
      loggingAdapter.debug(s"Reached max:$map")
      map
    }
  }
}

object EntityParser {

  implicit class MyCell(self: Cell) {
    val allSupportedDateFormats = List(
      DateTimeFormat.forPattern("dd.MM.yy"),
      DateTimeFormat.forPattern("dd.MM.yyyy"),
      DateTimeFormat.forPattern("MM/dd/yy"),
      DateTimeFormat.forPattern("MM/dd/yyyy")
    )

    def tryParseDate(value: String, nextFormats: List[DateTimeFormatter] = allSupportedDateFormats): DateTime = {
      nextFormats match {
        case head :: tail => try {
          DateTime.parse(value, head)
        } catch {
          case e: Exception => tryParseDate(value, tail)
        }
        case Nil => throw ParseException(s"No matching date format found for value:$value")
      }
    }

    def value[T: TypeTag]: T = {
      val typ = typeOf[T]
      try {
        (typ match {
          case t if t =:= typeOf[Boolean] => self.getStringValue.toLowerCase match {
            case "true" | "richtig" | "wahr" | "1" | "x" => true
            case "false" | "falsch" | "0" => false
            case x => throw ParseException(s"Unsupported boolean format:'$x' on col:${self.getColumnIndex}, row:${self.getRowIndex}")
          }

          case t if t =:= typeOf[String] => self.getStringValue
          case t if t =:= typeOf[Option[String]] => self.getStringOptionValue
          case t if t =:= typeOf[Double] => self.getStringValue.toDouble
          case t if t =:= typeOf[BigDecimal] => BigDecimal(self.getStringValue.toDouble)
          case t if t =:= typeOf[Option[BigDecimal]] => self.getStringOptionValue map (s => BigDecimal(s.toDouble))
          case t if t =:= typeOf[Date] => self.getDateValue
          case t if t =:= typeOf[DateTime] => tryParseDate(self.getStringValue)
          case t if t =:= typeOf[Option[DateTime]] => self.getStringOptionValue map (s => tryParseDate(s))
          case t if t =:= typeOf[LocalDate] => tryParseDate(self.getStringValue).toLocalDate
          case t if t =:= typeOf[Option[LocalDate]] => self.getStringOptionValue map (s => tryParseDate(s).toLocalDate)
          case t if t =:= typeOf[Int] => self.getStringValue.toInt
          case t if t =:= typeOf[Option[Int]] => getStringOptionValue map (_.toInt)
          case t if t =:= typeOf[Long] => self.getStringValue.toLong
          case t if t =:= typeOf[Option[Long]] => getStringOptionValue map (_.toLong)
          case t if t =:= typeOf[Float] => self.getStringValue.toFloat
          case t if t =:= typeOf[Option[Float]] => self.getStringOptionValue map (_.toFloat)
          case _ =>
            throw ParseException(s"Unsupported format:$typ on col:${self.getColumnIndex}, row:${self.getRowIndex}")
        }).asInstanceOf[T]
      } catch {
        case error: Throwable => {
          val sheet = self.getTable.getTableName
          val row = self.getRowIndex
          val col = self.getColumnIndex
          val displayValue = self.getDisplayText
          val title: String = if (row > 0) {
            self.getTable.getRowByIndex(0).getCellByIndex(col).getDisplayText
          } else {
            "<notitle>"
          }

          throw new ParseException(s"Couldn't parse value in sheet:$sheet, column:$col, row:$row, title:$title => displayValue=$displayValue, error:$error")
        }
      }
    }

    def getStringOptionValue: Option[String] = {
      self.getStringValue match { case null | "" => None; case s => Some(s) }
    }
  }

  implicit class MyRow(self: Row) {
    def value[T: TypeTag](index: Int): T = {
      val value = self.getCellByIndex(index).value[T]
      value match {
        case x: String => x.trim().asInstanceOf[T]
        case _ => value
      }
    }
  }
}
