package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object KundeParser extends EntityParser {
  import EntityParser._

  def parse(personen: List[Person])(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Kunde, KundeId]("id", Seq("bezeichnung", "strasse", "haus_nummer", "adress_zusatz", "plz", "ort", "bemerkungen",
      "abweichende_lieferadresse", "bezeichnung_lieferung", "strasse_lieferung", "haus_nummer_lieferung",
      "adress_zusatz_lieferung", "plz_lieferung", "ort_lieferung", "zusatzinfo_lieferung", "typen",
      "anzahl_abos", "anzahl_pendenzen", "anzahl_personen") ++ modifyColumns) { id => indexes => row =>
      //match column indexes
      val Seq(indexBezeichnung, indexStrasse, indexHausNummer, indexAdressZusatz, indexPlz, indexOrt, indexBemerkungen,
        indexAbweichendeLieferadresse, indexBezeichnungLieferung, indexStrasseLieferung, indexHausNummerLieferung,
        indexAdresseZusatzLieferung, indexPlzLieferung, indexOrtLieferung, indexZusatzinfoLieferung, indexKundentyp,
        indexAnzahlAbos, indexAnzahlPendenzen, indexAnzahlPersonen) =
        indexes take (19)
      val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

      val kundeId = KundeId(id)
      val personenByKundeId = personen filter (_.kundeId == kundeId)
      if (personenByKundeId.isEmpty) {
        throw ParseException(s"Kunde id $kundeId does not reference any person. At least one person is required")
      }
      val bez = row.value[Option[String]](indexBezeichnung) getOrElse (s"${personenByKundeId.head.vorname}  ${personenByKundeId.head.name}")
      Kunde(
        kundeId,
        bezeichnung = bez,
        strasse = row.value[String](indexStrasse),
        hausNummer = row.value[Option[String]](indexHausNummer),
        adressZusatz = row.value[Option[String]](indexAdressZusatz),
        plz = row.value[String](indexPlz),
        ort = row.value[String](indexOrt),
        bemerkungen = row.value[Option[String]](indexBemerkungen),
        abweichendeLieferadresse = row.value[Boolean](indexAbweichendeLieferadresse),
        bezeichnungLieferung = row.value[Option[String]](indexBezeichnungLieferung),
        strasseLieferung = row.value[Option[String]](indexStrasseLieferung),
        hausNummerLieferung = row.value[Option[String]](indexHausNummerLieferung),
        adressZusatzLieferung = row.value[Option[String]](indexAdresseZusatzLieferung),
        plzLieferung = row.value[Option[String]](indexPlzLieferung),
        ortLieferung = row.value[Option[String]](indexOrtLieferung),
        zusatzinfoLieferung = row.value[Option[String]](indexZusatzinfoLieferung),
        typen = (row.value[String](indexKundentyp).split(",") map (KundentypId)).toSet,
        //Zusatzinformationen
        anzahlAbos = row.value[Int](indexAnzahlAbos),
        anzahlPendenzen = row.value[Int](indexAnzahlPendenzen),
        anzahlPersonen = row.value[Int](indexAnzahlPersonen),
        //modification flags
        erstelldat = row.value[DateTime](indexErstelldat),
        ersteller = PersonId(row.value[Long](indexErsteller)),
        modifidat = row.value[DateTime](indexModifidat),
        modifikator = PersonId(row.value[Long](indexModifikator))
      )
    }
  }
}