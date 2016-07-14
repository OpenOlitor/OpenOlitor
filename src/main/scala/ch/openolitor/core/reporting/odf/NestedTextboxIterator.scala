package ch.openolitor.core.reporting.odf

import java.util.Iterator
import org.odftoolkit.simple.form._
import org.odftoolkit.odfdom.dom.element.form._
import org.odftoolkit.odfdom.pkg._
import org.apache.xerces.dom.ParentNode
import org.w3c.dom.Node
import org.odftoolkit.simple.draw.Textbox
import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement
import org.odftoolkit.odfdom.dom.element.draw.DrawTextBoxElement
import org.odftoolkit.simple.draw.TextboxContainer

/**
 * This class is an enhanced implementation for finding all textboxes in a textbox container. The SimpleTextboxIterator only looks up
 * children directly attached to provided parent. In fact when you apply some styling to a paragaph the textbox
 * might get encapsulated into a <p><span><draw-frame></span></p>. In this case, the SimpleTextboxIterator won't find the textboxes accordingly
 */
class NestedTextboxIterator(container: TextboxContainer) extends Iterator[Textbox] {

  val containerElement: OdfElement = container.getFrameContainerElement
  var nextElement: Option[(Node, Textbox)] = None;
  var tempElement: Option[(Node, Textbox)] = None;

  def hasNext(): Boolean = {
    tempElement = findNext(nextElement)
    tempElement.isDefined
  }

  def next(): Textbox = {
    tempElement.map { e =>
      nextElement = tempElement
      tempElement = None
      e._2
    } orElse {
      nextElement = findNext(nextElement)
      nextElement.map(_._2)
    } getOrElse null
  }

  def remove(): Unit = {
    throw new IllegalStateException("Unsupported operation")
  }

  private def findNext(lastResult: Option[(Node, Textbox)]): Option[(Node, Textbox)] = {
    val node = lastResult map (_._1)
    findDeepFirstChildNode(classOf[DrawFrameElement], containerElement, node) flatMap {
      case (node, nextFrame) =>
        findDeepFirstChildNode(classOf[DrawTextBoxElement], nextFrame, None) map {
          case (_, nextbox) => (node, Textbox.getInstanceof(nextbox.asInstanceOf[DrawTextBoxElement]))
        }
    }
  }

  private def findDeepFirstChildNode[T <: OdfElement](clazz: Class[T], parent: Node, refNode: Option[Node]): Option[(Node, Node)] = {
    val startingNode = Option(refNode.map { node =>
      node.getNextSibling
    }.getOrElse {
      parent.getFirstChild
    })

    startingNode.map { node =>
      findDeepChildNode(clazz, node) match {
        case r @ Some(result) => r
        case None => findDeepFirstChildNode(clazz, parent, Some(node))
      }
    }.getOrElse(None)
  }

  private def findDeepChildNode[T <: OdfElement](
    clazz: Class[T],
    refNode: Node
  ): Option[(Node, Node)] = {
    refNode match {
      case n: Node if clazz.isAssignableFrom(n.getClass) => Some((n, n))
      case n: ParentNode =>
        findDeepFirstChildNode(clazz, n, None) match {
          case Some((p, c)) => Some((n, c))
          case None => None
        }
      case _ => None
    }
  }
}