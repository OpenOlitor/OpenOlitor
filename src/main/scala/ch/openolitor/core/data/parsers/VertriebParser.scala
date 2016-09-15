package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object VertriebParser extends EntityParser {
  import EntityParser._

  def parse(vertriebsarten: List[Vertriebsart])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Vertrieb, VertriebId]("id", Seq("abotyp_id", "beschrieb", "liefertag", "anzahl_abos", "durchschnittspreis", "anzahl_lieferungen") ++ modifyColumns) { id => indexes => row =>
      val Seq(indexAbotypId, indexBeschrieb, indexLiefertag, indexAnzahlAbos, indexDurchschnittspreis, indexLieferungen) = indexes take (6)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val vertriebId = VertriebId(id)
      val abotypId = AbotypId(row.value[Long](indexAbotypId))
      val beschrieb = row.value[Option[String]](indexBeschrieb)
      val liefertag = Lieferzeitpunkt(row.value[String](indexLiefertag))
      val anzahlAbos = row.value[Int](indexAnzahlAbos)

      val anzahlLieferungen = parseTreeMap(row.value[String](indexLieferungen))(identity, _.toInt)
      val durchschnittspreis = parseTreeMap(row.value[String](indexDurchschnittspreis))(identity, BigDecimal(_))

      Vertrieb(vertriebId, abotypId, liefertag, beschrieb,
        anzahlAbos, durchschnittspreis, anzahlLieferungen,
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator)))
    }

  }
}