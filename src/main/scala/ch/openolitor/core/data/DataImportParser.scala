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
import org.joda.time.DateTime

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
    val kunde2PersonenMapping = doc.withSheet("Personen")(parsePersonen)
    val (kunden, kundeIdMapping) = doc.withSheet("Kunden")(parseKunden(kunde2PersonenMapping))
    val (abotypen, abotypIdMapping) = doc.withSheet("Abotyp")(parseAbotypen)
    val (depots, depotIdMapping) = doc.withSheet("Depot")(parseDepots)
    val (abos, _) = doc.withSheet("Abos")(parseAbos(kundeIdMapping, abotypIdMapping, depotIdMapping))

    ImportResult(kunden, abotypen, depots, abos)
  }

  def parseKunden(kunde2PersonenMapping: Map[Int, Seq[PersonModify]]) = {
    parse("id", Seq("id", "bezeichnung", "strasse", "hausNummer", "plz", "ort", "bemerkungen")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexKundeId, indexBezeichnung, indexStrasse, indexHausNummer, indexPlz, indexOrt, indexBemerkungen) =
            indexes

          val kundeIdInt = row.value[Int](indexKundeId)
          val personen = kunde2PersonenMapping.get(kundeIdInt).getOrElse(sys.error(s"Kunde id $kundeIdInt does not reference any person. At least one person is required"))

          (KundeId(UUID.randomUUID),
            KundeModify(
              bezeichnung = row.value[Option[String]](indexBezeichnung),
              strasse = row.value[String](indexStrasse),
              hausNummer = row.value[Option[String]](indexHausNummer),
              adressZusatz = None,
              plz = row.value[String](indexPlz),
              ort = row.value[String](indexOrt),
              bemerkungen = row.value[Option[String]](indexBemerkungen),
              //TODO: parse personentypen as well
              typen = Set(Vereinsmitglied.kundentyp),
              ansprechpersonen = personen))
    }
  }

  def parsePersonen = {
    parseSubEntities("kundeId", Seq("name", "vorname", "email", "emailAlternative",
      "telefonMobil", "telefonFestnetz", "bemerkungen")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil, indexTelefonFestnetz, indexBemerkungen) =
            indexes

          PersonModify(None,
            name = row.value[String](indexName),
            vorname = row.value[String](indexVorname),
            email = row.value[String](indexEmail),
            emailAlternative = row.value[Option[String]](indexEmailAlternative),
            telefonMobil = row.value[Option[String]](indexTelefonMobil),
            telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
            bemerkungen = row.value[Option[String]](indexBemerkungen))
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
              name = row.value[String](indexName),
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
              aktiv = row.value[Boolean](indexAktiv),
              oeffnungszeiten = None,
              iban = None,
              bank = None,
              beschreibung = None,
              anzahlAbonnentenMax = None))
    }
  }

  val parseAbotypen = {
    parse("id", Seq("name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktivVon,
            indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten) = indexes

          (AbotypId(UUID.randomUUID),
            AbotypModify(
              name = row.value[String](indexName),
              beschreibung = row.value[Option[String]](indexBeschreibung),
              lieferrhythmus = Rhythmus(row.value[String](indexlieferrhytmus)),
              aktivVon = row.value[Option[DateTime]](indexAktivVon),
              aktivBis = row.value[Option[DateTime]](indexAktivBis),
              preis = row.value[BigDecimal](indexPreis),
              preiseinheit = Preiseinheit(row.value[String](indexPreiseinheit)),
              laufzeit = row.value[Int](indexLaufzeit),
              laufzeiteinheit = Laufzeiteinheit(row.value[String](indexLaufzeiteinheit)),
              anzahlAbwesenheiten = row.value[Option[Int]](indexAnzahlAbwesenheiten),
              farbCode = row.value[String](indexFarbCode),
              zielpreis = row.value[Option[BigDecimal]](indexZielpreis),
              //TODO: parse vertriebsarten as well
              vertriebsarten = Set()))
    }
  }

  def parseAbos(kundeIdMapping: Map[Int, KundeId], abotypIdMapping: Map[Int, AbotypId], depotIdMapping: Map[Int, DepotId]) = {
    parse("kundeId", Seq("kundeId", "kunde", "abotypId", "abotypName", "depotId", "depotName")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(kundeIdIndex, kundeIndex, abotypIdIndex, abotypNameIndex, depotIdIndex, depotNameIndex) = indexes

          val kundeIdInt = row.value[Int](kundeIdIndex)
          val abotypIdInt = row.value[Int](abotypIdIndex)
          val depotIdOpt = row.value[Option[Int]](depotIdIndex)

          val kunde = row.value[String](kundeIndex)
          val abotypName = row.value[String](abotypNameIndex)
          val depotName = row.value[String](abotypNameIndex)

          val kundeId = kundeIdMapping.getOrElse(kundeIdInt, sys.error(s"Kunde id $kundeIdInt referenced from abo not found"))
          val abotypId = abotypIdMapping.getOrElse(abotypIdInt, sys.error(s"Abotyp id $abotypIdInt referenced from abo not found"))
          depotIdOpt.map { depotIdInt =>
            val depotId = depotIdMapping.getOrElse(depotIdInt, sys.error(s"Dept id $depotIdInt referenced from abo not found"))

            //TODO: read lieferzeitpunkt
            (AboId(UUID.randomUUID),
              DepotlieferungAboModify(kundeId, kunde, abotypId, abotypName, depotId, depotName, Montag).asInstanceOf[AboModify])
          }.getOrElse(sys.error(s"Unknown abotyp: no depot specified"))
    }
  }

  def parse[E, I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => (I, E)) = {
    name: String =>
      table: Table =>
        var idMapping = Map[Int, I]()
        val parseResult = parseImpl(name, table, idCol, colNames)(entityFactory) {
          case (id, (entityId, entity)) =>
            idMapping = idMapping + (id -> entityId)
            Some(ImportEntityResult(entityId, entity))
        }
        (parseResult, idMapping)
  }

  def parseSubEntities[E](idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => (E)) = {
    name: String =>
      table: Table =>
        var entityMap = Map[Int, Seq[E]]()
        parseImpl(name, table, idCol, colNames)(entityFactory) { (id, entity) =>
          val newList = entityMap.get(id).map { values =>
            values :+ entity
          }.getOrElse {
            Seq(entity)
          }
          entityMap = entityMap + (id -> newList)
          None
        }
        entityMap
  }

  def parseImpl[E, P, R](name: String, table: Table, idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => P)(resultHandler: (Int, P) => Option[R]): List[R] = {
    log.debug(s"Parse $name")
    val rows = table.getRowList().toList.take(1000)
    val header = rows.head
    val data = rows.tail

    //match column indexes
    val indexes = columnIndexes(header, name, Seq(idCol) ++ colNames)
    val indexId = indexes.head
    val otherIndexes = indexes.tail

    (for {
      row <- data
    } yield {
      val optId = row.value[Option[Int]](indexId)
      optId.map { id =>
        val result = entityFactory(otherIndexes)(row)

        resultHandler(id, result)
      }.getOrElse(None)
    }).flatten
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

object DataImportParser {

  case class ParseSpreadsheet(file: File)
  case class ImportEntityResult[E, I <: BaseId](id: I, entity: E)
  case class ImportResult(
    kunden: List[ImportEntityResult[KundeModify, KundeId]],
    abotypen: List[ImportEntityResult[AbotypModify, AbotypId]],
    depots: List[ImportEntityResult[DepotModify, DepotId]],
    abos: List[ImportEntityResult[AboModify, AboId]])

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
      (typ match {
        case t if t =:= typeOf[Boolean] => self.getStringValue match {
          case "true" | "1" | "x" | "X" => true
          case "false" | "0" => false
          case x => sys.error(s"Unsupported boolean format:$x")
        }

        case t if t =:= typeOf[String] => self.getStringValue
        case t if t =:= typeOf[Option[String]] => self.getStringOptionValue
        case t if t =:= typeOf[Double] => self.getStringValue.toDouble
        case t if t =:= typeOf[BigDecimal] => BigDecimal(self.getStringValue.toDouble)
        case t if t =:= typeOf[Date] => self.getDateValue
        case t if t =:= typeOf[Int] => self.getStringValue.toInt
        case t if t =:= typeOf[Option[Int]] => getStringOptionValue.map(_.toInt)
        case t if t =:= typeOf[Float] => self.getStringValue.toFloat
        case t if t =:= typeOf[Option[Float]] => self.getStringOptionValue.map(_.toFloat)
        case _ =>
          sys.error(s"Unsupported format:$typ")
      }).asInstanceOf[T]
    }

    def getStringOptionValue: Option[String] = {
      self.getStringValue match { case null | "" => None; case s => Some(s) }
    }
  }

  implicit class MyRow(self: Row) {
    def value[T: TypeTag](index: Int): T = self.getCellByIndex(index).value[T]
  }
}