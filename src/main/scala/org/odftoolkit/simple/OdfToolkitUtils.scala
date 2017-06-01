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