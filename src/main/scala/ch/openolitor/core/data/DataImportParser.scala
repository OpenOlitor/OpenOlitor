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
package ch.openolitor.core.data

import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import org.odftoolkit.simple._
import org.odftoolkit.simple.table._
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import java.util.UUID
import java.util.Date
import akka.actor._
import java.io.File
import java.io.FileInputStream

object DataImportParser {

  case class ParseSpreadsheet(file: File)
  case class ImportEntityResult[E, I <: BaseId](id: I, entity: E)
  case class ImportResultList[E, I <: BaseId](list: List[ImportEntityResult[E, I]])
  case class ImportResult(abotypen: ImportResultList[AbotypModify, AbotypId])

  def props(): Props = Props(classOf[DataImportParser])

  implicit class MySpreadsheet(self: SpreadsheetDocument) {
    def sheet(name: String): Option[Table] = {
      val sheet = self.getSheetByName(name)
      if (sheet != null) {
        Some(sheet)
      } else {
        None
      }
    }

    def withSheet[R](name: String)(f: Table => R): R = {
      sheet(name).map(f).getOrElse(sys.error(s"Missing sheet '$name'"))
    }
  }

  implicit class MyCell(self: Cell) {
    def value[T: TypeTag]: T = {
      val typ = typeOf[T]
      typ match {
        case t if t =:= typeOf[Boolean] => self.getBooleanValue.asInstanceOf[T]
        case t if t =:= typeOf[String] => self.getStringValue.asInstanceOf[T]
        case t if t =:= typeOf[Option[String]] => self.getStringOptionValue.asInstanceOf[T]
        case t if t =:= typeOf[Double] => self.getCurrencyValue.asInstanceOf[T]
        case t if t =:= typeOf[Date] => self.getDateValue.asInstanceOf[T]
        case t if t =:= typeOf[Int] => self.getStringValue.toInt.asInstanceOf[T]
        case t if t =:= typeOf[Option[Int]] => self.getStringOptionValue.map(_.toInt).getOrElse(None).asInstanceOf[T]
        case t if t =:= typeOf[Float] => self.getStringValue.toFloat.asInstanceOf[T]
        case t if t =:= typeOf[Option[Float]] => self.getStringOptionValue.map(_.toFloat).getOrElse(None).asInstanceOf[T]
        case _ => sys.error(s"Unsupported format:$typ")
      }
    }

    def getStringOptionValue: Option[String] = {
      self.getStringValue match { case null | "" => None; case s => Some(s) }
    }
  }
}

class DataImportParser extends Actor with ActorLogging {
  import DataImportParser._

  var abotypIdMapping: Map[Int, _ <: BaseId] = Map()

  val receive: Receive = {
    case ParseSpreadsheet(file) =>
      val rec = sender
      rec ! importData(file)
  }

  def importData(file: File): ImportResult = {
    val doc = SpreadsheetDocument.loadDocument(file)

    //parse all sections
    val abotypen = doc.withSheet("Abotyp")(parseAbotypen)

    ImportResult(abotypen)
  }

  def parseAbotypen(table: Table): ImportResultList[AbotypModify, AbotypId] = {
    //reset id mapping
    abotypIdMapping = Map()

    val rows = table.getRowList().toList
    val header = rows.head
    val data = rows.tail

    //match column indexes
    val headerMap = headerMappings(header)
    val indexId = headerMap.get("id").getOrElse(sys.error(s"Missing column 'id' in sheet 'Abotyp'"))
    val indexName = headerMap.get("name").getOrElse(sys.error(s"Missing column 'name' in sheet 'Abotyp'"))
    val indexBeschreibung = headerMap.get("beschreibung").getOrElse(sys.error(s"Missing column 'beschreibung' in sheet 'Abotyp'"))
    val indexlieferrhytmus = headerMap.get("lieferrhythmus").getOrElse(sys.error(s"Missing column 'lieferrhythmus' in sheet 'Abotyp'"))
    val indexPreis = headerMap.get("preis").getOrElse(sys.error(s"Missing column 'preis' in sheet 'Abotyp'"))
    val indexPreiseinheit = headerMap.get("preiseinheit").getOrElse(sys.error(s"Missing column 'preiseinheit' in sheet 'Abotyp'"))
    val indexAktiv = headerMap.get("aktiv").getOrElse(sys.error(s"Missing column 'aktiv' in sheet 'Abotyp'"))

    log.debug(s"Parse abotypen, expected rows:${data.length}")

    ImportResultList((for {
      row <- data
    } yield {
      val stringId = row.getCellByIndex(indexId).value[Option[Int]]
      stringId.map { id =>
        val aboTypId = AbotypId(UUID.randomUUID)
        val abotyp = AbotypModify(
          name = row.getCellByIndex(indexName).value,
          beschreibung = row.getCellByIndex(indexBeschreibung).value,
          lieferrhythmus = Rhythmus(row.getCellByIndex(indexlieferrhytmus).value),
          enddatum = None,
          anzahlLieferungen = None,
          anzahlAbwesenheiten = None,
          preis = new BigDecimal(row.getCellByIndex(indexPreis).value),
          preiseinheit = Preiseinheit(row.getCellByIndex(indexPreiseinheit).value),
          aktiv = row.getCellByIndex(indexAktiv).value,
          waehrung = CHF,
          //TODO: parse vertriebsarten as well
          vertriebsarten = Set())
        abotypIdMapping = abotypIdMapping + (id -> aboTypId)
        Some(ImportEntityResult(aboTypId, abotyp))
      }.getOrElse(None)
    }).flatten)
  }

  def headerMappings(header: Row, map: Map[String, Int] = Map()): Map[String, Int] = {
    if (map.size < header.getCellCount()) {
      val index = map.size
      val cell = header.getCellByIndex(index)
      val name = cell.getStringValue().toLowerCase.trim
      headerMappings(header, map + (name -> index))
    } else {
      map
    }
  }
}