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