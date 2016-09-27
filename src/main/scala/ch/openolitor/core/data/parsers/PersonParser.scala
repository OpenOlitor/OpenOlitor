package ch.openolitor.core.data.parsers

import ch.openolitor.core.data.EntityParser
import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.data.ParseException
import java.util.Locale
import org.joda.time.DateTime
import akka.event.LoggingAdapter

object PersonParser extends EntityParser {
  import EntityParser._

  def parse(implicit loggingAdapter: LoggingAdapter) = {
    parseEntity[Person, PersonId]("id", Seq("kunde_id", "anrede", "name", "vorname", "email", "email_alternative",
      "telefon_mobil", "telefon_festnetz", "bemerkungen", "sort", "login_aktiv", "passwort", "letzte_anmeldung", "passwort_wechsel", "rolle") ++ modifyColumns) { id => indexes =>
      row =>
        //match column indexes
        val Seq(indexKundeId, indexAnrede, indexName, indexVorname, indexEmail, indexEmailAlternative, indexTelefonMobil,
          indexTelefonFestnetz, indexBemerkungen, indexSort, indexLoginAktiv, indexPasswort, indexLetzteAnmeldung, indexPasswortWechselErforderlich, indexRolle) = indexes take (15)
        val Seq(indexErstelldat, indexErsteller, indexModifidat, indexModifikator) = indexes takeRight (4)

        val kundeId = KundeId(row.value[Long](indexKundeId))

        Person(
          id = PersonId(id),
          kundeId = kundeId,
          anrede = row.value[Option[String]](indexAnrede) map (r => Anrede(r) getOrElse (throw ParseException(s"Unbekannte Anrede $r bei Person Nr. $id"))),
          name = row.value[String](indexName),
          vorname = row.value[String](indexVorname),
          email = row.value[Option[String]](indexEmail),
          emailAlternative = row.value[Option[String]](indexEmailAlternative),
          telefonMobil = row.value[Option[String]](indexTelefonMobil),
          telefonFestnetz = row.value[Option[String]](indexTelefonFestnetz),
          bemerkungen = row.value[Option[String]](indexBemerkungen),
          sort = row.value[Int](indexSort),
          // security daten
          loginAktiv = row.value[Boolean](indexLoginAktiv),
          passwort = row.value[Option[String]](indexPasswort) map (_.toCharArray),
          letzteAnmeldung = row.value[Option[DateTime]](indexLetzteAnmeldung),
          passwortWechselErforderlich = row.value[Boolean](indexPasswortWechselErforderlich),
          rolle = row.value[Option[String]](indexRolle) map (r => Rolle(r) getOrElse (throw ParseException(s"Unbekannte Rolle $r bei Person Nr. $id"))),
          // modification flags
          erstelldat = row.value[DateTime](indexErstelldat),
          ersteller = PersonId(row.value[Long](indexErsteller)),
          modifidat = row.value[DateTime](indexModifidat),
          modifikator = PersonId(row.value[Long](indexModifikator))
        )
    }
  }
}