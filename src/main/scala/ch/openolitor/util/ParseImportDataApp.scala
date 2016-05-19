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
package ch.openolitor.util

import akka.actor._
import ch.openolitor.core.data._
import ch.openolitor.core.data.DataImportParser._
import java.io.File
import scala.reflect.runtime.universe._
import ch.openolitor.core.models._
import java.io.FileInputStream

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
      parser ! ParseSpreadsheet(new FileInputStream(file))
    }

    def print[E <: BaseEntity[_]: TypeTag](resultList: List[E]): Unit = {
      val typ = typeOf[E]
      print(s"${typ.toString}: ${resultList.length} Einträge gefunden")
    }

    def receive = {
      case r: ParseResult =>
        print("Received import result")
        print(s"Projekt: + Eintrag gefunden")
        print(r.kundentypen)
        print(r.kunden)
        print(r.personen)
        print(r.pendenzen)
        print(r.touren)
        print(r.depots)
        print(r.abotypen)
        print(r.vertriebe)
        print(r.vertriebsarten)
        print(r.lieferungen)
        print(r.lieferplanungen)
        print(r.lieferpositionen)
        print(r.abos)
        print(r.abwesenheiten)
        print(r.produkte)
        print(r.produktekategorien)
        print(r.produktProduktekategorien)
        print(r.produzenten)
        print(r.produktProduzenten)
        print(r.bestellungen)
        print(r.bestellpositionen)
        print("Parsing finished")
        context.system.terminate()
      case ParseError(error) =>
        log.error(s"Couldn't parse file {}", error)
        error.printStackTrace
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", parser.path)
        context.system.terminate()
    }
  }
}