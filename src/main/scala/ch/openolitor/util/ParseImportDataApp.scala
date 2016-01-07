package ch.openolitor.util

import akka.actor._
import ch.openolitor.core.data._
import ch.openolitor.core.data.DataImportParser._
import java.io.File
import scala.reflect.runtime.universe._
import ch.openolitor.core.models._

/**
 * App starting the dataimportparser itself printing results to console
 */
object ParseImportDataApp extends App {

  if (args.length < 1) {
    sys.error(s"Missing argument <file>")
  }
  val file = new File(args(0))
  println(s"Parse file ${file} -> ${file.exists}")
  if (!file.exists) {
    sys.error(s"File $file does not exist!")
  }

  val system = ActorSystem("ParseImportDataApp")
  val parser = system.actorOf(DataImportParser.props, "parser")
  val resultLogger = system.actorOf(Props(classOf[ResultLogger], parser, file), "logger")

  class ResultLogger(parser: ActorRef, file: File) extends Actor with ActorLogging {
    context watch parser

    def print(msg: String): Unit = {
      println(s"${System.currentTimeMillis}:$msg")
    }

    override def preStart(): Unit = {
      print("Sending parse command to parser")
      parser ! ParseSpreadsheet(file)
    }

    def print[E: TypeTag, I <: BaseId](resultList: List[ImportEntityResult[E, I]]): Unit = {
      val typ = typeOf[E]
      print("-----------------------------------------------------------------------------------------")
      print(typ.toString)
      print("-----------------------------------------------------------------------------------------")
      resultList.map(x => print(x))
    }

    def print[E: TypeTag, I <: BaseId](result: ImportEntityResult[E, I]): Unit = {
      print(s"id:${result.id} => ${result.entity}")
    }

    def receive = {
      case ImportResult(kunden, personen, abotypen, depots, abos) =>
        print("Received import result")
        print(kunden)
        print(personen)
        print(abotypen)
        print(depots)
        print(abos)
        print("Parsing finished")
        context.system.shutdown
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", parser.path)
        context.system.shutdown
    }
  }
}