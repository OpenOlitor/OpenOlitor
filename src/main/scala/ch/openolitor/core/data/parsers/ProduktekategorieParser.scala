package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProduktekategorieParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Produktekategorie, ProduktekategorieId]("id", Seq("beschreibung") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBeschreibung) = indexes take (1)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

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
}