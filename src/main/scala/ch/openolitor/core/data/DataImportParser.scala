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
import java.util.Date
import akka.actor._
import java.io.File
import java.io.FileInputStream
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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

  def parseKunden(kunde2PersonenMapping: Map[Long, Seq[PersonModify]]) = {
    parse("id", Seq("id", "bezeichnung", "strasse", "hausNummer", "plz", "ort", "bemerkungen")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexKundeId, indexBezeichnung, indexStrasse, indexHausNummer, indexPlz, indexOrt, indexBemerkungen) =
            indexes

          val kundeIdLong = row.value[Long](indexKundeId)
          val personen = kunde2PersonenMapping.get(kundeIdLong).getOrElse(sys.error(s"Kunde id $kundeIdLong does not reference any person. At least one person is required"))

          (KundeId(kundeIdLong),
            KundeModify(
              bezeichnung = row.value[Option[String]](indexBezeichnung),
              strasse = row.value[String](indexStrasse),
              hausNummer = row.value[Option[String]](indexHausNummer),
              adressZusatz = None,
              plz = row.value[String](indexPlz),
              ort = row.value[String](indexOrt),
              bemerkungen = row.value[Option[String]](indexBemerkungen),
              strasseLieferung = None,
              hausNummerLieferung = None,
              adressZusatzLieferung = None,
              plzLieferung = None,
              ortLieferung = None,
              //TODO: parse personentypen as well
              typen = Set(Vereinsmitglied.kundentyp),
              ansprechpersonen = personen,
              pendenzen = List.empty))
    }
  }

  def parsePersonen = {
    parseSubEntities("kundeId", Seq("anrede", "name", "vorname", "email", "emailAlternative",
      "telefonMobil", "telefonFestnetz", "bemerkungen")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexAnrede, indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil, indexTelefonFestnetz, indexBemerkungen) =
            indexes

          PersonModify(None,
            anrede = row.value[Option[String]](indexAnrede).map(Anrede.apply),
            name = row.value[String](indexName),
            vorname = row.value[String](indexVorname),
            email = row.value[Option[String]](indexEmail),
            emailAlternative = row.value[Option[String]](indexEmailAlternative),
            telefonMobil = row.value[Option[String]](indexTelefonMobil),
            telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
            bemerkungen = row.value[Option[String]](indexBemerkungen))
    }
  }

  val parseDepots = {
    parse("id", Seq("id", "name", "kurzzeichen", "aktiv", "farbCode")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexId, indexName, indexKurzzeichen, indexAktiv, indexFarbCode) = indexes
          
          val idLong = row.value[Long](indexId)

          (DepotId(idLong),
            DepotModify(
              name = row.value[String](indexName),
              kurzzeichen = row.value[String](indexKurzzeichen),
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
              farbCode = row.value[Option[String]](indexFarbCode),
              iban = None,
              bank = None,
              beschreibung = None,
              anzahlAbonnentenMax = None))
    }
  }

  val parseAbotypen = {
    parse("id", Seq("id", "name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten", "saldo_mindestbestand", "admin_prozente")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexId, indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktivVon,
            indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten,
            indexSaldoMindestbestand, adminProzente) = indexes
            
          val idLong = row.value[Long](indexId)

          (AbotypId(idLong),
            AbotypModify(
              name = row.value[String](indexName),
              beschreibung = row.value[Option[String]](indexBeschreibung),
              lieferrhythmus = Rhythmus(row.value[String](indexlieferrhytmus)),
              aktivVon = row.value[Option[DateTime]](indexAktivVon),
              aktivBis = row.value[Option[DateTime]](indexAktivBis),
              preis = row.value[BigDecimal](indexPreis),
              preiseinheit = Preiseinheit(row.value[String](indexPreiseinheit)),
              laufzeit = row.value[Option[Int]](indexLaufzeit),
              laufzeiteinheit = Laufzeiteinheit(row.value[String](indexLaufzeiteinheit)),
              anzahlAbwesenheiten = row.value[Option[Int]](indexAnzahlAbwesenheiten),
              farbCode = row.value[String](indexFarbCode),
              zielpreis = row.value[Option[BigDecimal]](indexZielpreis),
              saldoMindestbestand = row.value[Int](indexSaldoMindestbestand),
              adminProzente = row.value[BigDecimal](adminProzente)))
    }
  }

  //TODO: parse vertriebsarten

  def parseAbos(kundeIdMapping: Map[Long, KundeId], abotypIdMapping: Map[Long, AbotypId], depotIdMapping: Map[Long, DepotId]) = {
    parse("kundeId", Seq("id", "kundeId", "kunde", "abotypId", "abotypName", "depotId", "depotName")) {
      indexes =>
        row =>
          //match column indexes
          val Seq(indexId, kundeIdIndex, kundeIndex, abotypIdIndex, abotypNameIndex, depotIdIndex, depotNameIndex) = indexes

          val idLong = row.value[Long](indexId)
          val kundeIdInt = row.value[Long](kundeIdIndex)
          val abotypIdInt = row.value[Long](abotypIdIndex)
          val depotIdOpt = row.value[Option[Long]](depotIdIndex)

          val kunde = row.value[String](kundeIndex)
          val abotypName = row.value[String](abotypNameIndex)
          val depotName = row.value[String](abotypNameIndex)

          val kundeId = kundeIdMapping.getOrElse(kundeIdInt, sys.error(s"Kunde id $kundeIdInt referenced from abo not found"))
          val abotypId = abotypIdMapping.getOrElse(abotypIdInt, sys.error(s"Abotyp id $abotypIdInt referenced from abo not found"))
          depotIdOpt.map { depotIdInt =>
            val depotId = depotIdMapping.getOrElse(depotIdInt, sys.error(s"Dept id $depotIdInt referenced from abo not found"))

            //TODO: read lieferzeitpunkt
            (AboId(idLong),
              DepotlieferungAboModify(kundeId, kunde, abotypId, abotypName, depotId, depotName, Montag).asInstanceOf[AboModify])
          }.getOrElse(sys.error(s"Unknown abotyp: no depot specified"))
    }
  }

  def parse[E, I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => (I, E)) = {
    name: String =>
      table: Table =>
        var idMapping = Map[Long, I]()
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
        var entityMap = Map[Long, Seq[E]]()
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

  def parseImpl[E, P, R](name: String, table: Table, idCol: String, colNames: Seq[String])(entityFactory: Seq[Int] => Row => P)(resultHandler: (Long, P) => Option[R]): List[R] = {
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
      val optId = row.value[Option[Long]](indexId)
      optId.map { id =>
        val result = entityFactory(otherIndexes)(row)

        resultHandler(id, result)
      }.getOrElse(None)
    }).flatten
  }

  def columnIndexes(header: Row, sheet: String, names: Seq[String], maxCols: Option[Int] = None) = {
    log.debug(s"columnIndexes for:$names")
    val headerMap = headerMappings(header, names, maxCols.getOrElse(names.size * 2))
    names.map { name =>
      headerMap.get(name.toLowerCase.trim).getOrElse(sys.error(s"Missing column '$name' in sheet '$sheet'"))
    }
  }

  def headerMappings(header: Row, names: Seq[String], maxCols: Int = 30, map: Map[String, Int] = Map(), index: Int = 0): Map[String, Int] = {
    if (map.size < maxCols && map.size < names.size) {
      val cell = header.getCellByIndex(index)
      val name = cell.getStringValue().toLowerCase.trim
      name match {
        case n if n.isEmpty =>
          log.debug(s"Found no cell value at:$index, result:$map")
          map //break if no column name was found anymore
        case n =>
          val newMap = names.find(_.toLowerCase.trim == name).map(x => map + (name -> index)).getOrElse(map)
          headerMappings(header, names, maxCols, newMap, index + 1)
      }
    } else {
      log.debug(s"Reached max:$map")
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
    val format = DateTimeFormat.forPattern("dd.MM.yyyy")

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
        case t if t =:= typeOf[Option[BigDecimal]] => self.getStringOptionValue.map(s => BigDecimal(s.toDouble))
        case t if t =:= typeOf[Date] => self.getDateValue
        case t if t =:= typeOf[DateTime] => DateTime.parse(self.getStringValue, format)
        case t if t =:= typeOf[Option[DateTime]] => self.getStringOptionValue.map(s => DateTime.parse(s, format))
        case t if t =:= typeOf[Int] => self.getStringValue.toInt        
        case t if t =:= typeOf[Option[Int]] => getStringOptionValue.map(_.toInt)
        case t if t =:= typeOf[Long] => self.getStringValue.toLong
        case t if t =:= typeOf[Option[Long]] => getStringOptionValue.map(_.toLong)
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