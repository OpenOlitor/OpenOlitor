package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProduktProduzentParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[ProduktProduzent, ProduktProduzentId]("id", Seq("produkt_id", "produzent_id") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduktId, indexProduzentId) = indexes take (2)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

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
}