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
import scala.reflect.runtime.universe.{ Try => UTry, _ }
import java.util.Date
import akka.actor._
import java.io.File
import java.io.FileInputStream
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ch.openolitor.util.DateTimeUtil
import scala.collection.immutable.TreeMap
import java.io.InputStream
import scala.util._
import org.joda.time.format.DateTimeFormatter
import java.util.Locale
import ch.openolitor.core.data.parsers._

case class ParseException(msg: String) extends Exception(msg)

class DataImportParser extends Actor with ActorLogging {
  import DataImportParser._

  val receive: Receive = {
    case ParseSpreadsheet(file) =>
      val rec = sender
      try {
        importData(file) match {
          case Success(result) =>
            rec ! result
          case Failure(error) =>
            log.warning("Couldn't import data {}", error.getMessage)
            rec ! ParseError(error)
        }
      } catch {
        case t: Throwable =>
          log.warning("Couldn't import data {}", t)
          rec ! ParseError(t)
      }
  }

  def importData(file: InputStream): Try[ParseResult] = {
    implicit val loggingAdapter = log

    val doc = SpreadsheetDocument.loadDocument(file)

    //parse all sections
    for {
      (projekte, _) <- Try(doc.withSheet("Projekt")(ProjektParser.parse))
      projekt = projekte.head
      (personen, _) <- Try(doc.withSheet("Personen")(PersonParser.parse))
      (kunden, kundeIdMapping) <- Try(doc.withSheet("Kunden")(KundeParser.parse(personen)))
      (pendenzen, _) <- Try(doc.withSheet("Pendenzen")(PendenzParser.parse(kunden)))
      (tours, tourIdMapping) <- Try(doc.withSheet("Touren")(TourParser.parse))
      (abotypen, abotypIdMapping) <- Try(doc.withSheet("Abotypen")(AbotypParser.parse))
      (zusatzAbotypen, abotypIdMapping) <- Try(doc.withSheet("ZusatzAbotypen")(ZusatzAbotypParser.parse))
      (depots, depotIdMapping) <- Try(doc.withSheet("Depots")(DepotParser.parse))
      (abwesenheiten, _) <- Try(doc.withSheet("Abwesenheiten")(AbwesenheitParser.parse))
      (vertriebsarten, vertriebsartIdMapping) <- Try(doc.withSheet("Vertriebsarten")(VertriebsartParser.parse))
      (vertriebe, _) <- Try(doc.withSheet("Vertriebe")(VertriebParser.parse(vertriebsarten)))
      (abos, _) <- Try(doc.withSheet("Abos")(AboParser.parse(kundeIdMapping, kunden, vertriebsartIdMapping, vertriebsarten, vertriebe, abotypen, depotIdMapping, depots, tourIdMapping, tours, abwesenheiten)))
      (zusatzAbos, _) <- Try(doc.withSheet("ZusatzAbos")(ZusatzAboParser.parse(kundeIdMapping, kunden, vertriebsartIdMapping, vertriebsarten, vertriebe, zusatzAbotypen, abos, abwesenheiten)))
      (lieferplanungen, _) <- Try(doc.withSheet("Lieferplanungen")(LieferplanungParser.parse))
      (lieferungen, _) <- Try(doc.withSheet("Lieferungen")(LieferungParser.parse(abotypen, vertriebe, abwesenheiten, lieferplanungen, depots, tours)))
      (produzenten, _) <- Try(doc.withSheet("Produzenten")(ProduzentParser.parse))
      (produktkategorien, _) <- Try(doc.withSheet("Produktekategorien")(ProduktekategorieParser.parse))
      (produktProduzenten, _) <- Try(doc.withSheet("ProduktProduzenten")(ProduktProduzentParser.parse))
      (produktProduktekategorien, _) <- Try(doc.withSheet("ProduktProduktkategorien")(ProduktProduktekategorieParser.parse))
      (produkte, _) <- Try(doc.withSheet("Produkte")(ProduktParser.parse(produzenten, produktProduzenten, produktkategorien, produktProduktekategorien)))
      (lieferpositionen, _) <- Try(doc.withSheet("Lieferpositionen")(LieferpositionParser.parse(produkte, produzenten)))
      (sammelbestellungen, _) <- Try(doc.withSheet("Sammelbestellungen")(SammelbestellungParser.parse(produzenten, lieferplanungen)))
      (bestellungen, _) <- Try(doc.withSheet("Bestellungen")(BestellungParser.parse(sammelbestellungen)))
      (bestellpositionen, _) <- Try(doc.withSheet("Bestellpositionen")(BestellpositionParser.parse(produkte)))
      (customKundentypen, _) <- Try(doc.withSheet("Kundentypen")(CustomKundentypParser.parse))
      (tourlieferungen, _) <- Try(doc.withSheet("Tourlieferungen")(TourlieferungParser.parse(abos, kunden)))
    } yield {
      ParseResult(
        projekt,
        customKundentypen,
        kunden,
        personen,
        pendenzen,
        tours,
        depots,
        abotypen,
        zusatzAbotypen,
        vertriebsarten,
        vertriebe,
        lieferungen,
        lieferplanungen,
        lieferpositionen,
        abos,
        zusatzAbos,
        abwesenheiten,
        produkte,
        produktkategorien,
        produktProduktekategorien,
        produzenten,
        produktProduzenten,
        sammelbestellungen,
        bestellungen,
        bestellpositionen,
        tourlieferungen
      )
    }
  }
}

object DataImportParser {

  case class ParseSpreadsheet(file: InputStream)
  case class ImportEntityResult[E, I <: BaseId](id: I, entity: E)
  case class ParseError(error: Throwable)
  case class ParseResult(
    projekt: Projekt,
    kundentypen: List[CustomKundentyp],
    kunden: List[Kunde],
    personen: List[Person],
    pendenzen: List[Pendenz],
    touren: List[Tour],
    depots: List[Depot],
    abotypen: List[Abotyp],
    zusatzAbotypem: List[ZusatzAbotyp],
    vertriebsarten: List[Vertriebsart],
    vertriebe: List[Vertrieb],
    lieferungen: List[Lieferung],
    lieferplanungen: List[Lieferplanung],
    lieferpositionen: List[Lieferposition],
    abos: List[Abo],
    zusatzAbos: List[ZusatzAbo],
    abwesenheiten: List[Abwesenheit],
    produkte: List[Produkt],
    produktekategorien: List[Produktekategorie],
    produktProduktekategorien: List[ProduktProduktekategorie],
    produzenten: List[Produzent],
    produktProduzenten: List[ProduktProduzent],
    sammelbestellungen: List[Sammelbestellung],
    bestellungen: List[Bestellung],
    bestellpositionen: List[Bestellposition],
    tourlieferung: List[Tourlieferung]
  )

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
      sheet(name) map (t => f(name)(t)) getOrElse (throw ParseException(s"Missing sheet '$name'"))
    }
  }
}