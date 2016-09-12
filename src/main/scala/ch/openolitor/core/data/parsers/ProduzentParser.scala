package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProduzentParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Produzent, ProduzentId]("id", Seq("name", "vorname", "kurzzeichen", "strasse", "haus_nummer", "adress_zusatz",
      "plz", "ort", "bemerkung", "email", "telefon_mobil", "telefon_festnetz", "iban", "bank", "mwst", "mwst_satz", "mwst_nr", "aktiv") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexName, indexVorname, indexKurzzeichen, indexStrasse, indexHausNummer, indexAdressZusatz,
        indexPlz, indexOrt, indexBemerkung, indexEmail, indexTelefonMobil, indexTelefonFestnetz, indexIban, indexBank, indexMwst,
        indexMwstSatz, indexMwstNr, indexAktiv) = indexes take (18)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      Produzent(
        id = ProduzentId(id),
        name = row.value[String](indexName),
        vorname = row.value[Option[String]](indexVorname),
        kurzzeichen = row.value[String](indexKurzzeichen),
        strasse = row.value[Option[String]](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkung),
        email = row.value[String](indexEmail),
        telefonMobil = row.value[Option[String]](indexTelefonMobil),
        telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
        iban = row.value[Option[String]](indexIban),
        bank = row.value[Option[String]](indexBank),
        mwst = row.value[Boolean](indexMwst),
        mwstSatz = row.value[Option[BigDecimal]](indexMwstSatz),
        mwstNr = row.value[Option[String]](indexMwstNr),
        aktiv = row.value[Boolean](indexAktiv),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}