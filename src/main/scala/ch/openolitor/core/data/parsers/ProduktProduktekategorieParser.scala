package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProduktProduktekategorieParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[ProduktProduktekategorie, ProduktProduktekategorieId]("id", Seq("produkt_id", "produktekategorie_id") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexProduktId, indexProduktekategorieId) = indexes take (2)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

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
}