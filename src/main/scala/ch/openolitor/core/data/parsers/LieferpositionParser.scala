package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object LieferpositionParser extends EntityParser {
  import EntityParser._

  def parse(produkte: List[Produkt], produzenten: List[Produzent])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Lieferposition, LieferpositionId]("id", Seq("lieferung_id", "produkt_id", "produzent_id", "preis_einheit", "liefereinheit", "menge", "preis", "anzahl") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexLieferungId, indexProduktId, indexProduzentId, indexPreisEinheit, indexLiefereinheit, indexMenge, indexPreis,
        indexAnzahl) = indexes take (8)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val produktId = Some(ProduktId(row.value[Long](indexProduktId)))
      val produzentId = ProduzentId(row.value[Long](indexProduzentId))
      val produkt = produkte.find(_.id == produktId) getOrElse (throw ParseException(s"No produkt found for id $produktId"))
      val produzent = produzenten.find(_.id == produzentId) getOrElse (throw ParseException(s"No produzent found for id $produzentId"))

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
}