/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.core.db.evolution.scripts

import ch.openolitor.core.db.evolution._
import scalikejdbc._
import scala.util._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import org.mindrot.jbcrypt.BCrypt
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.core.SystemConfig
import org.joda.time.DateTime
import scala.collection.immutable.TreeMap
import ch.openolitor.core.repositories.BaseWriteRepository
import ch.openolitor.core.NoPublishEventStream
import ch.openolitor.core.models.PersonId

object V1Scripts {
  val StammdatenDBInitializationScript = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //drop all tables
      logger.debug(s"oo-system: cleanupDatabase - drop tables - stammdaten")

      sql"drop table if exists ${vertriebMapping.table}".execute.apply()
      sql"drop table if exists ${postlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungMapping.table}".execute.apply()
      sql"drop table if exists ${depotMapping.table}".execute.apply()
      sql"drop table if exists ${tourMapping.table}".execute.apply()
      sql"drop table if exists ${abotypMapping.table}".execute.apply()
      sql"drop table if exists ${kundeMapping.table}".execute.apply()
      sql"drop table if exists ${pendenzMapping.table}".execute.apply()
      sql"drop table if exists ${customKundentypMapping.table}".execute.apply()
      sql"drop table if exists ${personMapping.table}".execute.apply()
      sql"drop table if exists ${depotlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${heimlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${postlieferungAboMapping.table}".execute.apply()
      sql"drop table if exists ${lieferplanungMapping.table}".execute.apply()
      sql"drop table if exists ${lieferungMapping.table}".execute.apply()
      sql"drop table if exists ${lieferpositionMapping.table}".execute.apply()
      sql"drop table if exists ${bestellungMapping.table}".execute.apply()
      sql"drop table if exists ${bestellpositionMapping.table}".execute.apply()
      sql"drop table if exists ${korbMapping.table}".execute.apply()
      sql"drop table if exists ${produktMapping.table}".execute.apply()
      sql"drop table if exists ${produktekategorieMapping.table}".execute.apply()
      sql"drop table if exists ${produzentMapping.table}".execute.apply()
      sql"drop table if exists ${projektMapping.table}".execute.apply()
      sql"drop table if exists ${produktProduzentMapping.table}".execute.apply()
      sql"drop table if exists ${produktProduktekategorieMapping.table}".execute.apply()
      sql"drop table if exists ${abwesenheitMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables - stammdaten")
      //create tables

      sql"""create table ${vertriebMapping.table}  (
        id BIGINT not null,
        abotyp_id BIGINT not null,
        liefertag varchar(10),      	
        beschrieb varchar(2000),
      	anzahl_abos int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${postlieferungMapping.table}  (
        id BIGINT not null,
        vertrieb_id BIGINT not null,
        anzahl_abos int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${depotlieferungMapping.table} (
        id BIGINT not null,
        vertrieb_id BIGINT not null,
        depot_id BIGINT not null,
        anzahl_abos int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${heimlieferungMapping.table} (
        id BIGINT not null,
        vertrieb_id BIGINT not null,
        tour_id BIGINT not null,
        anzahl_abos int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${depotMapping.table} (
        id BIGINT not null,
        name varchar(50) not null,
        kurzzeichen varchar(6) not null,
        ap_name varchar(50),
        ap_vorname varchar(50),
        ap_telefon varchar(20),
        ap_email varchar(100),
        v_name varchar(50),
        v_vorname varchar(50),
        v_telefon varchar(20),
        v_email varchar(100),
        strasse varchar(50),
        haus_nummer varchar(10),
        plz varchar(10) not null,
        ort varchar(50) not null,
        aktiv varchar(1),
        oeffnungszeiten varchar(200),
        farb_code varchar(20),
        iban varchar(34),
        bank varchar(50),
        beschreibung varchar(200),
        anzahl_abonnenten_max int,
        anzahl_abonnenten int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${tourMapping.table} (
        id BIGINT not null,
        name varchar(50) not null,
        beschreibung varchar(256),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${abotypMapping.table} (
        id BIGINT not null,
        name varchar(50) not null,
        beschreibung varchar(256),
        lieferrhythmus varchar(256),
        aktiv_von datetime default null,
        aktiv_bis datetime default null,
        preis DECIMAL(7,2) not null,
        preiseinheit varchar(20) not null,
        laufzeit int,
        laufzeiteinheit varchar(50),
        vertragslaufzeit varchar(50),
        kuendigungsfrist varchar(50),
        anzahl_abwesenheiten int, farb_code varchar(20),
        zielpreis DECIMAL(7,2),
        guthaben_mindestbestand int,
        admin_prozente DECIMAL(5,2),
        wird_geplant varchar(1) not null,
        anzahl_abonnenten INT not null,
        letzte_lieferung datetime default null,
        waehrung varchar(10),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${kundeMapping.table} (
        id BIGINT not null,
        bezeichnung varchar(50),
        strasse varchar(50) not null,
        haus_nummer varchar(10),
        adress_zusatz varchar(100),
        plz varchar(10) not null,
        ort varchar(50) not null,
        bemerkungen varchar(1000),
        abweichende_lieferadresse varchar(1),
        bezeichnung_lieferung varchar(50),
        strasse_lieferung varchar(50),
        haus_nummer_lieferung varchar(10),
        adress_zusatz_lieferung varchar(100),
        plz_lieferung varchar(10),
        ort_lieferung varchar(50),
        zusatzinfo_lieferung varchar(100),
        typen varchar(200),
        anzahl_abos int not null,
        anzahl_pendenzen int not null,
        anzahl_personen int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${pendenzMapping.table} (
        id BIGINT not null,
        kunde_id varchar(50) not null,
        kunde_bezeichnung varchar(50),
        datum datetime default null,
        bemerkung varchar(2000),
        status varchar(50) not null,
        generiert varchar(1),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${customKundentypMapping.table} (
        id BIGINT not null,
        kundentyp varchar(50) not null,
        beschreibung varchar(250),
        anzahl_verknuepfungen int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${personMapping.table} (
        id BIGINT not null,
        kunde_id varchar(50) not null,
        anrede varchar(20) null,
        name varchar(50) not null,
        vorname varchar(50) not null,
        email varchar(100),
        email_alternative varchar(100),
        telefon_mobil varchar(50),
        telefon_festnetz varchar(50),
        bemerkungen varchar(512),
        sort int not null,
        login_aktiv varchar(1) not null,
        passwort varchar(300),
        letzte_anmeldung datetime,
        passwort_wechsel_erforderlich varchar(1),
        rolle varchar(50),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${depotlieferungAboMapping.table}  (
        id BIGINT not null,
        kunde_id BIGINT not null,
        kunde varchar(100),
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        abotyp_id BIGINT not null,
        abotyp_name varchar(50),
        depot_id BIGINT,
        depot_name varchar(50),
        start datetime not null,
        ende datetime,
        guthaben_vertraglich int,
        guthaben int not null default 0,
        guthaben_in_rechnung int not null default 0,
        letzte_lieferung datetime,
        anzahl_abwesenheiten varchar(500),
        anzahl_lieferungen varchar(500),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${heimlieferungAboMapping.table}  (
        id BIGINT not null,
        kunde_id BIGINT not null,
        kunde varchar(100),
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        abotyp_id BIGINT not null,
        abotyp_name varchar(50),
        tour_id BIGINT,
        tour_name varchar(50),
        start datetime not null,
        ende datetime,
        guthaben_vertraglich int,
        guthaben int not null default 0,
        guthaben_in_rechnung int not null default 0,
        letzte_lieferung datetime,
        anzahl_abwesenheiten varchar(500),
        anzahl_lieferungen varchar(500),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${postlieferungAboMapping.table}  (
        id BIGINT not null,
        kunde_id BIGINT not null,
        kunde varchar(100),
        vertriebsart_id BIGINT not null,
        vertrieb_id BIGINT not null,
        abotyp_id BIGINT not null,
        abotyp_name varchar(50),
        start datetime not null,
        ende datetime,
        guthaben_vertraglich int,
        guthaben int not null default 0,
        guthaben_in_rechnung int not null default 0,
        letzte_lieferung datetime,
        anzahl_abwesenheiten varchar(500),
        anzahl_lieferungen varchar(500),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${lieferplanungMapping.table}  (
        id varchar(36) not null,
        bemerkungen varchar(2000),
        abotyp_depot_tour varchar(2000),
        status varchar(50) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${lieferungMapping.table}  (
        id varchar(36) not null,
        abotyp_id varchar(36) not null,
        abotyp_beschrieb varchar(100) not null,
        vertrieb_id varchar(36) not null,
        vertrieb_beschrieb varchar(100),
        status varchar(50) not null,
        datum datetime not null,
        durchschnittspreis DECIMAL(7,2) not null,
        anzahl_lieferungen int not null,
        anzahl_koerbe_zu_liefern int not null,
        anzahl_abwesenheiten int not null,
        anzahl_saldo_zu_tief int not null,
        zielpreis DECIMAL(7,2),
        preis_total DECIMAL(7,2) not null,
        lieferplanung_id varchar(36),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${lieferpositionMapping.table}  (
        id varchar(36) not null,
        lieferung_id varchar(36) not null,
        produkt_id varchar(36),
        produkt_beschrieb varchar(100) not null,
        produzent_id varchar(36) not null,
        produzent_kurzzeichen varchar(6) not null,
        preis_einheit DECIMAL(7,2),
        einheit varchar(20) not null,
        menge DECIMAL(7,2) not null,
        preis DECIMAL(7,2), anzahl int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${bestellungMapping.table}  (
        id varchar(36) not null,
        produzent_id varchar(36) not null,
        produzent_kurzzeichen varchar(6) not null,
        lieferplanung_id varchar(36) not null,
        status varchar(50) not null,
        datum datetime not null,
        datum_abrechnung datetime default null,
        preis_total DECIMAL(7,2) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${bestellpositionMapping.table}  (
        id varchar(36) not null,
        bestellung_id varchar(36) not null,
        produkt_id varchar(36),
        produkt_beschrieb varchar(100) not null,
        preis_einheit DECIMAL(7,2),
        einheit varchar(20) not null,
        menge DECIMAL(7,2),
        preis DECIMAL(7,2),
        anzahl int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${korbMapping.table}  (
        id varchar(36) not null,
        lieferung_id varchar(36) not null,
        abo_id varchar(36)  not null,
        status varchar(50) not null,
        guthaben_vor_lieferung int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${produktMapping.table}  (
        id BIGINT not null,
        name varchar(140) not null,
        verfuegbar_von varchar(10) not null,
        verfuegbar_bis varchar(10) not null,
        kategorien varchar(300),
        standardmenge DECIMAL(7,3),
        einheit varchar(20) not null,
        preis DECIMAL(7,2) not null,
        produzenten varchar(300),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${produktekategorieMapping.table}  (
        id BIGINT not null,
        beschreibung varchar(50) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${produzentMapping.table}  (
        id BIGINT not null,
        name varchar(50) not null,
        vorname varchar(50),
        kurzzeichen varchar(6) not null,
        strasse varchar(50),
        haus_nummer varchar(10),
        adress_zusatz varchar(100),
        plz varchar(10) not null,
        ort varchar(50) not null,
        bemerkungen varchar(1000),
        email varchar(100) not null,
        telefon_mobil varchar(50),
        telefon_festnetz varchar(50),
        iban varchar(34),
        bank varchar(50),
        mwst varchar(1),
        mwst_satz DECIMAL(4,2),
        mwst_nr varchar(30),
        aktiv varchar(1),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${projektMapping.table}  (
        id BIGINT not null,
        bezeichnung varchar(50) not null,
        strasse varchar(50),
        haus_nummer varchar(10),
        adress_zusatz varchar(100),
        plz varchar(10),
        ort varchar(50),
        preise_sichtbar varchar(1) not null,
        preise_editierbar varchar(1) not null,
        email_erforderlich varchar(1) not null,
        waehrung varchar(10) not null,
        geschaeftsjahr_monat DECIMAL(2,0) not null,
        geschaeftsjahr_tag DECIMAL(2,0) not null,
        two_factor_authentication varchar(100),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${produktProduzentMapping.table} (
        id BIGINT not null,
        produkt_id BIGINT not null,
        produzent_id BIGINT not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${produktProduktekategorieMapping.table} (
        id BIGINT not null,
        produkt_id BIGINT not null,
        produktekategorie_id BIGINT not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${abwesenheitMapping.table} (
        id BIGINT not null,
        abo_id BIGINT not null,
        lieferung_id BIGINT not null,
        datum datetime not null,
        bemerkung varchar(500),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end - stammdaten")
      Success(true)
    }
  }

  val InitialDataScript = new Script with LazyLogging with StammdatenDBMappings with BaseWriteRepository with NoPublishEventStream {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //Create initial account
      val pwd = BCrypt.hashpw("admin", BCrypt.gensalt());
      val pid = sysConfig.mandantConfiguration.dbSeeds.get(classOf[PersonId]).getOrElse(1L)
      implicit val personId = PersonId(pid)
      val kid = sysConfig.mandantConfiguration.dbSeeds.get(classOf[KundeId]).getOrElse(1L)
      val kunde = Kunde(
        id = KundeId(kid),
        bezeichnung = "System Administator",
        strasse = "",
        hausNummer = None,
        adressZusatz = None,
        plz = "",
        ort = "",
        bemerkungen = None,
        abweichendeLieferadresse = false,
        bezeichnungLieferung = None,
        strasseLieferung = None,
        hausNummerLieferung = None,
        adressZusatzLieferung = None,
        plzLieferung = None,
        ortLieferung = None,
        zusatzinfoLieferung = None,
        typen = Set(),
        //Zusatzinformationen
        anzahlAbos = 0,
        anzahlPendenzen = 0,
        anzahlPersonen = 1,
        //modification flags
        erstelldat = DateTime.now,
        ersteller = personId,
        modifidat = DateTime.now,
        modifikator = personId
      )

      val person = Person(
        id = personId,
        kundeId = kunde.id,
        anrede = None,
        name = "Administrator",
        vorname = "System",
        // Email adresse entspricht login name
        email = Some("admin@openolitor.ch"),
        emailAlternative = None,
        telefonMobil = None,
        telefonFestnetz = None,
        bemerkungen = None,
        sort = 1,
        // security data
        loginAktiv = true,
        passwort = Some(pwd.toCharArray),
        letzteAnmeldung = None,
        passwortWechselErforderlich = true,
        rolle = Some(AdministratorZugang),
        // modification flags
        erstelldat = DateTime.now,
        ersteller = personId,
        modifidat = DateTime.now,
        modifikator = personId
      )

      val projId = sysConfig.mandantConfiguration.dbSeeds.get(classOf[ProjektId]).getOrElse(1L)
      val projekt = Projekt(
        id = ProjektId(projId),
        bezeichnung = "Demo Projekt",
        strasse = None,
        hausNummer = None,
        adressZusatz = None,
        plz = None,
        ort = None,
        preiseSichtbar = true,
        preiseEditierbar = true,
        emailErforderlich = true,
        waehrung = CHF,
        geschaeftsjahrMonat = 1,
        geschaeftsjahrTag = 1,
        twoFactorAuthentication = Map(AdministratorZugang -> false, KundenZugang -> true),
        //modification flags
        erstelldat = DateTime.now,
        ersteller = personId,
        modifidat = DateTime.now,
        modifikator = personId
      )

      insertEntity[Kunde, KundeId](kunde);
      insertEntity[Person, PersonId](person);
      insertEntity[Projekt, ProjektId](projekt);

      Success(true)
    }
  }

  val BuchhaltungDBInitializationScript = new Script with LazyLogging with BuchhaltungDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      //drop all tables
      logger.debug(s"oo-system: cleanupDatabase - drop tables - buchhaltung")

      sql"drop table if exists ${rechnungMapping.table}".execute.apply()
      sql"drop table if exists ${zahlungsImportMapping.table}".execute.apply()
      sql"drop table if exists ${zahlungsEingangMapping.table}".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - create tables - buchhaltung")
      //create tables

      sql"""create table ${rechnungMapping.table} (
        id BIGINT not null,
        kunde_id BIGINT not null,
        abo_id BIGINT not null,
        titel varchar(100),
        anzahl_lieferungen INT not null,
        waehrung varchar(10) not null,
        betrag DECIMAL(8,2) not null,
        einbezahlter_betrag DECIMAL(8,2),
        rechnungs_datum datetime not null,
        faelligkeits_datum datetime not null,
        eingangs_datum datetime,
        status varchar(50) not null,
        referenz_nummer varchar(27) not null,
        esr_nummer varchar(54) not null,
        strasse varchar(50) not null,
        haus_nummer varchar(10),
        adress_zusatz varchar(100),
        plz varchar(10) not null,
        ort varchar(50) not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${zahlungsImportMapping.table} (
        id BIGINT not null,
        file varchar(255) not null,
        anzahl_zahlungs_eingaenge int not null,
        anzahl_zahlungs_eingaenge_erledigt int not null,
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      sql"""create table ${zahlungsEingangMapping.table} (
        id BIGINT not null,
        zahlungs_import_id BIGINT not null,
        rechnung_id BIGINT,
        transaktionsart varchar(100) not null,
        teilnehmer_nummer varchar (10) not null,
        referenz_nummer varchar(27) not null,
        waehrung varchar(10) not null,
        betrag DECIMAL(8,2) not null,
        aufgabe_datum datetime,
        verarbeitungs_datum datetime,
        gutschrifts_datum datetime,
        status varchar(50) not null,
        erledigt varchar(1) not null,
        bemerkung varchar(2000),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      logger.debug(s"oo-system: cleanupDatabase - end - buchhaltung")
      Success(true)
    }
  }

  val dbInitializationScripts = Seq(
    StammdatenDBInitializationScript,
    BuchhaltungDBInitializationScript,
    InitialDataScript
  )

  val scripts = dbInitializationScripts
}