package ch.openolitor.core.reporting

import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisher._
import ch.openolitor.core.reporting.ReportSystem._
import akka.actor._
import scala.annotation.tailrec

object ReportResultPublisher {
  def props(reportSystem: ActorRef): Props = Props(classOf[ReportResultPublisher], reportSystem)
}

/**
 * This actor receives results from the reportprocessor and pushed results to a stream as a publisher.
 * <a href="http://doc.akka.io/docs/akka/snapshot/scala/stream/stream-integrations.html#integrating-with-actors">Read more about integrating actors with akka streams</a>
 */
class ReportResultPublisher(reportSystem: ActorRef) extends ActorPublisher[ReportResult] {
  import akka.stream.actor.ActorPublisherMessage._
  import ReportSystem._

  var buf = Vector.empty[ReportResult]

  val receive: Receive = {
    case request: GenerateReports[_] =>
      reportSystem ! request
      context become publishingResults
  }

  def publishingResults: Receive = {
    case result: ReportResult =>
      if (buf.isEmpty && totalDemand > 0)
        onNext(result)
      else {
        buf :+= result
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}