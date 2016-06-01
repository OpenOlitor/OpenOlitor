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

import org.odftoolkit.odfdom.`type`.Color
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.dom._
import org.odftoolkit.odfdom.dom.element.text._
import org.odftoolkit.odfdom.dom.style._
import org.odftoolkit.simple._
import org.odftoolkit.simple.common.field._
import org.odftoolkit.simple.table._
import org.odftoolkit.simple.text._
import org.odftoolkit.simple.text.list._
import org.odftoolkit.simple.draw._
import org.odftoolkit.simple.style._
import scala.util.Try
import spray.json._
import scala.collection.JavaConversions._
import org.joda.time.format.ISODateTimeFormat
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.format.DateTimeFormat
import java.util.Locale
import java.text.DecimalFormat

case class Value(jsValue: JsValue, value: String)

class ReportException(msg: String) extends Exception(msg)

trait DocumentProcessor extends LazyLogging {
  import OdfToolkitUtils._

  val dateFormatPattern = """date:\s*"(.*)"""".r
  val numberFormatPattern = """number:\s*"(\[(\w+)\])?([#,.0]+)(;((\[(\w+)\])?-([#,.0]+)))?"""".r
  val dateFormatter = ISODateTimeFormat.dateTime
  val libreOfficeDateFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")

  val colorMap: Map[String, Color] = Map(
    // english words
    "AQUA" -> Color.AQUA,
    "BLACK" -> Color.BLACK,
    "BLUE" -> Color.BLUE,
    "FUCHSIA" -> Color.FUCHSIA,
    "GRAY" -> Color.GRAY,
    "GREEN" -> Color.GREEN,
    "LIME" -> Color.LIME,
    "MAROON" -> Color.MAROON,
    "NAVY" -> Color.NAVY,
    "OLIVE" -> Color.OLIVE,
    "ORANGE" -> Color.ORANGE,
    "PURPLE" -> Color.PURPLE,
    "RED" -> Color.RED,
    "SILVER" -> Color.SILVER,
    "TEAL" -> Color.TEAL,
    "WHITE" -> Color.WHITE,
    "YELLOW" -> Color.YELLOW,

    // german words
    "SCHWARZ" -> Color.BLACK,
    "BLAU" -> Color.BLUE,
    "GRAU" -> Color.GRAY,
    "GRUEN" -> Color.GREEN,
    "VIOLETT" -> Color.PURPLE,
    "ROT" -> Color.RED,
    "SILBER" -> Color.SILVER,
    "WEISS" -> Color.WHITE,
    "GELB" -> Color.YELLOW,

    // french words
    "NOIR" -> Color.BLACK,
    "BLEU" -> Color.BLUE,
    "GRIS" -> Color.GRAY,
    "VERT" -> Color.GREEN,
    "VIOLET" -> Color.PURPLE,
    "ROUGE" -> Color.RED,
    "BLANC" -> Color.WHITE,
    "JAUNE" -> Color.YELLOW
  )

  def processDocument(doc: TextDocument, data: JsValue, locale: Locale = Locale.getDefault): Try[Boolean] = {
    logger.debug(s"processDocument with data: $data")
    doc.setLocale(locale)
    for {
      props <- Try(extractProperties(data))
      _ <- Try(processVariables(doc.getHeader, props))
      _ <- Try(processVariables(doc.getFooter, props))
      _ <- Try(processVariables(doc, props))
      _ <- Try(processTables(doc, props, locale, ""))
      _ <- Try(processLists(doc, props, locale, ""))
      _ <- Try(processSections(doc, props, locale))
      _ <- Try(processTextboxes(doc, props, locale))
      _ <- Try(registerVariables(doc, props))
    } yield {
      true
    }
  }

  /**
   * Register all values as variables to conditional field might react on them
   */
  def registerVariables(doc: TextDocument, props: Map[String, Value]) = {
    props.map {
      case (property, Value(JsObject(_), _)) =>
      //ignore object properties
      case (property, Value(JsArray(values), _)) =>
        //register length of array as variable
        val field = Fields.createSimpleVariableField(doc, property + "_length")
        field.updateField(values.length.toString, doc.getContentRoot)
      case (property, value) =>
        logger.debug(s"Register variable:$property")
        val field = Fields.createSimpleVariableField(doc, property)
        value match {
          case Value(JsNull, _) =>
            field.updateField("", doc.getContentRoot)
          case Value(JsString(str), _) =>
            field.updateField(str, doc.getContentRoot)
          case Value(JsBoolean(bool), _) =>
            field.updateField(if (bool) "1" else "0", doc.getContentRoot)
          case Value(JsNumber(number), _) =>
            field.updateField(number.toString, doc.getContentRoot)
        }
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

  def processTables(doc: TableContainer, props: Map[String, Value], locale: Locale, prefix: String) = {
    doc.getTableList map (table => processTable(doc, table, props, locale, prefix))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  def processTable(doc: TableContainer, table: Table, props: Map[String, Value], locale: Locale, prefix: String) = {
    props.get(prefix + table.getTableName) map {
      case Value(JsArray(values), _) =>
        logger.debug(s"processTable (dynamic): ${table.getTableName}")
        processTableWithValues(doc, table, props, values, locale, prefix)

    } getOrElse {
      //static proccesing
      logger.debug(s"processTable (static): ${table.getTableName}")
      processStaticTable(table, props, locale, prefix)
    }
  }

  def processStaticTable(table: Table, props: Map[String, Value], locale: Locale = Locale.getDefault, prefix: String) = {
    for (r <- 0 to table.getRowCount - 1) {
      //replace textfields
      for (c <- 0 to table.getColumnCount - 1) {
        val cell = table.getCellByPosition(c, r)
        processTextboxes(cell, props, locale, prefix)
      }
    }
  }

  def processLists(doc: ListContainer, props: Map[String, Value], locale: Locale, prefix: String) = {
    for {
      list <- doc.getListIterator
    } processList(doc, list, props, locale, prefix)
  }

  /**
   * Process list:
   * process content of every list item as paragraph container
   */
  def processList(doc: ListContainer, list: List, props: Map[String, Value], locale: Locale, prefix: String) = {
    for {
      item <- list.getItems
    } yield {
      val container = new GenericParagraphContainerImpl(item.getOdfElement())
      processTextboxes(container, props, locale, prefix)
    }
  }

  def processTableWithValues(doc: TableContainer, table: Table, props: Map[String, Value], values: Vector[JsValue], locale: Locale, prefix: String) = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    logger.debug(s"processTable: ${table.getTableName} -> Header rows: ${table.getHeaderRowCount}")

    for (index <- 0 to values.length - 1) {
      val rowKey = s"$prefix${table.getTableName}.$index."

      //copy rows
      val newRows = table.appendRows(nonHeaderRows.length).toList
      logger.debug(s"processTable: ${table.getTableName} -> Appended rows: ${newRows.length}")
      for (r <- 0 to newRows.length - 1) {
        //replace textfields
        for (cell <- 0 to table.getColumnCount - 1) {
          val origCell = nonHeaderRows.get(r).getCellByIndex(cell)
          val newCell = newRows.get(r).getCellByIndex(cell)

          // copy cell content
          copyCell(origCell, newCell)

          // replace textfields
          processTextboxes(newCell, props, locale, rowKey)
        }
      }
    }

    //remove template rows
    logger.debug(s"processTable: ${table.getTableName} -> Remove template rows from:$startIndex, count: ${nonHeaderRows.length}.")
    if (nonHeaderRows.length == table.getRowCount) {
      //remove whole table
      table.remove()
    } else {
      table.removeRowsByIndex(startIndex, nonHeaderRows.length)
    }
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
    } processSection(doc, s, props, locale)
  }

  private def processSection(doc: TextDocument, section: Section, props: Map[String, Value], locale: Locale) = {
    props.get(section.getName) map {
      case Value(JsArray(values), _) =>
        processSectionWithValues(doc, section, props, values, locale)
    } getOrElse logger.debug(s"Section not mapped to property, will be processed statically:${section.getName}")
  }

  private def processSectionWithValues(doc: TextDocument, section: Section, props: Map[String, Value], values: Vector[JsValue], locale: Locale) = {
    for (index <- 0 to values.length - 1) {
      val sectionKey = s"${section.getName}.$index."
      logger.debug(s"processSection:$sectionKey")
      processTextboxes(section, props, locale, sectionKey)
      processTables(section, props, locale, sectionKey)
      processLists(section, props, locale, sectionKey)
      //append section
      doc.appendSection(section, false)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process textboxes and fill in content based on
   */
  private def processTextboxes(cont: ParagraphContainer, props: Map[String, Value], locale: Locale, pathPrefix: String = "") = {
    for {
      p <- cont.getParagraphIterator
      t <- p.getTextboxIterator
    } {
      t.removeCommonStyle()
      val (name, format) = parseFormat(t.getName)
      val propertyKey = s"$pathPrefix$name"
      logger.debug(s"processTextbox: ${propertyKey} | format:$format")
      props.get(propertyKey) map {
        case Value(_, value) =>
          val formatValue = format getOrElse ""
          applyFormat(t, formatValue, value, locale)
      }
    }
  }

  /**
   *
   */
  private def applyFormat(textbox: Textbox, format: String, value: String, locale: Locale) = {
    format match {
      case dateFormatPattern(pattern) =>
        // parse date
        val formattedDate = libreOfficeDateFormat.parseDateTime(value).toString(pattern, locale)
        textbox.setTextContent(formattedDate)
      case numberFormatPattern(_, positiveColor, positivePattern, _, _, _, negativeColor, negativeFormat) =>
        // lookup color value        
        val number = value.toDouble
        if (number < 0 && negativeFormat != null) {
          val formattedValue = new DecimalFormat(negativeFormat).format(value.toDouble)
          textbox.setTextContent(formattedValue)
          if (negativeColor != null) {
            val color = if (Color.isValid(negativeColor)) Color.valueOf(negativeColor) else colorMap.get(negativeColor.toUpperCase).getOrElse(throw new ReportException(s"Unsupported color:$negativeColor"))
            textbox.setFontColor(color)
          }
        } else {
          val formattedValue = new DecimalFormat(positivePattern).format(value.toDouble)
          textbox.setTextContent(formattedValue)
          if (positiveColor != null) {
            val color = if (Color.isValid(positiveColor)) Color.valueOf(positiveColor) else colorMap.get(positiveColor.toUpperCase).getOrElse(throw new ReportException(s"Unsupported color:positiveColor"))
            textbox.setFontColor(color)
          }
        }
      case x if format.length > 0 =>
        logger.warn(s"Unsupported format:$format")
        textbox.setTextContent(value)
      case _ =>
        textbox.setTextContent(value)
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
      case j @ JsString(value) if Try(dateFormatter.parseDateTime(value)).isSuccess =>
        //it is a date, convert to libreoffice compatible datetime format
        val convertedDate = dateFormatter.parseDateTime(value).toString(libreOfficeDateFormat)
        Map(prefix -> Value(j, convertedDate))
      case j @ JsString(value) => Map(prefix -> Value(j, value))
      case value => Map(prefix -> Value(value, value.toString))
    }
  }
}

object OdfToolkitUtils {
  implicit class MyTextbox(self: Textbox) {
    /**
     * Remove style declared on draw textbox, otherwise styles applied on template won't get applied
     */
    def removeCommonStyle() = {
      self.getDrawFrameElement().setDrawTextStyleNameAttribute(null)
    }

    def setFontColor(color: Color) = {
      val p = self.getParagraphIterator.next()
      p.getStyleHandler.getTextPropertiesForWrite().setFontColor(color)
      val styleName = p.getStyleName()

      val lastNode = p.getOdfElement.getLastChild();
      if (lastNode != null && lastNode.getNodeName() != null
        && (lastNode.getNodeName().equals("text:a") || lastNode.getNodeName().equals("text:span"))) {
        // register style as well on span element
        lastNode.asInstanceOf[OdfElement].setAttributeNS("urn:oasis:names:tc:opendocument:xlmns:style:1.0", "style:style-name", styleName)
      } else {
        // create new style element to support coloring of font
        val content = self.getTextContent
        //remove last node (current text node)
        p.getOdfElement.removeChild(lastNode)
        val textP = p.getOdfElement.asInstanceOf[TextPElement]
        val span = textP.newTextSpanElement()

        val dom = self.getOdfElement.getOwnerDocument
        val styles = if (dom.isInstanceOf[OdfContentDom]) {
          dom.asInstanceOf[OdfContentDom].getAutomaticStyles
        } else {
          dom.asInstanceOf[OdfStylesDom].getAutomaticStyles
        }
        val textStyle = styles.newStyle(OdfStyleFamily.Text)
        val styleTextPropertiesElement = textStyle.newStyleTextPropertiesElement(null)
        styleTextPropertiesElement.setFoColorAttribute(color.toString)

        // set comment content
        span.setStyleName(textStyle.getStyleNameAttribute)
        span.setTextContent(content)
      }
    }
  }
}

class GenericParagraphContainerImpl(containerElement: OdfElement) extends AbstractParagraphContainer {
  def getParagraphContainerElement(): OdfElement = {
    containerElement
  }
}