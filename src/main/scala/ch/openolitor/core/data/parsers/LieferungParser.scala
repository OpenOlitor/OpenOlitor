package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object LieferungParser extends EntityParser {
  import EntityParser._

  def parse(abotypen: List[Abotyp], vertriebe: List[Vertrieb], abwesenheiten: List[Abwesenheit], lieferplanungen: List[Lieferplanung],
    depots: List[Depot], touren: List[Tour])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Lieferung, LieferungId]("id", Seq("abotyp_id", "vertrieb_id", "lieferplanung_id", "status", "datum", "anzahl_abwesenheiten", "durchschnittspreis",
      "anzahl_lieferungen", "anzahl_koerbe_zu_liefern", "anzahl_saldo_zu_tief", "zielpreis", "preis_total") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexAbotypId, indexVertriebId, indexLieferplanungId, indexStatus, indexDatum, indexAnzahlAbwesenheiten, indexDurchschnittspreis,
        indexAnzahlLieferungen, indexAnzahlKoerbeZuLiefern, indexAnzahlSaldoZuTief, indexZielpreis, indexPreisTotal) = indexes take (12)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val lieferungId = LieferungId(id)
      val abotypId = AbotypId(row.value[Long](indexAbotypId))
      val abotyp = abotypen.find(_.id == abotypId) getOrElse (throw ParseException(s"No abotyp found for id:$abotypId"))
      val vertriebId = VertriebId(row.value[Long](indexVertriebId))
      val vertrieb = vertriebe.find(_.id == vertriebId) getOrElse (throw ParseException(s"No vertrieb found for id $vertriebId"))

      val vBeschrieb = vertrieb.beschrieb

      val durchschnittspreis = row.value[BigDecimal](indexDurchschnittspreis)
      val anzahlLieferungen = row.value[Int](indexAnzahlLieferungen)
      val preisTotal = row.value[BigDecimal](indexPreisTotal)

      val lieferplanungId = row.value[Option[Long]](indexLieferplanungId) map (LieferplanungId)

      Lieferung(
        id = lieferungId,
        abotypId = abotypId,
        abotypBeschrieb = abotyp.beschreibung getOrElse (""),
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
}