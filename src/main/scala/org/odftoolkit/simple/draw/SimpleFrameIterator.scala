package org.odftoolkit.simple.draw

import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement
import org.odftoolkit.simple.OdfToolkitUtils

/**
 *
 */
class SimpleFrameIterator(containerElement: OdfElement) extends Iterator[Frame] {

  import OdfToolkitUtils._

  private var nextElement: Frame = _
  private var tempElement: Frame = _

  def hasNext(): Boolean = {
    tempElement = findNext(nextElement)
    (tempElement != null)
  }

  def next(): Frame = {
    if (tempElement != null) {
      nextElement = tempElement
      tempElement = null
    } else {
      nextElement = findNext(nextElement)
    }
    nextElement
  }

  def remove(): Unit = {
    if (nextElement == null) {
      throw new IllegalStateException("Cannot remove empty element")
    }
    nextElement.remove()
  }

  def findNext(thisBox: Frame): Frame = {
    val nextFrame =
      if (thisBox == null) {
        OdfElement.findFirstChildNode(classOf[DrawFrameElement], containerElement)
      } else {
        OdfElement.findNextChildNode(classOf[DrawFrameElement], thisBox.getOdfElement())
      }

    if (nextFrame != null) {
      return Frame.getInstanceof(nextFrame)
    } else {
      null
    }
  }
}

object MyFrame {
  def getInstanceOf(element: DrawFrameElement): Frame = Frame.getInstanceof(element)
}