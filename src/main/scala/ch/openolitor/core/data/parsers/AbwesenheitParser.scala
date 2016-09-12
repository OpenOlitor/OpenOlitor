package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object AbwesenheitParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Abwesenheit, AbwesenheitId]("id", Seq("abo_id", "lieferung_id", "datum", "bemerkung") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexAboId, indexLieferungId, indexDatum, indexBemerkung) = indexes take (4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      Abwesenheit(
        id = AbwesenheitId(id),
        aboId = AboId(row.value[Long](indexAboId)),
        lieferungId = LieferungId(row.value[Long](indexLieferungId)),
        datum = row.value[DateTime](indexDatum),
        bemerkung = row.value[Option[String]](indexBemerkung),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}