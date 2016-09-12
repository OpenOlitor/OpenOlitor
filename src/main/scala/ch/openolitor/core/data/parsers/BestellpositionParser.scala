package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object BestellpositionParser extends EntityParser {
  import EntityParser._

  def parse(produkte: List[Produkt])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Bestellposition, BestellpositionId]("id", Seq("bestellung_id", "produkt_id", "preis_einheit", "einheit", "menge", "preis",
      "anzahl") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBestellungId, indexProduktId, indexPreisEinheit, indexEinheit, indexMenge, indexPreis, indexAnzahl) = indexes take (7)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produktId = Some(ProduktId(row.value[Long](indexProduktId)))
      val produkt = produkte.find(_.id == produktId) getOrElse (throw ParseException(s"No produkt found for id $produktId"))

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
}