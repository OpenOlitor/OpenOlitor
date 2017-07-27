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
package org.odftoolkit.simple

import org.odftoolkit.odfdom.pkg._
import org.odftoolkit.simple.draw._
import org.odftoolkit.odfdom.dom._
import org.odftoolkit.odfdom.dom.style._
import org.odftoolkit.odfdom.dom.element.text._
import org.odftoolkit.odfdom.dom.element.draw._
import org.odftoolkit.odfdom.dom.element.office._
import org.odftoolkit.odfdom.`type`.Color
import org.odftoolkit.simple.table._
import org.odftoolkit.simple.text.Paragraph
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle
import org.odftoolkit.odfdom.incubator.doc.style.OdfDefaultStyle
import org.odftoolkit.odfdom.dom.attribute.draw.DrawTextStyleNameAttribute

/**
 * Extends document to make method accessor public available
 */

object OdfToolkitUtils {
  implicit class MyTextbox(self: Textbox) {
    /**
     * Remove style declared on draw textbox, otherwise styles applied on template won't get applied
     */
    def removeCommonStyle() = {
      self.getDrawFrameElement().setDrawTextStyleNameAttribute(null)
    }

    def setTextContentStyleAware(content: String) = {
      val p = self.getParagraphIterator.next()
      val lastNode = p.getOdfElement.getLastChild()
      if (lastNode != null && lastNode.getNodeName() != null
        && (lastNode.getNodeName().equals("text:span"))) {
        //set text content on span element
        val span = lastNode.asInstanceOf[TextSpanElement]
        span.setTextContent(content)
      } else {
        self.setTextContent(content)
      }
    }

    def setBackgroundColorWithNewStyle(color: Color) = {
      val parent = self.getOdfElement.getParentNode.asInstanceOf[DrawFrameElement]
      val styleName = parent.getStyleName
      val styleFamily = parent.getStyleFamily

      val dom = self.getOdfElement.getOwnerDocument
      val doc = self.getOwnerDocument()
      val styles = if (dom.isInstanceOf[OdfContentDom]) {
        dom.asInstanceOf[OdfContentDom].getAutomaticStyles
      } else {
        dom.asInstanceOf[OdfStylesDom].getAutomaticStyles
      }
      val baseStyle = styles.getStyle(styleName, styleFamily)

      val graphicStyle = styles.newStyle(OdfStyleFamily.Graphic)
      val props = graphicStyle.newStyleGraphicPropertiesElement()

      val attrs = baseStyle.getAttributes
      val l = attrs.getLength - 1
      for (i <- 0 to l) {
        val item = attrs.item(i)
        props.setAttribute(item.getNodeName, item.getNodeValue)
      }

      props.setDrawStrokeAttribute("none")
      props.setDrawFillAttribute("solid")
      props.setDrawFillColorAttribute(color.toString)
      props.setStyleRunThroughAttribute("foreground")

      // set comment content
      parent.setStyleName(graphicStyle.getStyleNameAttribute)
    }

    def setFontColor(color: Color) = {
      val p = self.getParagraphIterator.next()
      p.getStyleHandler.getTextPropertiesForWrite().setFontColor(color)
      val styleName = p.getStyleName()

      val lastNode = p.getOdfElement.getLastChild();
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

  implicit class FrameExt(self: Frame) {
    def remove() = {
      Component.unregisterComponent(self.getOdfElement());
      self.getOdfElement().getParentNode().removeChild(self.getOdfElement());
    }
  }

  implicit class TableExt(self: Table) {
    def getDotTableName = {
      self.getTableName.replaceAll("#", ".")
    }
  }

  /**
   * Add possilbity to append frames
   */
  implicit class TextDocumentWithFrames(self: Paragraph) {
    def appendFrame(frame: Frame): Frame = {
      try {
        val doc = self.getOwnerDocument()
        val isForeignNode = (frame.getOdfElement().getOwnerDocument() != doc.getContentDom())
        val oldFrameEle = frame.getOdfElement()
        val newFrameEle = oldFrameEle.cloneNode(true).asInstanceOf[DrawFrameElement]

        if (isForeignNode) {
          doc.copyLinkedRefInBatch(newFrameEle, frame.getOwnerDocument())
          doc.copyForeignStyleRef(newFrameEle, frame.getOwnerDocument())
        }

        val importedNode =
          if (isForeignNode) {
            // not in a same document
            doc.cloneForeignElement(newFrameEle, doc.getContentDom(), true).asInstanceOf[DrawFrameElement]
          } else {
            newFrameEle
          }

        doc.updateNames(importedNode)
        doc.updateXMLIds(importedNode)
        val parent = self.getFrameContainerElement()
        parent.appendChild(importedNode)
        MyFrame.getInstanceOf(importedNode)
      } catch {
        case e: Exception =>
          //Logger.getLogger(classOf[TextDocument].getName()).log(Level.SEVERE, null, e)
          null
      }
    }

    /**
     * This method will search the document content,
     * return an iterator of frame objects.
     *
     * @return an iterator of frame objects
     */
    def getFrameIterator(): Iterator[Frame] = {
      new SimpleFrameIterator(self.getFrameContainerElement())
    }
  }
}
