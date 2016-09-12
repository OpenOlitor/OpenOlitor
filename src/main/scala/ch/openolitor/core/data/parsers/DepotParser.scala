package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object DepotParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Depot, DepotId]("id", Seq("name", "kurzzeichen", "ap_name", "ap_vorname", "ap_telefon", "ap_email", "v_name", "v_vorname",
      "v_telefon", "v_email", "strasse", "haus_nummer",
      "plz", "ort", "aktiv", "oeffnungszeiten", "farb_code", "iban", "bank", "beschreibung", "max_abonnenten", "anzahl_abonnenten") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexKurzzeichen, indexApName, indexApVorname, indexApTelefon, indexApEmail,
        indexVName, indexVVorname, indexVTelefon, indexVEmail, indexStrasse, indexHausNummer, indexPLZ, indexOrt,
        indexAktiv, indexOeffnungszeiten, indexFarbCode, indexIBAN, indexBank, indexBeschreibung, indexMaxAbonnenten,
        indexAnzahlAbonnenten) = indexes take (22)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      Depot(
        id = DepotId(id),
        name = row.value[String](indexName),
        kurzzeichen = row.value[String](indexKurzzeichen),
        apName = row.value[Option[String]](indexApName),
        apVorname = row.value[Option[String]](indexApVorname),
        apTelefon = row.value[Option[String]](indexApTelefon),
        apEmail = row.value[Option[String]](indexApEmail),
        vName = row.value[Option[String]](indexVName),
        vVorname = row.value[Option[String]](indexVVorname),
        vTelefon = row.value[Option[String]](indexVTelefon),
        vEmail = row.value[Option[String]](indexVEmail),
        strasse = row.value[Option[String]](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        plz = row.value[String](indexPLZ),
        ort = row.value[String](indexOrt),
        aktiv = row.value[Boolean](indexAktiv),
        oeffnungszeiten = row.value[Option[String]](indexOeffnungszeiten),
        farbCode = row.value[Option[String]](indexFarbCode),
        iban = row.value[Option[String]](indexIBAN),
        bank = row.value[Option[String]](indexBank),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        anzahlAbonnentenMax = row.value[Option[Int]](indexMaxAbonnenten),
        //Zusatzinformationen
        anzahlAbonnenten = row.value[Int](indexAnzahlAbonnenten),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}