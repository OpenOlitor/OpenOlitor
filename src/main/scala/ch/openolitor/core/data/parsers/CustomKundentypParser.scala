package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object CustomKundentypParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[CustomKundentyp, CustomKundentypId]("id", Seq("kundentyp", "beschreibung", "anzahl_verknuepfungen") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexKundentyp, indexBeschreibung, indexAnzahlVerknuepfungen) = indexes take (3)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      CustomKundentyp(
        CustomKundentypId(id),
        kundentyp = KundentypId(row.value[String](indexKundentyp)),
        beschreibung = row.value[Option[String]](indexBeschreibung),
        anzahlVerknuepfungen = row.value[Int](indexAnzahlVerknuepfungen),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}