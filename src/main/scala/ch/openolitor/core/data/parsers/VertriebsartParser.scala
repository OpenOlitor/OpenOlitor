package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object VertriebsartParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Vertriebsart, VertriebsartId]("id", Seq("vertrieb_id", "depot_id", "tour_id", "anzahl_abos") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexVertriebId, indexDepotId, indexTourId, indexAnzahlAbos) = indexes take (4)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val vertriebsartId = VertriebsartId(id)
      val vertriebId = VertriebId(row.value[Long](indexVertriebId))
      val depotIdOpt = row.value[Option[Long]](indexDepotId) map (DepotId)
      val tourIdOpt = row.value[Option[Long]](indexTourId) map (TourId)
      val anzahlAbos = row.value[Int](indexAnzahlAbos)

      depotIdOpt map { depotId =>
        Depotlieferung(
          vertriebsartId,
          vertriebId, depotId, anzahlAbos,
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
      } getOrElse {
        tourIdOpt map { tourId =>
          Heimlieferung(vertriebsartId, vertriebId, tourId, anzahlAbos,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        } getOrElse {
          Postlieferung(vertriebsartId, vertriebId, anzahlAbos,
            //modification flags
            erstelldat = row.value[DateTime](indexErstelldat),
            ersteller = PersonId(row.value[Long](indexErsteller)),
            modifidat = row.value[DateTime](indexModifidat),
            modifikator = PersonId(row.value[Long](indexModifikator)))
        }
      }
    }
  }
}