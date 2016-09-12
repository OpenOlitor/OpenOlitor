package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import ch.openolitor.util.DateTimeUtil
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object AbotypParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Abotyp, AbotypId]("id", Seq("name", "beschreibung", "lieferrhythmus", "preis", "preiseinheit", "aktiv_von", "aktiv_bis", "laufzeit",
      "laufzeit_einheit", "farb_code", "zielpreis", "anzahl_abwesenheiten", "guthaben_mindestbestand", "admin_prozente", "wird_geplant",
      "kuendigungsfrist", "vertragslaufzeit", "anzahl_abonnenten", "letzte_lieferung", "waehrung") ++ modifyColumns) { id => indexes => row =>
      import DateTimeUtil._

      //match column indexes
      val Seq(indexName, indexBeschreibung, indexlieferrhytmus, indexPreis, indexPreiseinheit, indexAktivVon,
        indexAktivBis, indexLaufzeit, indexLaufzeiteinheit, indexFarbCode, indexZielpreis, indexAnzahlAbwesenheiten,
        indexGuthabenMindestbestand, indexAdminProzente, indexWirdGeplant, indexKuendigungsfrist, indexVertrag,
        indexAnzahlAbonnenten, indexLetzteLieferung, indexWaehrung) = indexes take (20)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val fristeinheitPattern = """(\d+)(M|W)""".r

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
        vertragslaufzeit = row.value[Option[String]](indexVertrag) map {
          case fristeinheitPattern(wert, "W") => Frist(wert.toInt, Wochenfrist)
          case fristeinheitPattern(wert, "M") => Frist(wert.toInt, Monatsfrist)
        },
        kuendigungsfrist = row.value[Option[String]](indexKuendigungsfrist) map {
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
}