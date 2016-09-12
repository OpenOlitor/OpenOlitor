package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object BestellungParser extends EntityParser {
  import EntityParser._

  def parse(produzenten: List[Produzent], lieferplanungen: List[Lieferplanung])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Bestellung, BestellungId]("id", Seq("produzent_id", "lieferplanung_id", "datum", "datum_abrechnung", "preis_total", "steuer_satz", "steuer", "total_steuer", "datum_versendet") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduzentId, indexLieferplanungId, indexDatum, indexDatumAbrechnung, indexPreisTotal, indexSteuerSatz, indexSteuer, indexTotalSteuer, indexDatumVersendet) = indexes take (6)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produzentId = ProduzentId(row.value[Long](indexProduzentId))
      val produzent = produzenten.find(_.id == produzentId) getOrElse (throw ParseException(s"No produzent found with id $produzentId"))

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
        steuerSatz = row.value[Option[BigDecimal]](indexSteuerSatz),
        steuer = row.value[BigDecimal](indexSteuer),
        totalSteuer = row.value[BigDecimal](indexTotalSteuer),
        datumVersendet = row.value[Option[DateTime]](indexDatumVersendet),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}