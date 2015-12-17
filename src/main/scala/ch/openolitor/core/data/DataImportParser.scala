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
  case class ImportResult(
    personen: List[ImportEntityResult[PersonModify, PersonId]],
    abotypen: List[ImportEntityResult[AbotypModify, AbotypId]],
    depots: List[ImportEntityResult[DepotModify, DepotId]])

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

    def withSheet[R](name: String)(f: String => Table => R): R = {
      sheet(name).map(t => f(name)(t)).getOrElse(sys.error(s"Missing sheet '$name'"))
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

  implicit class MyRow(self: Row) {
    def value[T: TypeTag](index: Int): T = self.getCellByIndex(index).value[T]
  }
}

class DataImportParser extends Actor with ActorLogging {
  import DataImportParser._

  val receive: Receive = {
    case ParseSpreadsheet(file) =>
      val rec = sender
      rec ! importData(file)
  }

  def importData(file: File): ImportResult = {
    val doc = SpreadsheetDocument.loadDocument(file)

    //parse all sections
    val (personen, personIdMapping) = doc.withSheet("Personen")(parsePersonen)
    val (abotypen, abotypIdMapping) = doc.withSheet("Abotyp")(parseAbotypen)
    val (depots, depotIdMapping) = doc.withSheet("Depot")(parseDepots)

    ImportResult(personen, abotypen, depots)
  }

  val parsePersonen = {
    parse("id", Seq("name", "vorname", "strasse", "hausNummer", "plz", "ort", "email", "emailAlternative",
      "telefon", "telefonAlternative", "bemerkungen")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexName, indexVorname, indexStrasse, indexHausNummer, indexPlz, indexOrt, indexEmail, indexEmailAlternative, indexTelefon, indexTelefonAlternative, indexBemerkungen) =
            indexes

          (PersonId(UUID.randomUUID),
            PersonModify(
              name = row.value(indexName),
              vorname = row.value(indexVorname),
              strasse = row.value(indexStrasse),
              hausNummer = row.value(indexHausNummer),
              adressZusatz = None,
              plz = row.value(indexPlz),
              ort = row.value(indexOrt),
              email = row.value(indexEmail),
              emailAlternative = row.value(indexEmailAlternative),
              telefon = row.value(indexTelefon),
              telefonAlternative = row.value(indexTelefonAlternative),
              bemerkungen = row.value(indexBemerkungen),
              //TODO: parse personentypen as well
              typen = Set(Vereinsmitglied)))
    }
  }

  val parseDepots = {
    parse("id", Seq("name", "aktiv")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexName, indexAktiv) = indexes

          (DepotId(UUID.randomUUID),
            DepotModify(
              name = row.value(indexName),
              apName = None,
              apVorname = None,
              apTelefon = None,
              apEmail = None,
              vName = None,
              vVorname = None,
              vTelefon = None,
              vEmail = None,
              strasse = None,
              hausNummer = None,
              plz = "",
              ort = "",
              aktiv = row.value(indexAktiv),
              oeffnungszeiten = None,
              iban = None,
              bank = None,
              beschreibung = None))
    }

  }

  val parseAbotypen = {
    parse("id", Seq("name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktiv) = indexes

          (AbotypId(UUID.randomUUID),
            AbotypModify(
              name = row.value(indexName),
              beschreibung = row.value(indexBeschreibung),
              lieferrhythmus = Rhythmus(row.value(indexlieferrhytmus)),
              enddatum = None,
              anzahlLieferungen = None,
              anzahlAbwesenheiten = None,
              preis = new BigDecimal(row.value(indexPreis)),
              preiseinheit = Preiseinheit(row.value(indexPreiseinheit)),
              aktiv = row.value(indexAktiv),
              waehrung = CHF,
              //TODO: parse vertriebsarten as well
              vertriebsarten = Set()))
    }
  }

  def parse[E, I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => (I, E)) = {
    name: String =>
      table: Table =>
        log.debug(s"Parse $name")
        //rest id mapping
        var idMapping = Map[Int, I]()

        val rows = table.getRowList().toList.take(1000)
        val header = rows.head
        val data = rows.tail

        //match column indexes
        val indexes = columnIndexes(header, name, Seq(idCol) ++ colNames)
        val indexId = indexes.head
        val otherIndexes = indexes.tail

        ((for {
          row <- data
        } yield {
          val optId = row.value[Option[Int]](indexId)
          optId.map { id =>
            val (entityId, entity) = entityFactory(otherIndexes)(row)

            idMapping = idMapping + (id -> entityId)
            Some(ImportEntityResult(entityId, entity))
          }.getOrElse(None)
        }).flatten, idMapping)
  }

  def columnIndexes(header: Row, sheet: String, names: Seq[String], maxCols: Option[Int] = None) = {
    val headerMap = headerMappings(header, maxCols.getOrElse(names.size * 2))
    names.map { name =>
      headerMap.get(name.toLowerCase.trim).getOrElse(sys.error(s"Missing column '$name' in sheet '$sheet'"))
    }
  }

  def headerMappings(header: Row, maxCols: Int = 30, map: Map[String, Int] = Map()): Map[String, Int] = {
    if (map.size < maxCols) {
      val index = map.size
      val cell = header.getCellByIndex(index)
      val name = cell.getStringValue().toLowerCase.trim
      name match {
        case n if n.isEmpty => map //break if no column name was found anymore
        case n =>
          headerMappings(header, maxCols, map + (name -> index))
      }
    } else {
      map
    }
  }
}