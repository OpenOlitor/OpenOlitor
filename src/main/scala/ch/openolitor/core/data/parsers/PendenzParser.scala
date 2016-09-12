package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object PendenzParser extends EntityParser {
  import EntityParser._

  def parse(kunden: List[Kunde])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Pendenz, PendenzId]("id", Seq("kunde_id", "datum", "bemerkung", "status", "generiert") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexDatum, indexBemerkung, indexStatus, indexGeneriert) = indexes take (5)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val kundeId = KundeId(row.value[Long](indexKundeId))
        val kunde = kunden.find(_.id == kundeId).headOption getOrElse (throw ParseException(s"Kunde not found with id $kundeId"))
        Pendenz(
          id = PendenzId(id),
          kundeId = kundeId,
          kundeBezeichnung = kunde.bezeichnung,
          datum = row.value[DateTime](indexDatum),
          bemerkung = row.value[Option[String]](indexBemerkung),
          generiert = row.value[Boolean](indexGeneriert),
          status = PendenzStatus(row.value[String](indexStatus)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}