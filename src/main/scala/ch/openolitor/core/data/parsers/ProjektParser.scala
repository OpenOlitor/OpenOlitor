package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object ProjektParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Projekt, ProjektId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort",
      "preise_sichtbar", "preise_editierbar", "email_erforderlich", "waehrung", "geschaeftsjahr_monat", "geschaeftsjahr_tag", "two_factor_auth", "sprache") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexPreiseSichtbar,
          indexPreiseEditierbar, indexEmailErforderlich, indexWaehrung, indexGeschaeftsjahrMonat, indexGeschaeftsjahrTag, indexTwoFactorAuth, indexSprache) = indexes take (14)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)
        val twoFactorAuth = parseMap(row.value[String](indexTwoFactorAuth))(r => Rolle(r).getOrElse(throw ParseException(s"Unknown Rolle $r while parsing Projekt")), _.toBoolean)

        Projekt(
          id = ProjektId(id),
          bezeichnung = row.value[String](indexBezeichnung),
          strasse = row.value[Option[String]](indexStrasse),
          hausNummer = row.value[Option[String]](indexHausNummer),
          adressZusatz = row.value[Option[String]](indexAdressZusatz),
          plz = row.value[Option[String]](indexPlz),
          ort = row.value[Option[String]](indexOrt),
          preiseSichtbar = row.value[Boolean](indexPreiseSichtbar),
          preiseEditierbar = row.value[Boolean](indexPreiseEditierbar),
          emailErforderlich = row.value[Boolean](indexEmailErforderlich),
          waehrung = Waehrung(row.value[String](indexWaehrung)),
          geschaeftsjahrMonat = row.value[Int](indexGeschaeftsjahrMonat),
          geschaeftsjahrTag = row.value[Int](indexGeschaeftsjahrTag),
          twoFactorAuthentication = twoFactorAuth,
          sprache = new Locale(row.value[String](indexSprache)),
          //modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}