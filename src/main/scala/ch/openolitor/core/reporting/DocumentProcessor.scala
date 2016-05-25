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
package ch.openolitor.core.reporting

import org.odftoolkit.simple._
import org.odftoolkit.simple.common.field._
import org.odftoolkit.simple.table._
import org.odftoolkit.simple.text._
import scala.util.Try
import spray.json._
import scala.collection.JavaConversions._
import org.joda.time.format.ISODateTimeFormat
import com.typesafe.scalalogging.LazyLogging

case class Value(jsValue: JsValue, value: String)

trait DocumentProcessor extends LazyLogging {

  val dateFormatPattern = """'date':'(.*)'""".r
  val dateFormatter = ISODateTimeFormat.dateTime

  def processDocument(doc: TextDocument, data: JsObject): Try[Boolean] = {
    for {
      props <- Try(extractProperties(data))
      x <- Try(processVariables(doc.getHeader, props))
      x2 <- Try(processVariables(doc.getFooter, props))
      x3 <- Try(processVariables(doc, props))
      x4 <- Try(processTables(doc.getTableList.toList, props))
      x5 <- Try(processSections(doc, props))
    } yield {
      true
    }
  }

  /**
   * Update variables in variablecontainer based on a data object. Every variables name represents a property
   * accessor which should resolve nested properties as well (with support for arrays .index notation i.e. 'adressen.0.strasse').
   */
  def processVariables(cont: VariableContainer, props: Map[String, Value]) = {
    props map {
      case (property, Value(_, value)) =>
        cont.getVariableFieldByName(property) match {
          case null =>
          case variable => variable.updateField(value, null)
        }
    }
  }

  def processTables(tables: List[Table], props: Map[String, Value]) = {
    tables map (table => processTable(table, props))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  def processTable(table: Table, props: Map[String, Value]) = {
    props.get(table.getTableName) collect {
      case Value(JsArray(values), _) =>
        processTableWithValues(table, props, values)
    }
  }

  def processTableWithValues(table: Table, props: Map[String, Value], values: Vector[JsValue]) = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    for (index <- 0 to values.length) {
      val rowKey = s"${table.getTableName}.$index."

      //copy rows
      val newRows = table.appendRows(nonHeaderRows.length).toList
      for (r <- 0 to newRows.length) {
        //replace textfields
        for (cell <- 0 to table.getColumnCount) {
          val origCell = table.getCellByPosition(cell, startIndex + r)
          val newCell = newRows.get(r).getCellByIndex(cell)

          // copy cell content
          copyCell(origCell, newCell)

          // replace textfields
          processTextboxes(newCell, props, rowKey)
        }
      }
    }

    //remove template rows
    table.removeRowsByIndex(startIndex, nonHeaderRows.length)
  }

  private def copyCell(source: Cell, dest: Cell) = {
    //clone cell
    //clean nodes in copies cell
    val childNodes = dest.getOdfElement.getChildNodes
    for (c <- 0 to childNodes.getLength) {
      dest.getOdfElement.removeChild(childNodes.item(c))
    }

    val sourceChildNodes = source.getOdfElement.getChildNodes
    for (c <- 0 to sourceChildNodes.getLength) {
      dest.getOdfElement.appendChild(sourceChildNodes.item(c).cloneNode(true))
    }
  }

  /**
   * Process section which are part of the property map and append it to the document
   */
  private def processSections(doc: TextDocument, props: Map[String, Value]) = {
    for {
      s <- doc.getSectionIterator
    } yield processSection(doc, s, props)
  }

  private def processSection(doc: TextDocument, section: Section, props: Map[String, Value]) = {
    props.get(section.getName) collect {
      case Value(JsArray(values), _) =>
        processSectionWithValues(doc, section, props, values)

    }
  }

  private def processSectionWithValues(doc: TextDocument, section: Section, props: Map[String, Value], values: Vector[JsValue]) = {
    for (index <- 0 to values.length) {
      val sectionKey = s"${section.getName}.$index."
      processTextboxes(section, props, sectionKey)
      //append section
      doc.appendSection(section, false)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process textboxes and fill in content based on
   */
  private def processTextboxes(cont: ParagraphContainer, props: Map[String, Value], pathPrefix: String = "") = {
    for {
      p <- cont.getParagraphIterator
      t <- p.getTextboxIterator
    } yield {
      val (name, format) = parseFormat(t.getName)
      val propertyKey = s"$pathPrefix$name"
      props.get(propertyKey) map {
        case Value(_, value) =>
          val formattedValue = format map (f => formatValue(f, value)) getOrElse value
          t.setTextContent(formattedValue)
      }
    }
  }

  /**
   *
   */
  private def formatValue(format: String, value: String): String = {
    format match {
      case dateFormatPattern(pattern) =>
        //parse date
        dateFormatter.parseDateTime(value).formatted(pattern)
      case _ =>
        logger.warn(s"Unsupported format:$format")
        value
    }
  }

  private def parseFormat(name: String): (String, Option[String]) = {
    name.split("|").toList match {
      case name :: format :: Nil => (name.trim, Some(format.trim))
      case name :: Nil => (name.trim, None)
      case x => (x.mkString("|"), None)
    }
  }

  /**
   * Extract all properties performing a deep lookup on the given jsvalue
   */
  def extractProperties(data: JsValue, prefix: String = ""): Map[String, Value] = {
    data match {
      case j @ JsObject(value) ⇒ value.map {
        case (k, v) =>
          extractProperties(v, s"$prefix.$k")
      }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsArray(values) =>
        values.zipWithIndex.map {
          case (v, index) => extractProperties(v, s"$prefix.$index")
        }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsNull => Map(prefix -> Value(j, ""))
      case value => Map(prefix -> Value(value, value.toString))
    }
  }
}