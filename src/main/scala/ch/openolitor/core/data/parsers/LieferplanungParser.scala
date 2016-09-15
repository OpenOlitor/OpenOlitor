package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object LieferplanungParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Lieferplanung, LieferplanungId]("id", Seq("bemerkung", "abotyp_depot_tour", "status") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBemerkung, indexAbotypDepotTour, indexStatus) = indexes take (4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

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
}