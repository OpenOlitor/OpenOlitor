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
import ch.openolitor.util.DateTimeUtil
import scala.collection.immutable.TreeMap

class DataImportParser extends Actor with ActorLogging {
  import DataImportParser._

  val receive: Receive = {
    case ParseSpreadsheet(file) =>
      val rec = sender
      rec ! importData(file)
  }

  val modifiCols = Seq("erstelldat", "ersteller", "modifidat", "modifikator")

  def importData(file: File): ImportResult = {
    val doc = SpreadsheetDocument.loadDocument(file)

    //parse all sections
    val (projekte, _) = doc.withSheet("Projekt")(parseProjekte)
    val projekt = projekte.head
    val (personen, _) = doc.withSheet("Personen")(parsePersonen)
    val (kunden, kundeIdMapping) = doc.withSheet("Kunden")(parseKunden(personen))
    val (pendenzen, _) = doc.withSheet("Pendenzen")(parsePendenzen(kunden))
    val (tours, tourIdMapping) = doc.withSheet("Tours")(parseTours)
    val (abotypen, abotypIdMapping) = doc.withSheet("Abotyp")(parseAbotypen)
    val (depots, depotIdMapping) = doc.withSheet("Depots")(parseDepots)
    val (abwesenheiten, _) = doc.withSheet("Abwesenheiten")(parseAbwesenheit)
    val (abos, _) = doc.withSheet("Abos")(parseAbos(kundeIdMapping, kunden, abotypIdMapping, abotypen, depotIdMapping, depots, tourIdMapping, tours, abwesenheiten))
    val (lieferplanungen, _) = doc.withSheet("Lieferplanungen")(parseLieferplanungen)
    val (vertriebsarten, _) = doc.withSheet("Vertriebsarten")(parseVertriebsarten)
    val (lieferungen, _) = doc.withSheet("Lieferungen")(parseLieferungen(abotypen, vertriebsarten, abwesenheiten, lieferplanungen))
    val (produkte, _) = doc.withSheet("Produkte")(parseProdukte)
    val (produzenten, _) = doc.withSheet("Produzenten")(parseProduzenten)
    val (lieferpositionen, _) = doc.withSheet("Lieferpositionen")(parseLieferpositionen(produkte, produzenten))
    val (bestellungen, _) = doc.withSheet("Bestellungen")(parseBestellungen(produzenten, lieferplanungen))
    val (bestellpositionen, _) = doc.withSheet("Bestellpositionen")(parseBestellpositionen(produkte))

    ImportResult(projekt, kunden, personen, abotypen, depots, tours, abos, pendenzen)
  }

  def parseProjekte = {
    parse[Projekt, ProjektId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort",
      "preise_sichtbar", "preise_editierbar", "waehrung") ++ modifiCols) { id =>
      indexes =>
        row =>
          //match column indexes
          val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexPreiseSichtbar,
            indexPreiseEditierbar, indexWaehrung) = indexes
          val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

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
            waehrung = Waehrung(row.value[String](indexWaehrung)),
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = UserId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseKunden(personen: List[Person]) = {
    parse[Kunde, KundeId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort", "bemerkungen",
      "abweichende_lieferadresse", "bezeichnung_lieferung", "strasse_lieferung", "haus_nummer_lieferung",
      "adress_zusatz_lieferung", "plz_lieferung", "ort_lieferung", "zusatzinfo_lieferung", "typen",
      "anzahl_abos", "anzahl_pendenzen", "anzahl_personen") ++ modifiCols) { kundeId =>
      indexes => row =>
        //match column indexes
        val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexBemerkungen,
          indexAbweichendeLieferadresse, indexBezeichnungLieferung, indexStrasseLieferung, indexHausNummerLieferung,
          indexAdresseZusatzLieferung, indexPlzLieferung, indexOrtLieferung, indexZusatzinfoLieferung, indexKundentyp,
          indexAnzahlAbos, indexAnzahlPendenzen, indexAnzahlPersonen) =
          indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val personenByKundeId = personen.filter(_.kundeId == kundeId)
        if (personenByKundeId.isEmpty) {
          sys.error(s"Kunde id $kundeId does not reference any person. At least one person is required")
        }
        Kunde(
          id = KundeId(kundeId),
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
          typen = row.value[Set[String]](indexKundentyp).map(KundentypId),
          //Zusatzinformationen
          anzahlAbos = row.value[Int](indexAnzahlAbos),
          anzahlPendenzen = row.value[Int](indexAnzahlPendenzen),
          anzahlPersonen = row.value[Int](indexAnzahlPersonen),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parsePersonen = {
    parse[Person, PersonId]("id", Seq("kundeId", "anrede", "name", "vorname", "email", "emailAlternative",
      "telefonMobil", "telefonFestnetz", "bemerkungen", "sort") ++ modifiCols) { id =>
      indexes =>
        row =>
          //match column indexes
          val Seq(indexKundeId, indexAnrede, indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil,
            indexTelefonFestnetz, indexBemerkungen, indexSort) = indexes
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
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = UserId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parsePendenzen(kunden: List[Kunde]) = {
    parse[Pendenz, PendenzId]("id", Seq("kundeId", "datum", "bemerkung", "status", "generiert") ++ modifiCols) { id =>
      indexes =>
        row =>
          //match column indexes
          val Seq(indexKundeId, indexDatum, indexBemerkung, indexStatus, indexGeneriert) = indexes
          val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

          val kundeId = KundeId(row.value[Long](indexKundeId))
          val kunde = kunden.find(_.id == kundeId).headOption.getOrElse(sys.error(s"Kunde not found with id $kundeId"))

          Pendenz(
            id = PendenzId(id),
            kundeId = kundeId,
            kundeBezeichnung = kunde.bezeichnung,
            datum = row.value[DateTime](indexDatum),
            bemerkung = row.value[Option[String]](indexBemerkung),
            status = PendenzStatus(row.value[String](indexStatus)),
            generiert = row.value[Boolean](indexGeneriert),
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = UserId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseDepots = {
    parse[Depot, DepotId]("id", Seq("name", "kurzzeichen", "ap_name", "ap_vorname", "ap_telefon", "ap_email", "v_name", "v_vorname", "v_telefon", "v_email", "strasse", "haus_nummer",
      "plz", "ort", "aktiv", "oeffnungszeiten", "farbCode", "iban", "bank", "beschreibung", "max_abonnenten", "anzahl_abonnenten") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexName, indexKurzzeichen, indexApName, indexApVorname, indexApTelefon, indexApEmail,
          indexVName, indexVVorname, indexVTelefon, indexVEmail, indexStrasse, indexHausNummer, indexPLZ, indexOrt,
          indexAktiv, indexOeffnungszeiten, indexFarbCode, indexIBAN, indexBank, indexBeschreibung, indexMaxAbonnenten,
          indexAnzahlAbonnenten) = indexes.take(22)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        //val abos = depot2AbosMapping.get(id).getOrElse(Seq())

        Depot(
          id = DepotId(id),
          name = row.value[String](indexName),
          kurzzeichen = row.value[String](indexKurzzeichen),
          apName = row.value[String](indexApName),
          apVorname = row.value[String](indexApVorname),
          apTelefon = row.value[Option[String]](indexApTelefon),
          apEmail = row.value[String](indexApEmail),
          vName = row.value[String](indexVName),
          vVorname = row.value[String](indexVVorname),
          vTelefon = row.value[Option[String]](indexVTelefon),
          vEmail = row.value[String](indexVEmail),
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
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseTours = {
    parse[Tour, TourId]("id", Seq("name", "beschreibung") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexName, indexBeschreibung) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        Tour(
          id = TourId(id),
          name = row.value[String](indexName),
          beschreibung = row.value[Option[String]](indexBeschreibung),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseAbotypen = {
    parse[Abotyp, AbotypId]("id", Seq("name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten", "saldo_mindestbestand", "admin_prozente", "wird_geplant",
      "kuendigungsfrist", "vertrag", "anzahl_abonnenten", "letzte_lieferung", "waehrung") ++ modifiCols) { id =>
      indexes => row =>
        import DateTimeUtil._

        //match column indexes
        val Seq(indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktivVon,
          indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten,
          indexSaldoMindestbestand, indexAdminProzente, indexWirdGeplant, indexKuendigungsfrist, indexVertrag,
          indexAnzahlAbonnenten, indexLetzteLieferung, indexWaehrung) = indexes
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
          guthabenMindestbestand = row.value[Int](indexSaldoMindestbestand),
          adminProzente = row.value[BigDecimal](indexAdminProzente),
          wirdGeplant = row.value[Boolean](indexWirdGeplant),
          //Zusatzinformationen
          anzahlAbonnenten = row.value[Int](indexAnzahlAbonnenten),
          letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung),
          waehrung = Waehrung(row.value[String](indexWaehrung)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseAbwesenheit = {
    parse[Abwesenheit, AbwesenheitId]("id", Seq("abo_id", "lieferung_id", "datum", "bemerkung") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexAboId, indexLieferungId, indexDatum, indexBemerkung) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        Abwesenheit(
          id = AbwesenheitId(id),
          aboId = AboId(row.value[Long](indexAboId)),
          lieferungId = LieferungId(row.value[Long](indexLieferungId)),
          datum = row.value[DateTime](indexDatum),
          bemerkung = row.value[Option[String]](indexBemerkung),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseVertriebsarten = {
    parse[Vertriebsart, VertriebsartId]("id", Seq("abotyp_id", "depot_id", "tour_id", "liefertag") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexAbotypId, indexDepotId, indexTourId, indexLieferzeitpunkt) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val vertriebsartId = VertriebsartId(id)
        val abotypId = AbotypId(row.value[Long](indexAbotypId))
        val depotIdOpt = row.value[Option[Long]](indexDepotId).map(DepotId)
        val tourIdOpt = row.value[Option[Long]](indexTourId).map(TourId)
        val liefertag = Lieferzeitpunkt(row.value[String](indexLieferzeitpunkt))

        depotIdOpt.map { depotId =>
          Depotlieferung(vertriebsartId,
            abotypId, depotId, liefertag,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = UserId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = UserId(row.value[Long](indexModifikator)))
        }.getOrElse {
          tourIdOpt.map { tourId =>
            Heimlieferung(vertriebsartId, abotypId, tourId, liefertag,
              //modification flags
              erstelldat = row.value[DateTime](indexErstelldat),
              ersteller = UserId(row.value[Long](indexErsteller)),
              modifidat = row.value[DateTime](indexModifidat),
              modifikator = UserId(row.value[Long](indexModifikator)))
          }.getOrElse {
            Postlieferung(vertriebsartId, abotypId, liefertag,
              //modification flags
              erstelldat = row.value[DateTime](indexErstelldat),
              ersteller = UserId(row.value[Long](indexErsteller)),
              modifidat = row.value[DateTime](indexModifidat),
              modifikator = UserId(row.value[Long](indexModifikator)))
          }
        }
    }
  }

  def parseProdukte = {
    parse[Produkt, ProduktId]("id", Seq("name", "verfuegbar_von", "verfuegbar_bis", "kategorien", "standard_menge", "einheit",
      "preis", "produzenten") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexName, indexVerfuegbarVon, indexVerfuegbarBis, indexKategorien, indexStandardMenge, indexEinheit,
          indexPreis, indexProduzenten) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        Produkt(
          id = ProduktId(id),
          name = row.value[String](indexName),
          verfuegbarVon = Liefersaison(row.value[String](indexVerfuegbarVon)),
          verfuegbarBis = Liefersaison(row.value[String](indexVerfuegbarBis)),
          kategorien = row.value[Seq[String]](indexKategorien),
          standardmenge = row.value[Option[BigDecimal]](indexStandardMenge),
          einheit = Liefereinheit(row.value[String](indexEinheit)),
          preis = row.value[BigDecimal](indexPreis),
          produzenten = row.value[Seq[String]](indexProduzenten),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseProduzenten = {
    parse[Produzent, ProduzentId]("id", Seq("name", "vorname", "kurzzeichen", "strasse", "haus_nummer", "adress_zusatz",
      "plz", "ort", "bemerkung", "email", "telefon_mobil", "telefon_festnetz", "iban", "bank", "mwst", "mwst_satz", "mwst_nr", "aktiv") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexName, indexVorname, indexKurzzeichen, indexStrasse, indexHausNummer, indexAdressZusatz,
          indexPlz, indexOrt, indexBemerkung, indexEmail, indexTelefonMobil, indexTelefonFestnetz, indexIban, indexBank, indexMwst,
          indexMwstSatz, indexMwstNr, indexAktiv) = indexes
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
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseLieferplanungen = {
    parse[Lieferplanung, LieferplanungId]("id", Seq("nr", "bemerkung", "status") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexNr, indexBemerkung, indexStatus) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        Lieferplanung(
          id = LieferplanungId(id),
          nr = row.value[Int](indexNr),
          bemerkungen = row.value[Option[String]](indexBemerkung),
          status = LieferungStatus(row.value[String](indexStatus)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseLieferpositionen(produkte: List[Produkt], produzenten: List[Produzent]) = {
    parse[Lieferposition, LieferpositionId]("id", Seq("lieferung_id", "produkt_id", "produzent_id", "preis_einheit", "liefereinheit", "menge", "preis", "anzahl") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexLieferungId, indexProduktId, indexProduzentId, indexPreisEinheit, indexLiefereinheit, indexMenge, indexPreis, indexAnzahl) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val produktId = ProduktId(row.value[Long](indexProduktId))
        val produkt = produkte.find(_.id == produktId).getOrElse(sys.error(s"No produkt found for id $produktId"))

        val produzentId = ProduzentId(row.value[Long](indexProduzentId))
        val produzent = produzenten.find(_.id == produzentId).getOrElse(sys.error(s"No produzent found for id $produzentId"))

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
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseLieferungen(abotypen: List[Abotyp], vertriebsarten: List[Vertriebsart], abwesenheiten: List[Abwesenheit], lieferplanungen: List[Lieferplanung]) = {
    parse[Lieferung, LieferungId]("id", Seq("abotyp_id", "vertriebsart_id", "lieferplanung_id", "datum", "durchschnittspreis", "anzahl_lieferungen",
      "preis_total") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexAbotypId, indexVertriebsartId, indexLieferplanungId, indexDatum, indexDurchschnittspreis,
          indexAnzahlLieferungen, indexPreisTotal) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val lieferungId = LieferungId(id)
        val abotypId = AbotypId(row.value[Long](indexAbotypId))
        val abotyp = abotypen.find(_.id == abotypId).getOrElse(sys.error(s"No abotyp found for id:$abotypId"))
        val vertriebsartId = VertriebsartId(row.value[Long](indexVertriebsartId))
        val vertriebsart = vertriebsarten.find(_.id == vertriebsartId).getOrElse(sys.error(s"No vertriebsart found for id $vertriebsartId"))

        val vaBeschrieb = vertriebsart match {
          case dl: DepotlieferungDetail => dl.depot.name
          case hl: HeimlieferungDetail => hl.tour.name
          case pl: PostlieferungDetail => ""
        }

        val abwesenheitByLieferung = abwesenheiten.filter(_.lieferungId == lieferungId)
        val durchschnittspreis = row.value[BigDecimal](indexDurchschnittspreis)
        val anzahlLieferungen = row.value[Int](indexAnzahlLieferungen)
        val preisTotal = row.value[BigDecimal](indexPreisTotal)

        val lieferplanungId = row.value[Option[Long]](indexLieferplanungId).map(LieferplanungId)
        val lieferplanungNr = lieferplanungId.map(id => lieferplanungen.find(_.id == id).getOrElse(sys.error(s"No lieferplanung found for id $id")).nr)

        Lieferung(
          id = lieferungId,
          abotypId = abotypId,
          abotypBeschrieb = abotyp.beschreibung.getOrElse(""),
          vertriebsartId = vertriebsartId,
          vertriebsartBeschrieb = vaBeschrieb,
          datum = row.value[DateTime](indexDatum),
          anzahlAbwesenheiten = abwesenheitByLieferung.size,
          durchschnittspreis = durchschnittspreis,
          anzahlLieferungen = anzahlLieferungen,
          preisTotal = preisTotal,
          lieferplanungId = lieferplanungId,
          lieferplanungNr = lieferplanungNr,
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseAbos(kundeIdMapping: Map[Long, KundeId], kunden: List[Kunde], abotypIdMapping: Map[Long, AbotypId],
    abotypen: List[Abotyp], depotIdMapping: Map[Long, DepotId], depots: List[Depot],
    tourIdMapping: Map[Long, TourId], tours: List[Tour], abwesenheiten: List[Abwesenheit]) = {
    parse[Abo, AboId]("id", Seq("kundeId", "abotypId", "lieferzeitpunkt", "start", "ende",
      "guthaben_vertraglich", "guthaben", "guthaben_in_rechnung", "letzte_lieferung", "anzahl_abwesenheiten", "anzahl_lieferungen",
      "depotId", "tourId")) { id =>
      indexes =>
        row =>
          //match column indexes
          val Seq(kundeIdIndex, abotypIdIndex, lieferzeitpunktIndex, startIndex, endeIndex,
            guthabenVertraglichIndex, guthabenIndex, guthabenInRechnungIndex, indexLetzteLieferung, indexAnzahlAbwesenheiten, lieferungenIndex,
            depotIdIndex, tourIdIndex) = indexes
          val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

          val kundeIdInt = row.value[Long](kundeIdIndex)
          val abotypIdInt = row.value[Long](abotypIdIndex)
          val start = row.value[DateTime](startIndex)
          val ende = row.value[Option[DateTime]](endeIndex)
          val lieferzeitpunkt = Lieferzeitpunkt(row.value[String](lieferzeitpunktIndex))
          val aboId = AboId(id)

          val guthabenVertraglich = row.value[Option[Int]](guthabenVertraglichIndex)
          val guthaben = row.value[Int](guthabenIndex)
          val guthabenInRechnung = row.value[Int](guthabenInRechnungIndex)

          val letzteLieferung = row.value[Option[DateTime]](indexLetzteLieferung)
          //calculate count
          val anzahlAbwesenheiten = parseTreeMap(row.value[String](indexAnzahlAbwesenheiten))
          val anzahlLieferungen = parseTreeMap(row.value[String](lieferungenIndex))

          val erstelldat = row.value[DateTime](indexErstelldat)
          val ersteller = UserId(row.value[Long](indexErsteller))
          val modifidat = row.value[DateTime](indexModifidat)
          val modifikator = UserId(row.value[Long](indexModifikator))

          val kundeId = kundeIdMapping.getOrElse(kundeIdInt, sys.error(s"Kunde id $kundeIdInt referenced from abo not found"))
          val kunde = kunden.filter(_.id == kundeId).headOption.map(_.bezeichnung).getOrElse(sys.error(s"Kunde not found for id:$kundeId"))
          val abotypId = abotypIdMapping.getOrElse(abotypIdInt, sys.error(s"Abotyp id $abotypIdInt referenced from abo not found"))
          val abotypName = abotypen.filter(_.id == abotypId).headOption.map(_.name).getOrElse(sys.error(s"Abotyp not found for id:$abotypId"))
          val depotIdOpt = row.value[Option[Long]](depotIdIndex)
          val tourIdOpt = row.value[Option[Long]](tourIdIndex)

          depotIdOpt.map { depotIdInt =>
            val depotId = depotIdMapping.getOrElse(depotIdInt, sys.error(s"Depot id $depotIdInt referenced from abo not found"))
            val depotName = depots.filter(_.id == depotId).headOption.map(_.name).getOrElse(s"Depot not found with id:$depotId")
            DepotlieferungAbo(aboId, kundeId, kunde, abotypId, abotypName, depotId, depotName,
              lieferzeitpunkt, start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
              anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
          }.getOrElse {
            tourIdOpt.map { tourIdInt =>
              val tourId = tourIdMapping.getOrElse(tourIdInt, sys.error(s"Tour id tourIdInt referenced from abo not found"))
              val tourName = tours.filter(_.id == tourId).headOption.map(_.name).getOrElse(s"Tour not found with id:$tourId")
              HeimlieferungAbo(aboId, kundeId, kunde, abotypId, abotypName, tourId, tourName,
                lieferzeitpunkt, start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
                anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
            }.getOrElse {
              PostlieferungAbo(aboId, kundeId, kunde, abotypId, abotypName,
                lieferzeitpunkt, start, ende, guthabenVertraglich, guthaben, guthabenInRechnung, letzteLieferung, anzahlAbwesenheiten,
                anzahlLieferungen, erstelldat, ersteller, modifidat, modifikator)
            }
          }
    }
  }

  def parseBestellungen(produzenten: List[Produzent], lieferplanungen: List[Lieferplanung]) = {
    parse[Bestellung, BestellungId]("id", Seq("produzent_id", "lieferplanung_id", "datum", "datum_abrechnung", "preis_total") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexProduzentId, indexLieferplanungId, indexDatum, indexDatumAbrechnung, indexPreisTotal) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val produzentId = ProduzentId(row.value[Long](indexProduzentId))
        val produzent = produzenten.find(_.id == produzentId).getOrElse(sys.error(s"No produzent found with id $produzentId"))

        val lieferplanungId = LieferplanungId(row.value[Long](indexLieferplanungId))
        val lieferplanungNr = lieferplanungen.find(_.id == id).getOrElse(sys.error(s"No lieferplanung found for id $id")).nr

        Bestellung(
          id = BestellungId(id),
          produzentId = produzentId,
          produzentKurzzeichen = produzent.kurzzeichen,
          lieferplanungId,
          lieferplanungNr,
          datum = row.value[DateTime](indexDatum),
          datumAbrechnung = row.value[Option[DateTime]](indexDatumAbrechnung),
          preisTotal = row.value[BigDecimal](indexPreisTotal),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseBestellpositionen(produkte: List[Produkt]) = {
    parse[Bestellposition, BestellpositionId]("id", Seq("bestellung_id", "produkt_id", "preis_einheit", "einheit", "menge", "preis",
      "anzahl") ++ modifiCols) { id =>
      indexes => row =>
        //match column indexes
        val Seq(indexBestellungId, indexProduktId, indexPreisEinheit, indexEinheit, indexMenge, indexPreis, indexAnzahl) = indexes
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes.takeRight(4)

        val produktId = ProduktId(row.value[Long](indexProduktId))
        val produkt = produkte.find(_.id == produktId).getOrElse(sys.error(s"No produkt found for id $produktId"))

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
          ersteller = UserId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = UserId(row.value[Long](indexModifikator)))
    }
  }

  def parseTreeMap(value: String) = {
    (TreeMap.empty[String, Int] /: value.split(",")) { (tree, str) =>
      str.split("=") match {
        case Array(left, right) =>
          tree + (left -> right.toInt)
        case _ =>
          tree
      }
    }
  }

  def parse[E <: BaseEntity[I], I <: BaseId](idCol: String, colNames: Seq[String])(entityFactory: Long => Seq[Int] => Row => E) = { name: String =>
    table: Table =>
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
    projekt: Projekt,
    kunden: List[Kunde],
    personen: List[Person],
    abotypen: List[Abotyp],
    depots: List[Depot],
    tours: List[Tour],
    abos: List[Abo],
    pendenzen: List[Pendenz])

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