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

    def print[E <: BaseEntity[_]: TypeTag](resultList: List[E]): Unit = {
      val typ = typeOf[E]
      print("-----------------------------------------------------------------------------------------")
      print(typ.toString)
      print("-----------------------------------------------------------------------------------------")
      resultList.map(x => print(x))
    }

    def print[E <: BaseEntity[_]: TypeTag](result: E): Unit = {
      print(s"id:${result.id} => ${result}")
    }

    def receive = {
      case ImportResult(projekt, kunden, personen, abotypen, depots, tours, abos, pendenzen) =>
        print("Received import result")
        print(projekt)
        print(kunden)
        print(personen)
        print(abotypen)
        print(depots)
        print(tours)
        print(abos)
        print(pendenzen)
        print("Parsing finished")
        context.system.shutdown
      case Terminated(_) =>
        log.info("{} has terminated, shutting down system", parser.path)
        context.system.shutdown
    }
  }
}