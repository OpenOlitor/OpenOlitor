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
import org.odftoolkit.odfdom.pkg.OdfElement
import org.joda.time.format.DateTimeFormat
import java.util.Locale

case class Value(jsValue: JsValue, value: String)

trait DocumentProcessor extends LazyLogging {

  val dateFormatPattern = """date:\s*"(.*)"""".r
  val dateFormatter = ISODateTimeFormat.dateTime

  def processDocument(doc: TextDocument, data: JsValue, locale: Locale = Locale.getDefault): Try[Boolean] = {
    logger.debug(s"processDocument with data: $data")
    for {
      props <- Try(extractProperties(data))
      x <- Try(processVariables(doc.getHeader, props))
      x2 <- Try(processVariables(doc.getFooter, props))
      x3 <- Try(processVariables(doc, props))
      x4 <- Try(processTables(doc, doc.getTableList.toList, props, locale))
      x5 <- Try(processSections(doc, props, locale))
    } yield {
      true
    }
  }

  /**
   * Update variables in variablecontainer based on a data object. Every variables name represents a property
   * accessor which should resolve nested properties as well (with support for arrays .index notation i.e. 'adressen.0.strasse').
   */
  def processVariables(cont: VariableContainer, props: Map[String, Value]) = {
    logger.debug(s"processVariables with Properties: $props")
    props map {
      case (property, Value(_, value)) =>
        cont.getVariableFieldByName(property) match {
          case null =>
          case variable =>
            logger.debug(s"Update variable:property -> $value")
            variable.updateField(value, null)
        }
    }
  }

  def processTables(doc: TextDocument, tables: List[Table], props: Map[String, Value], locale: Locale) = {
    tables map (table => processTable(doc, table, props, locale))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  def processTable(doc: TextDocument, table: Table, props: Map[String, Value], locale: Locale) = {
    logger.debug(s"processTable: ${table.getTableName}")
    props.get(table.getTableName) collect {
      case Value(JsArray(values), _) =>
        processTableWithValues(doc, table, props, values, locale)
    }
  }

  def processTableWithValues(doc: TextDocument, table: Table, props: Map[String, Value], values: Vector[JsValue], locale: Locale) = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    logger.debug(s"processTable: ${table.getTableName} -> Header rows: ${table.getHeaderRowCount}")

    for (index <- 0 to values.length - 1) {
      val rowKey = s"${table.getTableName}.$index."

      //copy rows
      val newRows = table.appendRows(nonHeaderRows.length).toList
      logger.debug(s"processTable: ${table.getTableName} -> Appended rows: ${newRows.length}")
      for (r <- 0 to newRows.length - 1) {
        //replace textfields
        for (cell <- 0 to table.getColumnCount - 1) {
          val origCell = nonHeaderRows.get(r).getCellByIndex(cell)
          val newCell = newRows.get(r).getCellByIndex(cell)

          // copy cell content
          logger.debug(s"processTable: ${table.getTableName} -> copy Cell: row: $r, col:${cell}: ${origCell.getParagraphIterator().next().getTextContent()}")
          copyCell(origCell, newCell)
          logger.debug(s"processTable: ${table.getTableName} -> after copying: row: $r, col:${cell}: ${newCell.getParagraphIterator().next().getTextContent()}")

          // replace textfields
          processTextboxes(newCell, props, rowKey, locale)
        }
      }
    }

    //remove template rows
    logger.debug(s"processTable: ${table.getTableName} -> Remove template rows from:$startIndex, count: ${nonHeaderRows.length}")
    table.removeRowsByIndex(startIndex, nonHeaderRows.length)
  }

  private def copyCell(source: Cell, dest: Cell) = {
    //clone cell
    //clean nodes in copies cell
    val childNodes = dest.getOdfElement.getChildNodes
    for (c <- 0 to childNodes.getLength - 1) {
      dest.getOdfElement.removeChild(childNodes.item(c))
    }

    val sourceChildNodes = source.getOdfElement.getChildNodes
    for (c <- 0 to sourceChildNodes.getLength - 1) {
      dest.getOdfElement.appendChild(sourceChildNodes.item(c).cloneNode(true))
    }
  }

  /**
   * Process section which are part of the property map and append it to the document
   */
  private def processSections(doc: TextDocument, props: Map[String, Value], locale: Locale) = {
    for {
      s <- doc.getSectionIterator
    } yield processSection(doc, s, props, locale)
  }

  private def processSection(doc: TextDocument, section: Section, props: Map[String, Value], locale: Locale) = {
    props.get(section.getName) collect {
      case Value(JsArray(values), _) =>
        processSectionWithValues(doc, section, props, values, locale)

    }
  }

  private def processSectionWithValues(doc: TextDocument, section: Section, props: Map[String, Value], values: Vector[JsValue], locale: Locale) = {
    for (index <- 0 to values.length) {
      val sectionKey = s"${section.getName}.$index."
      processTextboxes(section, props, sectionKey, locale)
      //append section
      doc.appendSection(section, false)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process textboxes and fill in content based on
   */
  private def processTextboxes(cont: ParagraphContainer, props: Map[String, Value], pathPrefix: String = "", locale: Locale) = {
    logger.debug(s"processTextboxes with prefix: ${pathPrefix}: ${cont.getParagraphIterator().next().getTextboxIterator().next()}")
    for {
      p <- cont.getParagraphIterator
      t <- p.getTextboxIterator
    } {
      val (name, format) = parseFormat(t.getName)
      val propertyKey = s"$pathPrefix$name"
      logger.debug(s"processTextbox: ${propertyKey} | format:$format")
      props.get(propertyKey) map {
        case Value(_, value) =>
          val formattedValue = format map (f => formatValue(f, value, locale)) getOrElse value
          t.setTextContent(formattedValue)
      }
    }
  }

  /**
   *
   */
  private def formatValue(format: String, value: String, locale: Locale): String = {
    format match {
      case dateFormatPattern(pattern) =>
        //parse date
        dateFormatter.parseDateTime(value).toString(pattern, locale)
      case _ =>
        logger.warn(s"Unsupported format:$format")
        value
    }
  }

  private def parseFormat(name: String): (String, Option[String]) = {
    name.split('|') match {
      case Array(name, format) => (name.trim, Some(format.trim))
      case Array(name) => (name.trim, None)
      case x => (x.mkString("|"), None)
    }
  }

  /**
   * Extract all properties performing a deep lookup on the given jsvalue
   */
  def extractProperties(data: JsValue, prefix: String = ""): Map[String, Value] = {
    val childPrefix = if (prefix == "") prefix else s"$prefix."
    data match {
      case j @ JsObject(value) ⇒ value.map {
        case (k, v) =>
          extractProperties(v, s"$childPrefix$k")
      }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsArray(values) =>
        values.zipWithIndex.map {
          case (v, index) => extractProperties(v, s"$childPrefix$index")
        }.flatten.toMap + (prefix -> Value(j, ""))
      case j @ JsNull => Map(prefix -> Value(j, ""))
      case j @ JsString(value) => Map(prefix -> Value(j, value))
      case value => Map(prefix -> Value(value, value.toString))
    }
  }
}