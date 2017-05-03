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
import scala.collection.JavaConverters._
import org.joda.time.format.ISODateTimeFormat
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.format.DateTimeFormat
import java.util.Locale
import java.text.DecimalFormat
import scala.annotation.tailrec
import ch.openolitor.core.reporting.odf.NestedTextboxIterator
import org.odftoolkit.odfdom.dom.element.draw.DrawTextBoxElement

case class Value(jsValue: JsValue, value: String)

class ReportException(msg: String) extends Exception(msg)

trait DocumentProcessor extends LazyLogging {
  import OdfToolkitUtils._

  val dateFormatPattern = """date:\s*"(.*)"""".r
  val numberFormatPattern = """number:\s*"(\[([#@]?[\w\.]+)\])?([#,.0]+)(;((\[([#@]?[\w\.]+)\])?-([#,.0]+)))?"""".r
  val backgroundColorFormatPattern = """bg-color:\s*"([#@]?[\w\.]+)"""".r
  val foregroundColorFormatPattern = """fg-color:\s*"([#@]?[\w\.]+)"""".r
  val dateFormatter = ISODateTimeFormat.dateTime
  val libreOfficeDateFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")

  val parentPathPattern = """\$parent\.(.*)""".r
  val resolvePropertyPattern = """@(.*)""".r

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
    logger.debug(s"processDocument with data: ${data.prettyPrint}")
    doc.setLocale(locale)
    for {
      props <- Try(extractProperties(data))
      // process header
      _ <- Try(processVariables(doc.getHeader, props))
      _ <- Try(processTables(doc.getHeader, props, locale, Nil))
      //_ <- Try(processLists(doc.getHeader, props, locale, ""))
      headerContainer = new GenericParagraphContainerImpl(doc.getHeader.getOdfElement)
      _ <- Try(processTextboxes(headerContainer, props, locale, Nil))

      // process footer
      _ <- Try(processVariables(doc.getFooter, props))
      _ <- Try(processTables(doc.getFooter, props, locale, Nil))
      //_ <- Try(processLists(doc.getFooter, props, locale, ""))
      footerContainer = new GenericParagraphContainerImpl(doc.getFooter.getOdfElement)
      _ <- Try(processTextboxes(footerContainer, props, locale, Nil))

      // process content, order is important
      _ <- Try(processVariables(doc, props))
      _ <- Try(processTables(doc, props, locale, Nil))
      _ <- Try(processLists(doc, props, locale, Nil))
      _ <- Try(processFrames(doc, props, locale))
      _ <- Try(processSections(doc, props, locale))
      _ <- Try(processTextboxes(doc, props, locale, Nil))
      _ <- Try(registerVariables(doc, props))
    } yield true
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

  def processTables(doc: TableContainer, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]) = {
    doc.getTableList map (table => processTable(doc, table, props, locale, pathPrefixes))
  }

  /**
   * Process table:
   * duplicate all rows except header rows. Try to replace textbox values with value from property map
   */
  def processTable(doc: TableContainer, table: Table, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]) = {
    val propertyKey = parsePropertyKey(table.getDotTableName, pathPrefixes)
    props.get(propertyKey) map {
      case Value(JsArray(values), _) =>
        logger.debug(s"processTable (dynamic): ${table.getDotTableName}")
        processTableWithValues(doc, table, props, values, locale, pathPrefixes)

    } getOrElse {
      //static proccesing
      logger.debug(s"processTable (static): ${table.getDotTableName}: $pathPrefixes, $propertyKey")
      processStaticTable(table, props, locale, pathPrefixes)
    }
  }

  def processStaticTable(table: Table, props: Map[String, Value], locale: Locale = Locale.getDefault, pathPrefixes: Seq[String]) = {
    for (r <- 0 to table.getRowCount - 1) {
      //replace textfields
      for (c <- 0 to table.getColumnCount - 1) {
        val cell = table.getCellByPosition(c, r)
        processTextboxes(cell, props, locale, pathPrefixes)
      }
    }
  }

  def processLists(doc: ListContainer, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      list <- doc.getListIterator
    } processList(doc, list, props, locale, pathPrefixes)
  }

  /**
   * Process list:
   * process content of every list item as paragraph container
   */
  def processList(doc: ListContainer, list: List, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      item <- list.getItems
    } yield {
      val container = new GenericParagraphContainerImpl(item.getOdfElement())
      processTextboxes(container, props, locale, pathPrefixes)
    }
  }

  def processTableWithValues(doc: TableContainer, table: Table, props: Map[String, Value], values: Vector[JsValue], locale: Locale, pathPrefixes: Seq[String]) = {
    val startIndex = Math.max(table.getHeaderRowCount, 0)
    val rows = table.getRowList.toList
    val nonHeaderRows = rows.takeRight(rows.length - startIndex)

    logger.debug(s"processTable: ${table.getDotTableName} -> Header rows: ${table.getHeaderRowCount}")

    for (index <- 0 to values.length - 1) {
      val rowPathPrefix = findPathPrefixes(table.getDotTableName + s".$index", pathPrefixes)

      //copy rows
      val newRows = table.appendRows(nonHeaderRows.length).toList
      logger.debug(s"processTable: ${table.getDotTableName} -> Appended rows: ${newRows.length}")
      for (r <- 0 to newRows.length - 1) {
        //replace textfields
        for (cell <- 0 to table.getColumnCount - 1) {
          val origCell = nonHeaderRows.get(r).getCellByIndex(cell)
          val newCell = newRows.get(r).getCellByIndex(cell)

          // copy cell content
          copyCell(origCell, newCell)

          // replace textfields
          processTextboxes(newCell, props, locale, rowPathPrefix)
        }
      }
    }

    //remove template rows
    logger.debug(s"processTable: ${table.getDotTableName} -> Remove template rows from:$startIndex, count: ${nonHeaderRows.length}.")
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
      val sectionKey = s"${section.getName}.$index"
      logger.debug(s"processSection:$sectionKey")
      processTextboxes(section, props, locale, Seq(sectionKey))
      processTables(section, props, locale, Seq(sectionKey))
      processLists(section, props, locale, Seq(sectionKey))
      //append section
      doc.appendSection(section, false)
    }

    //remove template section
    section.remove()
  }

  /**
   * Process frames which are part of the property map and append it to the document
   */
  private def processFrames(doc: TextDocument, props: Map[String, Value], locale: Locale) = {
    for {
      p <- doc.getParagraphIterator
      f <- p.getFrameIterator
    } processFrame(p, f, props, locale)
  }

  private def processFrame(p: Paragraph, frame: Frame, props: Map[String, Value], locale: Locale) = {
    props.get(frame.getName) map {
      case Value(JsArray(values), _) =>
        processFrameWithValues(p, frame, props, values, locale)
    } getOrElse logger.debug(s"Frame not mapped to property, will be processed statically:${frame.getName}")
  }

  private def processFrameWithValues(p: Paragraph, frame: Frame, props: Map[String, Value], values: Vector[JsValue], locale: Locale) = {
    for (index <- 0 to values.length - 1) {
      val key = s"${frame.getName}.$index"
      logger.debug(s"processFrame:$key")
      // process textboxes starting below first child which has to be a <text-box>
      val firstTextBox = OdfElement.findFirstChildNode(classOf[DrawTextBoxElement], frame.getOdfElement())
      val container = new GenericParagraphContainerImpl(firstTextBox)
      processTextboxes(container, props, locale, Seq(key))
      //append section
      p.appendFrame(frame)
    }
    //remove template
    frame.remove()
  }

  /**
   * Process textboxes and fill in content based on
   *
   * If pathPrefixes is Nil, applyFormats will not be executed
   */
  private def processTextboxes(cont: ParagraphContainer, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]) = {
    for {
      p <- cont.getParagraphIterator
      t <- new NestedTextboxIterator(p.getFrameContainerElement)
    } {
      val (name, formats) = parseFormats(t.getName)
      val propertyKey = parsePropertyKey(name, pathPrefixes)
      logger.debug(s"processTextbox: ${propertyKey} | formats:$formats")

      // resolve textbox content from properties, otherwise only apply formats to current content
      t.removeCommonStyle()
      props.get(propertyKey) map {
        case Value(_, value) =>
          applyFormats(t, formats, value, props, locale, pathPrefixes)
      } getOrElse {
        if (!pathPrefixes.isEmpty) {
          applyFormats(t, formats, t.getTextContent, props, locale, pathPrefixes)
        }
      }
    }
  }

  @tailrec
  private def applyFormats(textbox: Textbox, formats: Seq[String], value: String, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]): String = {
    formats match {
      case Nil => applyFormat(textbox, "", value, props, locale, pathPrefixes)
      case format :: tail =>
        val formattedValue = applyFormat(textbox, format, value, props, locale, pathPrefixes)
        applyFormats(textbox, tail, formattedValue, props, locale, pathPrefixes)
    }
  }

  private def parsePropertyKey(name: String, pathPrefixes: Seq[String] = Nil): String = {
    findPathPrefixes(name, pathPrefixes).mkString(".")
  }

  @tailrec
  private def findPathPrefixes(name: String, pathPrefixes: Seq[String] = Nil): Seq[String] = {
    name match {
      case parentPathPattern(rest) if !pathPrefixes.isEmpty => findPathPrefixes(rest, pathPrefixes.tail)
      case _ => pathPrefixes :+ name
    }
  }

  /**
   *
   */
  private def applyFormat(textbox: Textbox, format: String, value: String, props: Map[String, Value], locale: Locale, pathPrefixes: Seq[String]): String = {
    format match {
      case dateFormatPattern(pattern) =>
        // parse date
        val formattedDate = libreOfficeDateFormat.parseDateTime(value).toString(pattern, locale)
        logger.debug(s"Formatted date with pattern $pattern => $formattedDate")
        formattedDate
      case backgroundColorFormatPattern(pattern) =>
        // set background to textbox
        resolveColor(pattern, props, pathPrefixes) map { color =>
          textbox.setBackgroundColor(color)
        }
        value
      case foregroundColorFormatPattern(pattern) =>
        // set foreground to textbox (unfortunately resets font style to default)
        resolveColor(pattern, props, pathPrefixes) map { color =>
          textbox.setFontColor(color)
        }
        value
      case numberFormatPattern(_, positiveColor, positivePattern, _, _, _, negativeColor, negativeFormat) =>
        // lookup color value        
        val number = value.toDouble
        if (number < 0 && negativeFormat != null) {
          val formattedValue = decimaleFormatForLocale(negativeFormat, locale).format(value.toDouble)
          if (negativeColor != null) {
            resolveColor(negativeColor, props, pathPrefixes) map { color =>
              logger.debug(s"Resolved native color:$color")
              textbox.setFontColor(color)
            }
          } else {
            textbox.setFontColor(Color.BLACK)
          }
          formattedValue
        } else {
          val formattedValue = decimaleFormatForLocale(positivePattern, locale).format(value.toDouble)
          if (positiveColor != null) {
            resolveColor(positiveColor, props, pathPrefixes) map { color =>
              logger.debug(s"Resolved positive color:$color")
              textbox.setFontColor(color)
            }
          } else {
            textbox.setFontColor(Color.BLACK)
          }
          formattedValue
        }
      case x if format.length > 0 =>
        logger.warn(s"Unsupported format:$format")
        textbox.setTextContentStyleAware(value)
        value
      case _ =>
        textbox.setTextContentStyleAware(value)
        value
    }
  }

  private def decimaleFormatForLocale(pattern: String, locale: Locale): DecimalFormat = {
    val decimalFormat = java.text.NumberFormat.getNumberInstance(locale).asInstanceOf[DecimalFormat]
    decimalFormat.applyPattern(pattern)
    decimalFormat
  }

  private def parseFormats(name: String): (String, Seq[String]) = {
    if (name == null || name.trim.isEmpty) {
      return (name, Nil)
    }
    name.split('|').toList match {
      case name :: Nil => (name.trim, Nil)
      case name :: tail => (name.trim, tail.map(_.trim))
      case _ => (name, Nil)
    }
  }

  private def resolveColor(color: String, props: Map[String, Value], pathPrefixes: Seq[String]): Option[Color] = {
    color match {
      case resolvePropertyPattern(property) =>
        //resolve color in props
        val propertyKey = parsePropertyKey(property, pathPrefixes)
        props.get(propertyKey) flatMap {
          case Value(_, value) =>
            resolveColor(value, props, pathPrefixes)
        }
      case color =>
        if (Color.isValid(color)) Some(Color.valueOf(color))
        else colorMap.get(color.toUpperCase).orElse {
          logger.debug(s"Unsupported color: $color")
          None
        }
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

class GenericParagraphContainerImpl(containerElement: OdfElement) extends AbstractParagraphContainer {
  def getParagraphContainerElement(): OdfElement = {
    containerElement
  }
}