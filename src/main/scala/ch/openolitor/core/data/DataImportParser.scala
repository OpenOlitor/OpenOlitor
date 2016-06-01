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

import ch.openolitor.core.models.
  _
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

  val modifiCols = Seq("erstelldat", "ersteller", "modifidat", "modifikator")

  def importData(file: InputStream): Try[ParseResult] = {
    val doc = SpreadsheetDocument.loadDocument(file)

    //parse all sections
    for {
      (projekte, _) <- Try(doc.withSheet("Projekt")(parseProjekte))
      projekt = projekte.head
      (personen, _) <- Try(doc.withSheet("Personen")(parsePersonen))
      (kunden, kundeIdMapping) <- Try(doc.withSheet("Kunden")(parseKunden(personen)))
      (pendenzen, _) <- Try(doc.withSheet("Pendenzen")(parsePendenzen(kunden)))
      (tours, tourIdMapping) <- Try(doc.withSheet("Touren")(parseTours))
      (abotypen, abotypIdMapping) <- Try(doc.withSheet("Abotypen")(parseAbotypen))
      (depots, depotIdMapping) <- Try(doc.withSheet("Depots")(parseDepots))
      (abwesenheiten, _) <- Try(doc.withSheet("Abwesenheiten")(parseAbwesenheit))
      (vertriebsarten, vertriebsartIdMapping) <- Try(doc.withSheet("Vertriebsarten")(parseVertriebsarten))
      (vertriebe, _) <- Try(doc.withSheet("Vertriebe")(parseVertriebe(vertriebsarten)))
      (abos, _) <- Try(doc.withSheet("Abos")(parseAbos(kundeIdMapping, kunden, vertriebsartIdMapping, vertriebsarten, vertriebe, abotypen, depotIdMapping, depots, tourIdMapping, tours, abwesenheiten)))
      (lieferplanungen, _) <- Try(doc.withSheet("Lieferplanungen")(parseLieferplanungen))
      (lieferungen, _) <- Try(doc.withSheet("Lieferungen")(parseLieferungen(abotypen, vertriebe, abwesenheiten, lieferplanungen, depots, tours)))
      (produzenten, _) <- Try(doc.withSheet("Produzenten")(parseProduzenten))
      (produktkategorien, _) <- Try(doc.withSheet("Produktekategorien")(parseProduktekategorien))
      (produktProduzenten, _) <- Try(doc.withSheet("ProduktProduzenten")(parseProdukteProduzenten))
      (produktProduktekategorien, _) <- Try(doc.withSheet("ProduktProduktkategorien")(parseProdukteProduktkategorien))
      (produkte, _) <- Try(doc.withSheet("Produkte")(parseProdukte(produzenten, produktProduzenten, produktkategorien, produktProduktekategorien)))
      (lieferpositionen, _) <- Try(doc.withSheet("Lieferpositionen")(parseLieferpositionen(produkte, produzenten)))
      (bestellungen, _) <- Try(doc.withSheet("Bestellungen")(parseBestellungen(produzenten, lieferplanungen)))
      (bestellpositionen, _) <- Try(doc.withSheet("Bestellpositionen")(parseBestellpositionen(produkte)))
      (customKundentypen, _) <- Try(doc.withSheet("Kundentypen")(parseCustomKundentypen))
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
        vertriebsarten,
        vertriebe,
        lieferungen,
        lieferplanungen,
        lieferpositionen,
        abos,
        abwesenheiten,
        produkte,
        produktkategorien,
        produktProduktekategorien,
        produzenten,
        produktProduzenten,
        bestellungen,
        bestellpositionen
      )
    }
  }

  def parseProjekte = {
    parse[Projekt, ProjektId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort",
      "preise_sichtbar", "preise_editierbar", "email_erforderlich", "waehrung", "geschaeftsjahr_monat", "geschaeftsjahr_tag", "two_factor_auth") ++ modifiCols) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexPreiseSichtbar,
          indexPreiseEditierbar, indexEmailErforderlich, indexWaehrung, indexGeschaeftsjahrMonat, indexGeschaeftsjahrTag, indexTwoFactorAuth) = indexes.take(13)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)
        val twoFactorAuth = parseMap(row.value[String](indexTwoFactorAuth))(r => Rolle(r).getOrElse(throw ParseException(s"Unknown Rolle $r while parsing Projekt")), _.toBoolean)

        Projekt(
          id = ProjektId(id),
          bezeichnung = row.value[String](indexBezeichnung),
          strasse = row.value[Option[String]](indexStrasse),
          hausNummer = row.value[Option[String]](indexHausNummer),
          adressZusatz = row.value[Option[String]](indexAdressZusatz),
          plz = row.value[Option[String]](indexPlz),
          ort = row.value[Option[String]](indexOrt),
          preiseSichtbar = row.value[Boolean](indexPreiseSichtbar),
          preiseEditierbar = row.value[Boolean](indexPreiseEditierbar),
          emailErforderlich = row.value[Boolean](indexEmailErforderlich),
          waehrung = Waehrung(row.value[String](indexWaehrung)),
          geschaeftsjahrMonat = row.value[Int](indexGeschaeftsjahrMonat),
          geschaeftsjahrTag = row.value[Int](indexGeschaeftsjahrTag),
          twoFactorAuthentication = twoFactorAuth,
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }

  def parseKunden(personen: List[Person]) = {
    parse[Kunde, KundeId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort", "bemerkungen",
      "abweichende_lieferadresse", "bezeichnung_lieferung", "strasse_lieferung", "haus_nummer_lieferung",
      "adress_zusatz_lieferung", "plz_lieferung", "ort_lieferung", "zusatzinfo_lieferung", "typen",
      "anzahl_abos", "anzahl_pendenzen", "anzahl_personen") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexBemerkungen,
        indexAbweichendeLieferadresse, indexBezeichnungLieferung, indexStrasseLieferung, indexHausNummerLieferung,
        indexAdresseZusatzLieferung, indexPlzLieferung, indexOrtLieferung, indexZusatzinfoLieferung, indexKundentyp,
        indexAnzahlAbos, indexAnzahlPendenzen, indexAnzahlPersonen) =
        indexes.take(19)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val kundeId = KundeId(id)
      val personenByKundeId = personen.filter(_.kundeId == kundeId)
      if (personenByKundeId.isEmpty) {
        throw ParseException(s"Kunde id $kundeId does not reference any person. At least one person is required")
      }
      Kunde(
        kundeId,
        bezeichnung = row.value[String](indexBezeichnung),
        strasse = row.value[String](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkungen),
        abweichendeLieferadresse = row.value[Boolean](indexAbweichendeLieferadresse),
        bezeichnungLieferung = row.value[Option[String]](indexBezeichnungLieferung),
        strasseLieferung = row.value[Option[String]](indexStrasseLieferung),
        hausNummerLieferung = row.value[Option[String]](indexHausNummerLieferung),
        adressZusatzLieferung = row.value[Option[String]](indexAdresseZusatzLieferung),
        plzLieferung = row.value[Option[String]](indexPlzLieferung),
        ortLieferung = row.value[Option[String]](indexOrtLieferung),
        zusatzinfoLieferung = row.value[Option[String]](indexZusatzinfoLieferung),
        typen = row.value[String](indexKundentyp).split(",").map(KundentypId).toSet,
        //Zusatzinformationen
        anzahlAbos = row.value[Int](indexAnzahlAbos),
        anzahlPendenzen = row.value[Int](indexAnzahlPendenzen),
        anzahlPersonen = row.value[Int](indexAnzahlPersonen),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parsePersonen = {
    parse[Person, PersonId]("id", Seq("kunde_id", "anrede", "name", "vorname", "email", "email_alternative",
      "telefon_mobil", "telefon_festnetz", "bemerkungen", "sort", "login_aktiv", "passwort", "letzte_anmeldung", "passwort_wechsel", "rolle") ++ modifiCols) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexAnrede, indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil,
          indexTelefonFestnetz, indexBemerkungen, indexSort, indexLoginAktiv, indexPasswort, indexLetzteAnmeldung, indexPasswortWechselErforderlich, indexRolle) = indexes.take(15)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val kundeId = KundeId(row.value[Long](indexKundeId))

        Person(
          id = PersonId(id),
          kundeId = kundeId,
          anrede = row.value[Option[String]](indexAnrede).map(Anrede.apply),
          name = row.value[String](indexName),
          vorname = row.value[String](indexVorname),
          email = row.value[Option[String]](indexEmail),
          emailAlternative = row.value[Option[String]](indexEmailAlternative),
          telefonMobil = row.value[Option[String]](indexTelefonMobil),
          telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
          bemerkungen = row.value[Option[String]](indexBemerkungen),
          sort = row.value[Int](indexSort),
          // security daten
          loginAktiv = row.value[Boolean](indexLoginAktiv),
          passwort = row.value[Option[String]](indexPasswort).map(_.toCharArray),
          letzteAnmeldung = row.value[Option[DateTime]](indexLetzteAnmeldung),
          passwortWechselErforderlich = row.value[Boolean](indexPasswortWechselErforderlich),
          rolle = row.value[Option[String]](indexRolle).map(r => Rolle(r).getOrElse(throw ParseException(s"Unbekannte Rolle $r bei Person Nr. $id"))),
          // modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }

  def parsePendenzen(kunden: List[Kunde]) = {
    parse[Pendenz, PendenzId]("id", Seq("kunde_id", "datum", "bemerkung", "status", "generiert") ++ modifiCols) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexDatum, indexBemerkung, indexStatus, indexGeneriert) = indexes.take(5)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val kundeId = KundeId(row.value[Long](indexKundeId))
        val kunde = kunden.find(_.id == kundeId).headOption.getOrElse(throw ParseException(s"Kunde not found with id $kundeId"))
        Pendenz(
          id = PendenzId(id),
          kundeId = kundeId,
          kundeBezeichnung = kunde.bezeichnung,
          datum = row.value[DateTime](indexDatum),
          bemerkung = row.value[Option[String]](indexBemerkung),
          generiert = row.value[Boolean](indexGeneriert),
          status = PendenzStatus(row.value[String](indexStatus)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }

  def parseDepots = {
    parse[Depot, DepotId]("id", Seq("name", "kurzzeichen", "ap_name", "ap_vorname", "ap_telefon", "ap_email", "v_name", "v_vorname",
      "v_telefon", "v_email", "strasse", "haus_nummer",
      "plz", "ort", "aktiv", "oeffnungszeiten", "farb_code", "iban", "bank", "beschreibung", "max_abonnenten", "anzahl_abonnenten") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexKurzzeichen, indexApName, indexApVorname, indexApTelefon, indexApEmail,
        indexVName, indexVVorname, indexVTelefon, indexVEmail, indexStrasse, indexHausNummer, indexPLZ, indexOrt,
        indexAktiv, indexOeffnungszeiten, indexFarbCode, indexIBAN, indexBank, indexBeschreibung, indexMaxAbonnenten,
        indexAnzahlAbonnenten) = indexes.take(22)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

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

  def parseTours = {
    parse[Tour, TourId]("id", Seq("name", "beschreibung") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexBeschreibung) = indexes.take(2)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      Tour(
        id = TourId(id),
        name = row.value[String](indexName),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseAbotypen = {
    parse[Abotyp, AbotypId]("id", Seq("name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten", "guthaben_mindestbestand", "admin_prozente", "wird_geplant",
      "kuendigungsfrist", "vertragslaufzeit", "anzahl_abonnenten", "letzte_lieferung", "waehrung") ++ modifiCols) { id => indexes => row =>
      import DateTimeUtil._

      //match column indexes
      val Seq(indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktivVon,
        indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten,
        indexGuthabenMindestbestand, indexAdminProzente, indexWirdGeplant, indexKuendigungsfrist, indexVertrag,
        indexAnzahlAbonnenten, indexLetzteLieferung, indexWaehrung) = indexes.take(20)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val fristeinheitPattern = """(\d+)(M|W)""".r
      //          val abosByAbotyp = abos.filter(_.abotypId == id)
      //          val lieferungenByAbotyp = lieferungen.filter(_.abotypId == id).map(_.datum)
      //          val latestLieferung = lieferungenByAbotyp.sorted.reverse.headOption

      Abotyp(
        id = AbotypId(id),
        name = row.value[String](indexName),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        lieferrhythmus = Rhythmus(row.value[String](indexlieferrhytmus)),
        aktivVon = row.value[Option[DateTime]](indexAktivVon),
        aktivBis = row.value[Option[DateTime]](indexAktivBis),
        preis = row.value[BigDecimal](indexPreis),
        preiseinheit = Preiseinheit(row.value[String](indexPreiseinheit)),
        laufzeit = row.value[Option[Int]](indexLaufzeit),
        laufzeiteinheit = Laufzeiteinheit(row.value[String](indexLaufzeiteinheit)),
        vertragslaufzeit = row.value[Option[String]](indexVertrag).map {
          case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
          case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
        },
        kuendigungsfrist = row.value[Option[String]](indexKuendigungsfrist).map {
          case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
          case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
        },
        anzahlAbwesenheiten = row.value[Option[Int]](indexAnzahlAbwesenheiten),
        farbCode = row.value[String](indexFarbCode),
        zielpreis = row.value[Option[BigDecimal]](indexZielpreis),
        guthabenMindestbestand = row.value[Int](indexGuthabenMindestbestand),
        adminProzente = row.value[BigDecimal](indexAdminProzente),
        wirdGeplant = row.value[Boolean](indexWirdGeplant),
        //Zusatzinformationen
        anzahlAbonnenten = row.value[Int](indexAnzahlAbonnenten),
        letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung),
        waehrung = Waehrung(row.value[String](indexWaehrung)),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseAbwesenheit = {
    parse[Abwesenheit, AbwesenheitId]("id", Seq("abo_id", "lieferung_id", "datum", "bemerkung") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexAboId, indexLieferungId, indexDatum, indexBemerkung) = indexes.take(4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      Abwesenheit(
        id = AbwesenheitId(id),
        aboId = AboId(row.value[Long](indexAboId)),
        lieferungId = LieferungId(row.value[Long](indexLieferungId)),
        datum = row.value[DateTime](indexDatum),
        bemerkung = row.value[Option[String]](indexBemerkung),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseVertriebsarten = {
    parse[Vertriebsart, VertriebsartId]("id", Seq("vertrieb_id", "depot_id", "tour_id", "anzahl_abos") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexVertriebId, indexDepotId, indexTourId, indexAnzahlAbos) = indexes.take(4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val vertriebsartId = VertriebsartId(id)
      val vertriebId = VertriebId(row.value[Long](indexVertriebId))
      val depotIdOpt = row.value[Option[Long]](indexDepotId).map(DepotId)
      val tourIdOpt = row.value[Option[Long]](indexTourId).map(TourId)
      val anzahlAbos = row.value[Int](indexAnzahlAbos)

      depotIdOpt.map { depotId =>
        Depotlieferung(
          vertriebsartId,
          vertriebId, depotId, anzahlAbos,
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
      }.getOrElse {
        tourIdOpt.map { tourId =>
          Heimlieferung(vertriebsartId, vertriebId, tourId, anzahlAbos,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        }.getOrElse {
          Postlieferung(vertriebsartId, vertriebId, anzahlAbos,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        }
      }
    }
  }

  def parseProdukteProduzenten = {
    parse[ProduktProduzent, ProduktProduzentId]("id", Seq("produkt_id", "produzent_id") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduktId, indexProduzentId) = indexes.take(2)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      ProduktProduzent(
        ProduktProduzentId(id),
        produktId = ProduktId(row.value[Long](indexProduktId)),
        produzentId = ProduzentId(row.value[Long](indexProduzentId)),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseProdukteProduktkategorien = {
    parse[ProduktProduktekategorie, ProduktProduktekategorieId]("id", Seq("produkt_id", "produktekategorie_id") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduktId, indexProduktekategorieId) = indexes.take(2)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      ProduktProduktekategorie(
        ProduktProduktekategorieId(id),
        produktId = ProduktId(row.value[Long](indexProduktId)),
        produktekategorieId = ProduktekategorieId(row.value[Long](indexProduktekategorieId)),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseProduktekategorien = {
    parse[Produktekategorie, ProduktekategorieId]("id", Seq("beschreibung") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexBeschreibung) = indexes.take(1)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      Produktekategorie(
        ProduktekategorieId(id),
        beschreibung = row.value[String](indexBeschreibung),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseProdukte(produzenten: List[Produzent], produktProduzenten: List[ProduktProduzent], produktkategorien: List[Produktekategorie], produktProduktekategorien: List[ProduktProduktekategorie]) = {
    parse[Produkt, ProduktId]("id", Seq("name", "verfuegbar_von", "verfuegbar_bis", "standard_menge", "einheit",
      "preis") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexVerfuegbarVon, indexVerfuegbarBis, indexStandardMenge, indexEinheit,
        indexPreis) = indexes.take(6)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val produktId = ProduktId(id)
      val produzentenIds = produktProduzenten.filter(_.produktId == produktId).map(_.produzentId)
      val produzentenName = produzenten.filter(p => produzentenIds.contains(p.id)).map(_.kurzzeichen)

      val kategorienIds = produktProduktekategorien.filter(_.produktId == produktId).map(_.produktekategorieId)
      val kategorien = produktkategorien.filter(p => kategorienIds.contains(p.id)).map(_.beschreibung)

      Produkt(
        produktId,
        name = row.value[String](indexName),
        verfuegbarVon = Liefersaison(row.value[String](indexVerfuegbarVon)),
        verfuegbarBis = Liefersaison(row.value[String](indexVerfuegbarBis)),
        kategorien,
        standardmenge = row.value[Option[BigDecimal]](indexStandardMenge),
        einheit = Liefereinheit(row.value[String](indexEinheit)),
        preis = row.value[BigDecimal](indexPreis),
        produzentenName,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseProduzenten = {
    parse[Produzent, ProduzentId]("id", Seq("name", "vorname", "kurzzeichen", "strasse", "haus_nummer", "adress_zusatz",
      "plz", "ort", "bemerkung", "email", "telefon_mobil", "telefon_festnetz", "iban", "bank", "mwst", "mwst_satz", "mwst_nr", "aktiv") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexVorname, indexKurzzeichen, indexStrasse, indexHausNummer, indexAdressZusatz,
        indexPlz, indexOrt, indexBemerkung, indexEmail, indexTelefonMobil, indexTelefonFestnetz, indexIban, indexBank, indexMwst,
        indexMwstSatz, indexMwstNr, indexAktiv) = indexes.take(18)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      Produzent(
        id = ProduzentId(id),
        name = row.value[String](indexName),
        vorname = row.value[Option[String]](indexVorname),
        kurzzeichen = row.value[String](indexKurzzeichen),
        strasse = row.value[Option[String]](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkung),
        email = row.value[String](indexEmail),
        telefonMobil = row.value[Option[String]](indexTelefonMobil),
        telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
        iban = row.value[Option[String]](indexIban),
        bank = row.value[Option[String]](indexBank),
        mwst = row.value[Boolean](indexMwst),
        mwstSatz = row.value[Option[BigDecimal]](indexMwstSatz),
        mwstNr = row.value[Option[String]](indexMwstNr),
        aktiv = row.value[Boolean](indexAktiv),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseLieferplanungen = {
    parse[Lieferplanung, LieferplanungId]("id", Seq("bemerkung", "abotyp_depot_tour", "status") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexBemerkung, indexAbotypDepotTour, indexStatus) = indexes.take(4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      Lieferplanung(
        id = LieferplanungId(id),
        bemerkungen = row.value[Option[String]](indexBemerkung),
        abotypDepotTour = row.value[String](indexAbotypDepotTour),
        status = LieferungStatus(row.value[String](indexStatus)),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseLieferpositionen(produkte: List[Produkt], produzenten: List[Produzent]) = {
    parse[Lieferposition, LieferpositionId]("id", Seq("lieferung_id", "produkt_id", "produzent_id", "preis_einheit", "liefereinheit", "menge", "preis", "anzahl") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexLieferungId, indexProduktId, indexProduzentId, indexPreisEinheit, indexLiefereinheit, indexMenge, indexPreis,
        indexAnzahl) = indexes.take(8)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val produktId = Some(ProduktId(row.value[Long](indexProduktId)))
      val produzentId = ProduzentId(row.value[Long](indexProduzentId))
      val produkt = produkte.find(_.id == produktId).getOrElse(throw ParseException(s"No produkt found for id $produktId"))
      val produzent = produzenten.find(_.id == produzentId).getOrElse(throw ParseException(s"No produzent found for id $produzentId"))

      Lieferposition(
        id = LieferpositionId(id),
        lieferungId = LieferungId(row.value[Long](indexLieferungId)),
        produktId = produktId,
        //TODO: verify produktbeschrieb
        produktBeschrieb = produkt.name,
        produzentId = produzentId,
        produzentKurzzeichen = produzent.kurzzeichen,
        preisEinheit = row.value[Option[BigDecimal]](indexPreisEinheit),
        einheit = Liefereinheit(row.value[String](indexLiefereinheit)),
        menge = row.value[Option[BigDecimal]](indexMenge),
        preis = row.value[Option[BigDecimal]](indexPreis),
        anzahl = row.value[Int](indexAnzahl),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseVertriebe(vertriebsarten: List[Vertriebsart]) = {
    parse[Vertrieb, VertriebId]("id", Seq("abotyp_id", "beschrieb", "liefertag", "anzahl_abos") ++ modifiCols) { id => indexes => row =>
      val Seq(indexAbotypId, indexBeschrieb, indexLiefertag, indexAnzahlAbos) = indexes.take(4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val vertriebId = VertriebId(id)
      val abotypId = AbotypId(row.value[Long](indexAbotypId))
      val beschrieb = row.value[Option[String]](indexBeschrieb)
      /*val vaBeschrieb = vertriebsart match {
          case dl: Depotlieferung =>
            depots.find(_.id == dl.depotId).getOrElse(throw ParseException(s"No depot found for id ${dl.depotId}")).name
          case hl: Heimlieferung =>
            touren.find(_.id == hl.tourId).getOrElse(throw ParseException(s"No tour found for id ${hl.tourId}")).name
          case pl: Postlieferung => ""
        }*/
      val liefertag = Lieferzeitpunkt(row.value[String](indexLiefertag))
      val anzahlAbos = row.value[Int](indexAnzahlAbos)

      Vertrieb(vertriebId, abotypId, liefertag, beschrieb,
        anzahlAbos,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator)))
    }

  }

  def parseLieferungen(abotypen: List[Abotyp], vertriebe: List[Vertrieb], abwesenheiten: List[Abwesenheit], lieferplanungen: List[Lieferplanung],
    depots: List[Depot], touren: List[Tour]) = {
    parse[Lieferung, LieferungId]("id", Seq("abotyp_id", "vertrieb_id", "lieferplanung_id", "status", "datum", "anzahl_abwesenheiten", "durchschnittspreis",
      "anzahl_lieferungen", "anzahl_koerbe_zu_liefern", "anzahl_saldo_zu_tief", "zielpreis", "preis_total") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexAbotypId, indexVertriebId, indexLieferplanungId, indexStatus, indexDatum, indexAnzahlAbwesenheiten, indexDurchschnittspreis,
        indexAnzahlLieferungen, indexAnzahlKoerbeZuLiefern, indexAnzahlSaldoZuTief, indexZielpreis, indexPreisTotal) = indexes.take(12)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val lieferungId = LieferungId(id)
      val abotypId = AbotypId(row.value[Long](indexAbotypId))
      val abotyp = abotypen.find(_.id == abotypId).getOrElse(throw ParseException(s"No abotyp found for id:$abotypId"))
      val vertriebId = VertriebId(row.value[Long](indexVertriebId))
      val vertrieb = vertriebe.find(_.id == vertriebId).getOrElse(throw ParseException(s"No vertrieb found for id $vertriebId"))

      val vBeschrieb = vertrieb.beschrieb

      val durchschnittspreis = row.value[BigDecimal](indexDurchschnittspreis)
      val anzahlLieferungen = row.value[Int](indexAnzahlLieferungen)
      val preisTotal = row.value[BigDecimal](indexPreisTotal)

      val lieferplanungId = row.value[Option[Long]](indexLieferplanungId).map(LieferplanungId)

      Lieferung(
        id = lieferungId,
        abotypId = abotypId,
        abotypBeschrieb = abotyp.beschreibung.getOrElse(""),
        vertriebId = vertriebId,
        vertriebBeschrieb = vBeschrieb,
        status = LieferungStatus(row.value[String](indexStatus)),
        datum = row.value[DateTime](indexDatum),
        durchschnittspreis = row.value[BigDecimal](indexDurchschnittspreis),
        anzahlLieferungen = row.value[Int](indexAnzahlLieferungen),
        anzahlKoerbeZuLiefern = row.value[Int](indexAnzahlKoerbeZuLiefern),
        anzahlAbwesenheiten = row.value[Int](indexAnzahlAbwesenheiten),
        anzahlSaldoZuTief = row.value[Int](indexAnzahlSaldoZuTief),
        zielpreis = row.value[Option[BigDecimal]](indexZielpreis),
        preisTotal = row.value[BigDecimal](indexPreisTotal),
        lieferplanungId = lieferplanungId,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseAbos(kundeIdMapping: Map[Long, KundeId], kunden: List[Kunde], vertriebsartIdMapping: Map[Long, VertriebsartId], vertriebsarten: List[Vertriebsart], vertriebe: List[Vertrieb],
    abotypen: List[Abotyp], depotIdMapping: Map[Long, DepotId], depots: List[Depot],
    tourIdMapping: Map[Long, TourId], tours: List[Tour], abwesenheiten: List[Abwesenheit]) = {
    parse[Abo, AboId]("id", Seq("kunde_id", "vertriebsart_id", "start", "ende",
      "guthaben_vertraglich", "guthaben", "guthaben_in_rechnung", "letzte_lieferung", "anzahl_abwesenheiten", "anzahl_lieferungen",
      "depot_id", "tour_id") ++ modifiCols) { id => indexes =>
      row =>
        //match column indexes
        val Seq(kundeIdIndex, vertriebsartIdIndex, startIndex, endeIndex,
          guthabenVertraglichIndex, guthabenIndex, guthabenInRechnungIndex, indexLetzteLieferung, indexAnzahlAbwesenheiten, lieferungenIndex,
          depotIdIndex, tourIdIndex) = indexes.take(12)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val kundeIdInt = row.value[Long](kundeIdIndex)
        val vertriebsartIdInt = row.value[Long](vertriebsartIdIndex)
        val start = row.value[DateTime](startIndex)
        val ende = row.value[Option[DateTime]](endeIndex)
        val aboId = AboId(id)

        val guthabenVertraglich = row.value[Option[Int]](guthabenVertraglichIndex)
        val guthaben = row.value[Int](guthabenIndex)
        val guthabenInRechnung = row.value[Int](guthabenInRechnungIndex)

        val letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung)
        //calculate count
        val anzahlAbwesenheiten = parseTreeMap(row.value[String](indexAnzahlAbwesenheiten))(identity, _.toInt)
        val anzahlLieferungen = parseTreeMap(row.value[String](lieferungenIndex))(identity, _.toInt)

        val erstelldat = row.value[DateTime](indexErstelldat)
        val ersteller = PersonId(row.value[Long](indexErsteller))
        val modifidat = row.value[DateTime](indexModifidat)
        val modifikator = PersonId(row.value[Long](indexModifikator))

        val kundeId = kundeIdMapping.getOrElse(kundeIdInt, throw ParseException(s"Kunde id $kundeIdInt referenced from abo not found"))
        val kunde = kunden.filter(_.id == kundeId).headOption.map(_.bezeichnung).getOrElse(throw ParseException(s"Kunde not found for id:$kundeId"))

        val vertriebsartId = vertriebsartIdMapping.getOrElse(vertriebsartIdInt, throw ParseException(s"Vertriebsart id $vertriebsartIdInt referenced from abo not found"))
        val vertriebsart = vertriebsarten.filter(_.id == vertriebsartId).headOption.getOrElse(throw ParseException(s"Vertriebsart not found for id:$vertriebsartId"))
        val vertriebId = vertriebsart.vertriebId
        val vertrieb = vertriebe.filter(_.id == vertriebId).headOption.getOrElse(throw ParseException(s"Vertrieb not found for id:$vertriebId"))
        val abotypId = vertrieb.abotypId;
        val abotypName = abotypen.filter(_.id == abotypId).headOption.map(_.name).getOrElse(throw ParseException(s"Abotyp not found for id:$abotypId"))
        val depotIdOpt = row.value[Option[Long]](depotIdIndex)
        val tourIdOpt = row.value[Option[Long]](tourIdIndex)

        depotIdOpt.map { depotIdInt =>
          val depotId = depotIdMapping.getOrElse(depotIdInt, throw ParseException(s"Depot id $depotIdInt referenced from abo not found"))
          val depotName = depots.filter(_.id == depotId).headOption.map(_.name).getOrElse(s"Depot not found with id:$depotId")
          DepotlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, abotypId, abotypName, depotId, depotName,
            start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
            anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
        }.getOrElse {
          tourIdOpt.map { tourIdInt =>
            val tourId = tourIdMapping.getOrElse(tourIdInt, throw ParseException(s"Tour id tourIdInt referenced from abo not found"))
            val tourName = tours.filter(_.id == tourId).headOption.map(_.name).getOrElse(s"Tour not found with id:$tourId")
            HeimlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, abotypId, abotypName, tourId, tourName,
              start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
              anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
          }.getOrElse {
            PostlieferungAbo(aboId, kundeId, kunde, vertriebsartId, vertriebId, abotypId, abotypName,
              start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
              anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
          }
        }
    }
  }

  def parseBestellungen(produzenten: List[Produzent], lieferplanungen: List[Lieferplanung]) = {
    parse[Bestellung, BestellungId]("id", Seq("produzent_id", "lieferplanung_id", "datum", "datum_abrechnung", "preis_total") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduzentId, indexLieferplanungId, indexDatum, indexDatumAbrechnung, indexPreisTotal) = indexes.take(5)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val produzentId = ProduzentId(row.value[Long](indexProduzentId))
      val produzent = produzenten.find(_.id == produzentId).getOrElse(throw ParseException(s"No produzent found with id $produzentId"))

      val lieferplanungId = LieferplanungId(row.value[Long](indexLieferplanungId))

      Bestellung(
        id = BestellungId(id),
        produzentId = produzentId,
        produzentKurzzeichen = produzent.kurzzeichen,
        lieferplanungId,
        status = Offen,
        datum = row.value[DateTime](indexDatum),
        datumAbrechnung = row.value[Option[DateTime]](indexDatumAbrechnung),
        preisTotal = row.value[BigDecimal](indexPreisTotal),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseBestellpositionen(produkte: List[Produkt]) = {
    parse[Bestellposition, BestellpositionId]("id", Seq("bestellung_id", "produkt_id", "preis_einheit", "einheit", "menge", "preis",
      "anzahl") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexBestellungId, indexProduktId, indexPreisEinheit, indexEinheit, indexMenge, indexPreis, indexAnzahl) = indexes.take(7)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      val produktId = Some(ProduktId(row.value[Long](indexProduktId)))
      val produkt = produkte.find(_.id == produktId).getOrElse(throw ParseException(s"No produkt found for id $produktId"))

      Bestellposition(
        BestellpositionId(id),
        bestellungId = BestellungId(row.value[Long](indexBestellungId)),
        produktId,
        //TODO: verify
        produktBeschrieb = produkt.name,
        preisEinheit = row.value[Option[BigDecimal]](indexPreisEinheit),
        einheit = Liefereinheit(row.value[String](indexEinheit)),
        menge = row.value[BigDecimal](indexMenge),
        preis = row.value[Option[BigDecimal]](indexPreis),
        anzahl = row.value[Int](indexAnzahl),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseCustomKundentypen = {
    parse[CustomKundentyp, CustomKundentypId]("id", Seq("kundentyp", "beschreibung", "anzahl_verknuepfungen") ++ modifiCols) { id => indexes => row =>
      //match column indexes
      val Seq(indexKundentyp, indexBeschreibung, indexAnzahlVerknuepfungen) = indexes.take(3)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

      CustomKundentyp(
        CustomKundentypId(id),
        kundentyp = KundentypId(row.value[String](indexKundentyp)),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        anzahlVerknuepfungen = row.value[Int](indexAnzahlVerknuepfungen),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

  def parseTreeMap[K: Ordering, V](value: String)(kf: String => K, vf: String => V): TreeMap[K, V] = {
    (TreeMap.empty[K, V] /: value.split(",")) { (tree, str) =>
      str.split("=") match {
        case Array(left, right) =>
          tree + (kf(left) -> vf(right))
        case _ =>
          tree
      }
    }
  }

  def parseMap[K, V](value: String)(kf: String => K, vf: String => V): Map[K, V] = {
    (Map.empty[K, V] /: value.split(",")) { (tree, str) =>
      str.split("=") match {
        case Array(left, right) =>
          tree + (kf(left) -> vf(right))
        case _ =>
          tree
      }
    }
  }

  def parse[E <: BaseEntity[I], I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Long => Seq[Int] => Row => E) = { name: String => table: Table =>
    var idMapping = Map[Long, I]()
    val parseResult = parseImpl(name, table, idCol, colNames)(entityFactory) {
      case (id, entity) =>
        val entityId = entity.id
        idMapping = idMapping + (id -> entityId)
        Some(entity)
    }
    (parseResult, idMapping)
  }

  def parseImpl[E <: BaseEntity[_], P, R](name: String, table: Table, idCol: String, colNames: Seq[String])(entityFactory: Long => Seq[Int] => Row => P)(resultHandler: (Long, P) => Option[R]): List[R] = {
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
        val result = entityFactory(id)(otherIndexes)(row)
        resultHandler(id, result)
      }.getOrElse(None)
    }).flatten
  }

  def columnIndexes(header: Row, sheet: String, names: Seq[String], maxCols: Option[Int] = None) = {
    log.debug(s"columnIndexes for:$names")
    val headerMap = headerMappings(header, names, maxCols.getOrElse(names.size * 2))
    names.map { name =>
      headerMap.get(name.toLowerCase.trim).getOrElse(throw ParseException(s"Missing column '$name' in sheet '$sheet'"))
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
    vertriebsarten: List[Vertriebsart],
    vertriebe: List[Vertrieb],
    lieferungen: List[Lieferung],
    lieferplanungen: List[Lieferplanung],
    lieferpositionen: List[Lieferposition],
    abos: List[Abo],
    abwesenheiten: List[Abwesenheit],
    produkte: List[Produkt],
    produktekategorien: List[Produktekategorie],
    produktProduktekategorien: List[ProduktProduktekategorie],
    produzenten: List[Produzent],
    produktProduzenten: List[ProduktProduzent],
    bestellungen: List[Bestellung],
    bestellpositionen: List[Bestellposition]
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
      sheet(name).map(t => f(name)(t)).getOrElse(throw ParseException(s"Missing sheet '$name'"))
    }
  }

  implicit class MyCell(self: Cell) {
    val allSupportedDateFormats = List(
      DateTimeFormat.forPattern("dd.MM.yy"),
      DateTimeFormat.forPattern("dd.MM.yyyy"),
      DateTimeFormat.forPattern("MM/dd/yy"),
      DateTimeFormat.forPattern("MM/dd/yyyy")
    )

    def tryParseDate(value: String, nextFormats: List[DateTimeFormatter] = allSupportedDateFormats): DateTime = {
      nextFormats match {
        case head :: tail => try {
          DateTime.parse(value, head)
        } catch {
          case e: Exception => tryParseDate(value, tail)
        }
        case Nil => throw ParseException(s"No matching date format found for value:$value")
      }
    }

    def value[T: TypeTag]: T = {
      val typ = typeOf[T]
      try {
        (typ match {
          case t if t =:= typeOf[Boolean] => self.getStringValue.toLowerCase match {
            case "true" | "richtig" | "wahr" | "1" | "x" => true
            case "false" | "falsch" | "0" => false
            case x => throw ParseException(s"Unsupported boolean format:'$x' on col:${self.getColumnIndex}, row:${self.getRowIndex}")
          }

          case t if t =:= typeOf[String] => self.getStringValue
          case t if t =:= typeOf[Option[String]] => self.getStringOptionValue
          case t if t =:= typeOf[Double] => self.getStringValue.toDouble
          case t if t =:= typeOf[BigDecimal] => BigDecimal(self.getStringValue.toDouble)
          case t if t =:= typeOf[Option[BigDecimal]] => self.getStringOptionValue.map(s => BigDecimal(s.toDouble))
          case t if t =:= typeOf[Date] => self.getDateValue
          case t if t =:= typeOf[DateTime] => tryParseDate(self.getStringValue)
          case t if t =:= typeOf[Option[DateTime]] => self.getStringOptionValue.map(s => tryParseDate(s))
          case t if t =:= typeOf[Int] => self.getStringValue.toInt
          case t if t =:= typeOf[Option[Int]] => getStringOptionValue.map(_.toInt)
          case t if t =:= typeOf[Long] => self.getStringValue.toLong
          case t if t =:= typeOf[Option[Long]] => getStringOptionValue.map(_.toLong)
          case t if t =:= typeOf[Float] => self.getStringValue.toFloat
          case t if t =:= typeOf[Option[Float]] => self.getStringOptionValue.map(_.toFloat)
          case _ =>
            throw ParseException(s"Unsupported format:$typ on col:${self.getColumnIndex}, row:${self.getRowIndex}")
        }).asInstanceOf[T]
      } catch {
        case error: Throwable => {
          val sheet = self.getTable.getTableName
          val row = self.getRowIndex
          val col = self.getColumnIndex
          val displayValue = self.getDisplayText
          val title: String = if (row > 0) {
            self.getTable.getRowByIndex(0).getCellByIndex(col).getDisplayText
          } else {
            "<notitle>"
          }

          throw new ParseException(s"Couldn't parse value in sheet:$sheet, column:$col, row:$row, title:$title => displayValue=$displayValue, error:$error")
        }
      }
    }

    def getStringOptionValue: Option[String] = {
      self.getStringValue match { case null | "" => None; case s => Some(s) }
    }
  }

  implicit class MyRow(self: Row) {
    def value[T: TypeTag](index: Int): T = {
      val value = self.getCellByIndex(index).value[T]
      value match {
        case x: String => x.trim().asInstanceOf[T]
        case _ => value
      }
    }
  }
}