package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object TourlieferungParser extends EntityParser {
  import EntityParser._

  def parse(abos: List[Abo], kunden: List[Kunde])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Tourlieferung, AboId]("id", Seq("sort") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexSort) = indexes take 1
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight 4

      val aboId = AboId(id)
      val abo = abos collect { case a: HeimlieferungAbo => a } find (_.id == aboId) getOrElse (throw ParseException(s"No abo found with id $aboId"))
      val kunde = kunden find (_.id == abo.kundeId) getOrElse (throw ParseException(s"No abo found with id $aboId"))

      val sort = row.value[Option[Int]](indexSort)

      Tourlieferung(
        aboId,
        abo.tourId,
        abo.abotypId,
        abo.kundeId,
        abo.vertriebsartId,
        abo.vertriebId,
        kunde.bezeichnung,
        kunde.strasse,
        kunde.hausNummer,
        kunde.adressZusatz,
        kunde.plz,
        kunde.ort,
        abo.abotypName,
        sort,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }

}