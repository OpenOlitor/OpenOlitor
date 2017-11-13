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
package ch.openolitor.stammdaten.repositories

import scalikejdbc._
import sqls.{ distinct, count }
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.Macros._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.FilterExpr
import org.joda.time.LocalDate

trait StammdatenRepositoryQueries extends LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val zusatzAboTyp = zusatzAbotypMapping.syntax("zatyp")
  lazy val person = personMapping.syntax("pers")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val hauptLieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferungJoin = lieferungMapping.syntax("lieferungJ")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")
  lazy val bestellung = bestellungMapping.syntax("bestellung")
  lazy val sammelbestellung = sammelbestellungMapping.syntax("sammelbestellung")
  lazy val bestellposition = bestellpositionMapping.syntax("bestellposition")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val pendenz = pendenzMapping.syntax("pendenz")
  lazy val kundentyp = customKundentypMapping.syntax("kundentyp")
  lazy val postlieferung = postlieferungMapping.syntax("postlieferung")
  lazy val depotlieferung = depotlieferungMapping.syntax("depotlieferung")
  lazy val heimlieferung = heimlieferungMapping.syntax("heimlieferung")
  lazy val depot = depotMapping.syntax("depot")
  lazy val tour = tourMapping.syntax("tour")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val zusatzAbo = zusatzAboMapping.syntax("zusatzAbo")
  lazy val produkt = produktMapping.syntax("produkt")
  lazy val produktekategorie = produktekategorieMapping.syntax("produktekategorie")
  lazy val produzent = produzentMapping.syntax("produzent")
  lazy val projekt = projektMapping.syntax("projekt")
  lazy val kontoDaten = kontoDatenMapping.syntax("kontoDaten")
  lazy val produktProduzent = produktProduzentMapping.syntax("produktProduzent")
  lazy val produktProduktekategorie = produktProduktekategorieMapping.syntax("produktProduktekategorie")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")
  lazy val korb = korbMapping.syntax("korb")
  lazy val vertrieb = vertriebMapping.syntax("vertrieb")
  lazy val tourlieferung = tourlieferungMapping.syntax("tourlieferung")
  lazy val depotAuslieferung = depotAuslieferungMapping.syntax("depotAuslieferung")
  lazy val tourAuslieferung = tourAuslieferungMapping.syntax("tourAuslieferung")
  lazy val postAuslieferung = postAuslieferungMapping.syntax("postAuslieferung")
  lazy val projektVorlage = projektVorlageMapping.syntax("projektVorlage")
  lazy val einladung = einladungMapping.syntax("einladung")

  lazy val lieferpositionShort = lieferpositionMapping.syntax
  lazy val korbShort = korbMapping.syntax

  protected def getAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
        .orderBy(aboTyp.name)
    }.map(abotypMapping(aboTyp)).list
  }

  protected def getExistingZusatzAbotypenQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.abotypId, zusatzAboTyp.id)
        .leftJoin(korbMapping as korb).on(korb.aboId, zusatzAbo.hauptAboId)
        .where.eq(korb.lieferungId, lieferungId)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).list
  }

  protected def getZusatzAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAboTyp))
        .orderBy(zusatzAboTyp.name)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).list
  }

  protected def getKundenQuery = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .orderBy(kunde.bezeichnung)
    }.map(kundeMapping(kunde)).list
  }

  protected def getKundenByKundentypQuery(kundentyp: KundentypId) = {
    // search for kundentyp in typen spalte von Kunde (Komma separierte liste von Kundentypen)
    val kundentypRegex: String = SQLSyntax.createUnsafely(s"""([ ,]|^)${kundentyp.id}([ ,]|$$)+""")
    sql"""
      SELECT ${kunde.result.*} FROM ${kundeMapping as kunde}
      WHERE typen REGEXP ${kundentypRegex}
    """.map(kundeMapping(kunde)).list
  }

  protected def getKundenUebersichtQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, kunde))
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toMany(
        rs => personMapping.opt(person)(rs)
      )
      .map((kunde, personen) => {
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonSummary](p)).toSeq

        copyTo[Kunde, KundeUebersicht](kunde, "ansprechpersonen" -> personenWihoutPwd)
      }).list
  }

  protected def getCustomKundentypenQuery = {
    withSQL {
      select
        .from(customKundentypMapping as kundentyp)
        .orderBy(kundentyp.id)
    }.map(customKundentypMapping(kundentyp)).list
  }

  protected def getKundeDetailQuery(id: KundeId) = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .where.eq(kunde.id, id)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map((kunde, pl, hl, dl, personen, pendenzen) => {
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonDetail](p)).toSeq

        copyTo[Kunde, KundeDetail](kunde, "abos" -> abos, "pendenzen" -> pendenzen, "ansprechpersonen" -> personenWihoutPwd)
      }).single
  }

  protected def getKundeDetailReportQuery(kundeId: KundeId, projekt: ProjektReport) = {
    val x = SubQuery.syntax("x").include(abwesenheit)
    withSQL {
      select
        .from(kundeMapping as kunde)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(kunde.id, depotlieferungAbo.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(kunde.id, heimlieferungAbo.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(kunde.id, postlieferungAbo.kundeId)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(pendenzMapping as pendenz).on(kunde.id, pendenz.kundeId)
        .where.eq(kunde.id, kundeId)
        .orderBy(person.sort)
    }.one(kundeMapping(kunde))
      .toManies(
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => personMapping.opt(person)(rs),
        rs => pendenzMapping.opt(pendenz)(rs)
      )
      .map { (kunde, pl, hl, dl, personen, pendenzen) =>
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.toSet[Person].map(p => copyTo[Person, PersonDetail](p)).toSeq

        copyTo[Kunde, KundeDetailReport](kunde, "abos" -> abos, "pendenzen" -> pendenzen,
          "personen" -> personenWihoutPwd, "projekt" -> projekt)
      }.single
  }

  protected def getPersonenQuery = {
    withSQL {
      select
        .from(personMapping as person)
        .orderBy(person.kundeId, person.sort)
    }.map(personMapping(person)).list
  }

  protected def getPersonenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, kundeId)
        .orderBy(person.sort)
    }.map(personMapping(person)).list
  }

  protected def getPersonenUebersichtQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(kundeMapping as kunde).on(person.kundeId, kunde.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, person))
        .orderBy(person.name)
    }.one(personMapping(person))
      .toOne(
        rs => kundeMapping(kunde)(rs)
      ).map { (person, kunde) =>
          copyTo[Person, PersonUebersicht](
            person,
            "strasse" -> kunde.strasse,
            "hausNummer" -> kunde.hausNummer,
            "adressZusatz" -> kunde.adressZusatz,
            "plz" -> kunde.plz,
            "ort" -> kunde.ort,
            "kundentypen" -> kunde.typen,
            "kundenBemerkungen" -> kunde.bemerkungen
          )
        }.list
  }

  protected def getAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.id, id)
    }.map(abotypMapping(aboTyp)).single
  }

  protected def getZusatzAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(zusatzAbotypMapping as zusatzAboTyp)
        .where.eq(zusatzAboTyp.id, id)
    }.map(zusatzAbotypMapping(zusatzAboTyp)).single
  }

  protected def getDepotlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.abotypId, abotypId)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.abotypId, abotypId)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.abotypId, abotypId)
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, vertriebId)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, vertriebId)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, vertriebId)
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getZusatzAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.vertriebId, vertriebId)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getDepotlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.vertriebId, vertriebId)
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name, depot.head.kurzzeichen)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.list
  }

  protected def getDepotlieferungQuery(depotId: DepotId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .where.eq(depotlieferung.depotId, depotId)
    }.map(depotlieferungMapping(depotlieferung)).list
  }

  protected def getHeimlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.vertriebId, vertriebId)
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.list
  }

  protected def getHeimlieferungQuery(tourId: TourId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .where.eq(heimlieferung.tourId, tourId)
    }.map(heimlieferungMapping(heimlieferung)).list
  }

  protected def getPostlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.vertriebId, vertriebId)
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.list
  }

  protected def getDepotlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.id, vertriebsartId)
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name, depot.head.kurzzeichen)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.single
  }

  protected def getHeimlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.id, vertriebsartId)
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.single
  }

  protected def getPostlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.id, vertriebsartId)
    }.map { rs =>
      val pl = postlieferungMapping(postlieferung)(rs)
      copyTo[Postlieferung, PostlieferungDetail](pl)
    }.single
  }

  protected def getUngeplanteLieferungenQuery(abotypId: AbotypId, vertriebId: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .leftJoin(vertriebMapping as vertrieb).on(lieferung.vertriebId, vertrieb.id)
        .where.eq(vertrieb.abotypId, abotypId).and.eq(lieferung.vertriebId, vertriebId).and.isNull(lieferung.lieferplanungId)
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getUngeplanteLieferungenQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId).and.isNull(lieferung.lieferplanungId)
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getDepotsQuery = {
    withSQL {
      select
        .from(depotMapping as depot)
        .orderBy(depot.name)
    }.map(depotMapping(depot)).list
  }

  protected def getDepotDetailQuery(id: DepotId) = {
    withSQL {
      select
        .from(depotMapping as depot)
        .where.eq(depot.id, id)
    }.map(depotMapping(depot)).single
  }

  protected def getDepotDetailReportQuery(id: DepotId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(depotMapping as depot)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.depotId, depot.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .where.eq(depot.id, id)
    }
      .one(depotMapping(depot))
      .toManies(
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs)
      )
      .map((depot, abos, kunden, personen) => {
        val personenWithoutPwd = personen map (p => copyTo[Person, PersonDetail](p))
        val abosReport = abos.map { abo =>
          kunden.filter(_.id == abo.kundeId).headOption map { kunde =>
            val ansprechpersonen = personenWithoutPwd filter (_.kundeId == kunde.id)
            val kundeReport = copyTo[Kunde, KundeReport](kunde, "personen" -> ansprechpersonen)
            copyTo[DepotlieferungAbo, DepotlieferungAboReport](abo, "kundeReport" -> kundeReport)
          }
        }.flatten
        copyTo[Depot, DepotDetailReport](depot, "projekt" -> projekt, "abos" -> abosReport)
      }).single
  }

  protected def getDepotlieferungAbosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getPersonenByDepotsQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(depotMapping as depot).on(depotlieferungAbo.depotId, depot.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depot))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByDepotsQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(depotMapping as depot).on(depotlieferungAbo.depotId, depot.id)
        .where.eq(depotlieferungAbo.aktiv, true).and(UriQueryParamToSQLSyntaxBuilder.build(filter, depot))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenByTourenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(tourMapping as tour).on(heimlieferungAbo.tourId, tour.id)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, tour))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByTourenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(tourMapping as tour).on(heimlieferungAbo.tourId, tour.id)
        .where.eq(heimlieferungAbo.aktiv, true).and(UriQueryParamToSQLSyntaxBuilder.build(filter, tour))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenByAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.kundeId, person.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(depotlieferungAbo.abotypId, aboTyp.id).or.eq(heimlieferungAbo.abotypId, aboTyp.id).or.eq(postlieferungAbo.abotypId, aboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenAboAktivByAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.kundeId, person.kundeId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.kundeId, person.kundeId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.kundeId, person.kundeId)
        .leftJoin(abotypMapping as aboTyp).on(sqls.eq(depotlieferungAbo.abotypId, aboTyp.id).or.eq(heimlieferungAbo.abotypId, aboTyp.id).or.eq(postlieferungAbo.abotypId, aboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, aboTyp))
        .having(
          sqls.
            eq(depotlieferungAbo.aktiv, true)
            .or.eq(heimlieferungAbo.aktiv, true)
            .or.eq(postlieferungAbo.aktiv, true)
        )
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getPersonenZusatzAboAktivByZusatzAbotypenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(personMapping as person)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(zusatzAbo.kundeId, person.kundeId)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(sqls.eq(zusatzAbo.abotypId, zusatzAboTyp.id))
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAboTyp))
        .having(sqls.eq(zusatzAbo.aktiv, true))
    }.map { rs =>
      copyTo[Person, PersonSummary](personMapping(person)(rs))
    }.list
  }

  protected def getHeimlieferungAbosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAbosByDepotQuery(id: DepotId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.depotId, id)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getDepotlieferungAboAusstehendQuery(id: AboId) = {
    getDepotlieferungAboBaseQuery(id, Some(()))
  }

  protected def getDepotlieferungAboQuery(id: AboId) = {
    getDepotlieferungAboBaseQuery(id, None)
  }

  private def getDepotlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(depotlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(depotlieferungAbo.vertriebId, vertrieb.id)
        .leftJoin(lieferungMapping as lieferung).on(vertrieb.id, lieferung.vertriebId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(depotlieferungAbo.id, id)
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
        ))
    }
      .one(depotlieferungAboMapping(depotlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[DepotlieferungAbo, DepotlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getHeimlieferungAboAusstehendQuery(id: AboId) = {
    getHeimlieferungAboBaseQuery(id, Some(()))
  }

  protected def getHeimlieferungAboQuery(id: AboId) = {
    getHeimlieferungAboBaseQuery(id, None)
  }

  private def getHeimlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(heimlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(heimlieferungAbo.vertriebId, vertrieb.id)
        .leftJoin(lieferungMapping as lieferung).on(vertrieb.id, lieferung.vertriebId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(heimlieferungAbo.id, id)
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
        ))
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[HeimlieferungAbo, HeimlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getPostlieferungAboAusstehendQuery(id: AboId) = {
    getPostlieferungAboBaseQuery(id, Some(()))
  }

  protected def getPostlieferungAboQuery(id: AboId) = {
    getPostlieferungAboBaseQuery(id, None)
  }

  private def getPostlieferungAboBaseQuery(id: AboId, ausstehend: Option[Unit]) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(postlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(postlieferungAbo.vertriebId, vertrieb.id)
        .leftJoin(lieferungMapping as lieferung).on(vertrieb.id, lieferung.vertriebId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(postlieferungAbo.id, id)
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
        ))
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((abo, abw, aboTyp, vertriebe, lieferungen) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[PostlieferungAbo, PostlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def getZusatzAboDetailQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.id, id)
    }.map(zusatzAboMapping(zusatzAbo)).single
  }

  protected def getZusatzAboPerAboQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.hauptAboId, id)
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def countKoerbeQuery(auslieferungId: AuslieferungId) = {
    withSQL {
      select(count(distinct(korb.id)))
        .from(korbMapping as korb)
        .where.eq(korb.auslieferungId, auslieferungId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def countAbwesendQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.lieferungId, lieferungId).and.eq(abwesenheit.aboId, aboId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def countAbwesendQuery(aboId: AboId, datum: LocalDate) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.datum, datum).and.eq(abwesenheit.aboId, aboId)
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def getAktiveDepotlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, vertriebId)
        .and.eq(depotlieferungAbo.abotypId, abotypId)
        .and.le(depotlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.ge(depotlieferungAbo.ende, lieferdatum.toLocalDate) }
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getAktiveHeimlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, vertriebId)
        .and.eq(heimlieferungAbo.abotypId, abotypId)
        .and.le(heimlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.ge(heimlieferungAbo.ende, lieferdatum.toLocalDate) }
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getAktiveZusatzAbosQuery(abotypId: AbotypId, lieferdatum: DateTime, lieferplanungId: LieferplanungId) = {
    // zusätzlich get haupabo, get all Lieferungen where Lieferplanung is equal
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .innerJoin(lieferungMapping as lieferung).on(zusatzAbo.abotypId, lieferung.abotypId)
        .where.eq(zusatzAbo.abotypId, abotypId)
        .and.le(zusatzAbo.start, lieferdatum)
        .and.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.withRoundBracket { _.isNull(zusatzAbo.ende).or.ge(zusatzAbo.ende, lieferdatum) }
    }.map(zusatzAboMapping(zusatzAbo)).list
  }

  protected def getAktivePostlieferungAbosQuery(abotypId: AbotypId, vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, vertriebId)
        .and.eq(postlieferungAbo.abotypId, abotypId)
        .and.le(postlieferungAbo.start, lieferdatum)
        .and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.ge(postlieferungAbo.ende, lieferdatum.toLocalDate) }
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getPendenzenQuery = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .orderBy(pendenz.datum)
    }.map(pendenzMapping(pendenz)).list
  }

  protected def getPendenzenQuery(id: KundeId) = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.kundeId, id)
    }.map(pendenzMapping(pendenz)).list
  }

  protected def getPendenzDetailQuery(id: PendenzId) = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.id, id)
    }.map(pendenzMapping(pendenz)).single
  }

  protected def getProdukteQuery = {
    withSQL {
      select
        .from(produktMapping as produkt)
    }.map(produktMapping(produkt)).list
  }

  protected def getProduktekategorienQuery = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
    }.map(produktekategorieMapping(produktekategorie)).list
  }

  protected def getProduzentenQuery = {
    withSQL {
      select
        .from(produzentMapping as produzent)
    }.map(produzentMapping(produzent)).list
  }

  protected def getProduzentDetailQuery(id: ProduzentId) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, id)
    }.map(produzentMapping(produzent)).single
  }

  protected def getProduzentDetailReportQuery(id: ProduzentId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, id)
    }.map { rs =>
      val p = produzentMapping(produzent)(rs)
      copyTo[Produzent, ProduzentDetailReport](p, "projekt" -> projekt)
    }.single
  }

  protected def getTourenQuery = {
    withSQL {
      select
        .from(tourMapping as tour)
    }.map(tourMapping(tour)).list
  }

  protected def getTourDetailQuery(id: TourId) = {
    withSQL {
      select
        .from(tourMapping as tour)
        .leftJoin(tourlieferungMapping as tourlieferung).on(tour.id, tourlieferung.tourId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(tourlieferung.id, zusatzAbo.hauptAboId)
        .where.eq(tour.id, id)
        .orderBy(tourlieferung.sort)
    }.one(tourMapping(tour))
      .toManies(
        rs => tourlieferungMapping.opt(tourlieferung)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map({ (tour, tourlieferungen, zusatzAbos) =>
        val tourlieferungenDetails = tourlieferungen map { t =>
          val z = zusatzAbos filter (_.hauptAboId == t.id)

          copyTo[Tourlieferung, TourlieferungDetail](t, "zusatzAbos" -> z)
        }

        copyTo[Tour, TourDetail](tour, "tourlieferungen" -> tourlieferungenDetails)
      }).single
  }

  protected def getTourlieferungenQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, tourId)
        .orderBy(tourlieferung.sort)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByKundeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.kundeId, kundeId)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByTourQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, tourId)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }

  protected def getKontoDatenQuery = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
    }.map(kontoDatenMapping(kontoDaten)).single
  }

  protected def getProduktProduzentenQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduzentMapping as produktProduzent)
        .where.eq(produktProduzent.produktId, id)
    }.map(produktProduzentMapping(produktProduzent)).list
  }

  protected def getProduktProduktekategorienQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduktekategorieMapping as produktProduktekategorie)
        .where.eq(produktProduktekategorie.produktId, id)
    }.map(produktProduktekategorieMapping(produktProduktekategorie)).list
  }

  protected def getProduzentDetailByKurzzeichenQuery(kurzzeichen: String) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.kurzzeichen, kurzzeichen)
    }.map(produzentMapping(produzent)).single
  }

  protected def getProduktekategorieByBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
        .where.eq(produktekategorie.beschreibung, bezeichnung)
    }.map(produktekategorieMapping(produktekategorie)).single
  }

  protected def getProduzentenabrechnungQuery(sammelbestellungIds: Seq[SammelbestellungId]) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.in(sammelbestellung.id, sammelbestellungIds.map(_.id))
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen.sortBy(_.steuerSatz) map { b =>
          val p = positionen.filter(_.bestellungId == b.id).sortBy(_.produktBeschrieb)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).list
  }

  protected def getProdukteByProduktekategorieBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktMapping as produkt)
        .where.like(produkt.kategorien, '%' + bezeichnung + '%')
    }.map(produktMapping(produkt)).list
  }

  protected def getLieferplanungenQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
    }.map(lieferplanungMapping(lieferplanung)).list
  }

  protected def getLatestLieferplanungQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .orderBy(lieferplanung.id).desc
        .limit(1)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .where.eq(lieferplanung.id, id)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(lieferung.id, id)
    }.map(lieferplanungMapping(lieferplanung)).single
  }

  protected def getLieferplanungReportQuery(id: LieferplanungId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(lieferung.abotypId, zusatzAboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferplanung.id, id)
        .and.not.withRoundBracket {
          _.eq(lieferung.anzahlKoerbeZuLiefern, 0)
            .and.isNull(aboTyp.id)
        }
    }.one(lieferplanungMapping(lieferplanung))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map { (lieferplanung, lieferungen, abotypen, zusatzabotypen, positionen) =>
        val lieferungenDetails = lieferungen map { l =>
          val p = positionen.filter(_.lieferungId == l.id)
          val iabotyp = (abotypen find (_.id == l.abotypId)) orElse (zusatzabotypen find (_.id == l.abotypId))
          copyTo[Lieferung, LieferungDetail](l, "abotyp" -> iabotyp, "lieferpositionen" -> p, "lieferplanungBemerkungen" -> lieferplanung.bemerkungen)
        }

        copyTo[Lieferplanung, LieferplanungReport](lieferplanung, "lieferungen" -> lieferungenDetails, "projekt" -> projekt)
      }.single
  }

  protected def getLieferungenNextQuery = {
    sql"""
        SELECT
          ${lieferung.result.*}
        FROM
          ${lieferungMapping as lieferung}
        INNER JOIN
		      (SELECT ${lieferung.abotypId} AS abotypId, MIN(${lieferung.datum}) AS MinDateTime
		       FROM ${lieferungMapping as lieferung}
		       INNER JOIN ${abotypMapping as aboTyp}
		       ON ${lieferung.abotypId} = ${aboTyp.id}
		       WHERE ${aboTyp.wirdGeplant} = true
		       AND ${lieferung.lieferplanungId} IS NULL
		       GROUP BY ${lieferung.abotypId}) groupedLieferung
		    ON ${lieferung.abotypId} = groupedLieferung.abotypId
		    AND ${lieferung.datum} = groupedLieferung.MinDateTime
      """.map(lieferungMapping(lieferung)).list
  }

  protected def getLastGeplanteLieferungQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId).and.not.eq(lieferung.status, Ungeplant)
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .join(lieferungMapping as lieferung).on(lieferplanung.id, lieferung.lieferplanungId)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(lieferung.abotypId, zusatzAboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id)
        .and.not.withRoundBracket {
          _.eq(lieferung.anzahlKoerbeZuLiefern, 0)
            .and.eq(lieferung.anzahlAbwesenheiten, 0)
            .and.eq(lieferung.anzahlSaldoZuTief, 0)
            .and.isNull(aboTyp.id)
        }
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => lieferplanungMapping.opt(lieferplanung)(rs)
      )
      .map { (lieferung, abotyp, zusatzAbotyp, positionen, lieferplanung) =>
        val bemerkung = lieferplanung match {
          case Nil => None
          case x => x.head.bemerkungen
        }
        val iabotyp = abotyp.headOption orElse zusatzAbotyp.headOption
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> iabotyp, "lieferpositionen" -> positionen, "lieferplanungBemerkungen" -> bemerkung)
      }.list
  }

  protected def getLieferungQuery(id: AbwesenheitId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .join(abwesenheitMapping as abwesenheit)
        .where.eq(lieferung.id, abwesenheit.lieferungId).and.eq(abwesenheit.id, id)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getExistingZusatzaboLieferungQuery(zusatzAbotypId: AbotypId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, zusatzAbotypId)
        .and.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.eq(lieferung.datum, datum)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.lieferplanungId, id)
        .orderBy(lieferung.datum).desc
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLieferungenQuery(id: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, id)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def sumPreisTotalGeplanteLieferungenVorherQuery(vertriebId: VertriebId, datum: DateTime, startGeschaeftsjahr: DateTime) = {
    sql"""
      select
        sum(${lieferung.preisTotal})
      from
        ${lieferungMapping as lieferung}
      where
        ${lieferung.vertriebId} = ${vertriebId.id}
        and ${lieferung.lieferplanungId} IS NOT NULL
        and ${lieferung.datum} < ${datum}
        and ${lieferung.datum} >= ${startGeschaeftsjahr}
      """
      .map(x => BigDecimal(x.bigDecimalOpt(1).getOrElse(java.math.BigDecimal.ZERO))).single
  }

  protected def getGeplanteLieferungVorherQuery(vertriebId: VertriebId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, vertriebId)
        .and.not.isNull(lieferung.lieferplanungId)
        .and.lt(lieferung.datum, datum)
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getGeplanteLieferungNachherQuery(vertriebId: VertriebId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, vertriebId)
        .and.not.isNull(lieferung.lieferplanungId)
        .and.gt(lieferung.datum, datum)
        .orderBy(lieferung.datum).asc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def countEarlierLieferungOffenQuery(id: LieferplanungId) = {
    sql"""
        SELECT
        count(*)
        FROM ${lieferungMapping as lieferung}
        WHERE ${lieferung.status} = 'Offen'
        AND ${lieferung.lieferplanungId} <> ${id.id}
        AND ${lieferung.datum} <
        (
          SELECT
          MIN(${lieferung.datum})
          FROM ${lieferungMapping as lieferung}
          WHERE ${lieferung.status} = 'Offen'
          AND ${lieferung.lieferplanungId} = ${id.id}
        )
      """
      .map(_.int(1)).single
  }

  protected def getVerfuegbareLieferungenQuery(id: LieferplanungId) = {
    sql"""
        SELECT
          ${lieferung.result.*}, ${aboTyp.result.*}
        FROM
          ${lieferungMapping as lieferung}
          INNER JOIN ${abotypMapping as aboTyp} ON ${lieferung.abotypId} = ${aboTyp.id}
        INNER JOIN
		      (SELECT ${lieferung.abotypId} AS abotypId, MIN(${lieferung.datum}) AS MinDateTime
		       FROM ${lieferungMapping as lieferung}
		       INNER JOIN ${vertriebMapping as vertrieb}
		       ON ${lieferung.vertriebId} = ${vertrieb.id}
		       AND ${lieferung.lieferplanungId} IS NULL
		       AND ${vertrieb.id} NOT IN (
		         SELECT DISTINCT ${lieferung.vertriebId}
		         FROM ${lieferungMapping as lieferung}
		         WHERE ${lieferung.lieferplanungId} = ${id.id}
		       )
		       GROUP BY ${lieferung.abotypId}) groupedLieferung
		    ON ${lieferung.abotypId} = groupedLieferung.abotypId
		    AND ${lieferung.datum} = groupedLieferung.MinDateTime
      """.one(lieferungMapping(lieferung))
      .toOne(abotypMapping.opt(aboTyp))
      .map { (lieferung, abotyp) =>
        val emptyPosition = Seq.empty[Lieferposition]
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp, "lieferpositionen" -> emptyPosition, "lieferplanungBemerkungen" -> None)
      }.list
  }

  protected def getSammelbestellungDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(sammelbestellung.lieferplanungId, id)
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen map { b =>
          val p = positionen.filter(_.bestellungId == b.id)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).list
  }

  protected def getSammelbestellungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where.eq(sammelbestellung.lieferplanungId, id)
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getSammelbestellungenQuery(id: LieferungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(lieferplanungMapping as lieferplanung).on(sammelbestellung.lieferplanungId, lieferplanung.id)
        .join(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .where.eq(lieferung.id, id)
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getSammelbestellungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, sammelbestellung))
    }.map(sammelbestellungMapping(sammelbestellung)).list
  }

  protected def getBestellungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, bestellung))
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getSammelbestellungByProduzentLieferplanungDatumQuery(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .where.eq(sammelbestellung.produzentId, produzentId)
        .and.eq(sammelbestellung.lieferplanungId, lieferplanungId)
        .and.eq(sammelbestellung.datum, datum)
    }.map(sammelbestellungMapping(sammelbestellung)).single
  }

  protected def getBestellungenQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.sammelbestellungId, id)
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getBestellungQuery(id: SammelbestellungId, adminProzente: BigDecimal) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.sammelbestellungId, id).and.eq(bestellung.adminProzente, adminProzente)
    }.map(bestellungMapping(bestellung)).single
  }

  protected def getBestellpositionenQuery(id: BestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getBestellpositionenBySammelbestellungQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .join(bestellungMapping as bestellung).on(bestellung.id, bestellposition.bestellungId)
        .where.eq(bestellung.sammelbestellungId, id)
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getSammelbestellungDetailQuery(id: SammelbestellungId) = {
    withSQL {
      select
        .from(sammelbestellungMapping as sammelbestellung)
        .join(produzentMapping as produzent).on(sammelbestellung.produzentId, produzent.id)
        .join(bestellungMapping as bestellung).on(bestellung.sammelbestellungId, sammelbestellung.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(sammelbestellung.id, id)
    }.one(sammelbestellungMapping(sammelbestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellungMapping.opt(bestellung)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((sammelbestellung, produzenten, bestellungen, positionen) => {
        val bestellungenDetails = bestellungen map { b =>
          val p = positionen.filter(_.bestellungId == b.id)
          copyTo[Bestellung, BestellungDetail](b, "positionen" -> p)
        }
        copyTo[Sammelbestellung, SammelbestellungDetail](sammelbestellung, "produzent" -> produzenten.head, "bestellungen" -> bestellungenDetails)
      }).single
  }

  protected def getLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferantQuery(id: ProduzentId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.produzentId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getAboIdsQuery(lieferungId: LieferungId, korbStatus: KorbStatus) = {
    withSQL {
      select(korb.aboId)
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.eq(korb.status, korbStatus)
    }.map(res => AboId(res.long(1))).list
  }

  protected def getAboIdsQuery(lieferplanungId: LieferplanungId, korbStatus: KorbStatus) = {
    withSQL {
      select(korb.aboId)
        .from(korbMapping as korb)
        .leftJoin(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
        .and.eq(korb.status, korbStatus)
    }.map(res => AboId(res.long(1))).list
  }

  protected def getZusatzaboIdsQuery(lieferungId: LieferungId, korbStatus: KorbStatus) = {
    withSQL {
      select(zusatzAbo.hauptAboId)
        .from(zusatzAboMapping as zusatzAbo)
        .join(korbMapping as korb).on(korb.aboId, zusatzAbo.id)
        .where.eq(korb.lieferungId, lieferungId)
        .and.eq(korb.status, korbStatus)
    }.map(res => AboId(res.long(1))).list
  }

  protected def getBestellpositionByBestellungProduktQuery(bestellungId: BestellungId, produktId: ProduktId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, bestellungId)
        .and.eq(bestellposition.produktId, produktId)
    }.map(bestellpositionMapping(bestellposition)).single
  }

  protected def getLieferpositionenByLieferplanQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferplanAndProduzentQuery(id: LieferplanungId, produzentId: ProduzentId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, id).and.eq(lieferposition.produzentId, produzentId)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferungQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, id)
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getKorbQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.eq(korb.aboId, aboId).and.not.eq(korb.status, Geliefert)
    }.map(korbMapping(korb)).single
  }

  protected def getZusatzAboKorbQuery(hauptlieferungId: LieferungId, hauptAboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(zusatzAboMapping as zusatzAbo)
        .innerJoin(lieferungMapping as lieferung)
        .innerJoin(lieferungMapping as lieferungJoin).on(lieferungJoin.datum, lieferung.datum)
        .where.eq(korb.aboId, zusatzAbo.id)
        .and.eq(korb.lieferungId, lieferungJoin.id)
        .and.eq(zusatzAbo.hauptAboId, hauptAboId)
        .and.eq(lieferung.id, hauptlieferungId)
    }.map(korbMapping(korb)).list
  }

  protected def getNichtGelieferteKoerbeQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, lieferungId)
        .and.not.eq(korb.status, Geliefert)
    }.map(korbMapping(korb)).list
  }

  protected def getKoerbeQuery(aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .where.eq(korb.aboId, aboId)
    }.one(korbMapping(korb))
      .toMany(
        rs => lieferungMapping.opt(lieferung)(rs)
      )
      .map((korb, lieferung) => {
        copyTo[Korb, KorbLieferung](korb, "lieferung" -> lieferung.head)
      }).list
  }

  protected def getKoerbeQuery(datum: DateTime, vertriebsartId: VertriebsartId, status: KorbStatus) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.id, korb.aboId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.id, korb.aboId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.id, korb.aboId)
        .where.eq(lieferung.datum, datum).and.eq(korb.status, status).and.
        withRoundBracket(_.eq(depotlieferungAbo.vertriebsartId, vertriebsartId).
          or.eq(heimlieferungAbo.vertriebsartId, vertriebsartId).
          or.eq(postlieferungAbo.vertriebsartId, vertriebsartId))
    }.one(korbMapping(korb))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs)
      )
      .map { (korb, _, _, _, _) => korb }
      .list
  }

  protected def getKoerbeQuery(datum: DateTime, vertriebsartIds: List[VertriebsartId], status: KorbStatus) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.id, korb.aboId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.id, korb.aboId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.id, korb.aboId)
        .where.eq(lieferung.datum, datum).and.eq(korb.status, status).and.
        withRoundBracket(_.in(depotlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(heimlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(postlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)))
    }.one(korbMapping(korb))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs)
      )
      .map { (korb, _, _, _, _) => korb }
      .list
  }

  protected def getKoerbeQuery(auslieferungId: AuslieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.auslieferungId, auslieferungId)
    }.map(korbMapping(korb))
      .list
  }

  protected def getKoerbeNichtAusgeliefertByAboQuery(aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.aboId, aboId).and.eq(korb.status, WirdGeliefert)
    }.map(korbMapping(korb))
      .list
  }

  protected def getDepotAuslieferungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depotAuslieferung))
    }.map(depotAuslieferungMapping(depotAuslieferung)).list
  }

  protected def getTourAuslieferungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, tourAuslieferung))
    }.map(tourAuslieferungMapping(tourAuslieferung)).list
  }

  protected def getPostAuslieferungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, postAuslieferung))
    }.map(postAuslieferungMapping(postAuslieferung)).list
  }

  protected def getDepotAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, depotAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(depotAuslieferungMapping(depotAuslieferung)).list
  }

  protected def getTourAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(tourAuslieferungMapping(tourAuslieferung)).list
  }

  protected def getPostAuslieferungenQuery(lieferplanungId: LieferplanungId) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .join(korbMapping as korb).on(korb.auslieferungId, postAuslieferung.id)
        .join(lieferungMapping as lieferung).on(korb.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, lieferplanungId)
    }.map(postAuslieferungMapping(postAuslieferung)).list
  }

  protected def getDepotAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, abos, abotypen, kunden, _, zusatzAbos) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)

      copyTo[DepotAuslieferung, DepotAuslieferungDetail](auslieferung, "depot" -> depot, "koerbe" -> korbDetails)
    }
  }

  protected def getDepotAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, abos, abotypen, kunden, personen, zusatzAbos) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden, personen, zusatzAbos).sortBy(_.abotyp.name)

      val depotReport = copyTo[Depot, DepotReport](depot)
      copyTo[DepotAuslieferung, DepotAuslieferungReport](auslieferung, "depot" -> depotReport, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getDepotAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (DepotAuslieferung, Depot, Seq[Korb], Seq[DepotlieferungAbo], Seq[Abotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo]) => A) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(depotMapping as depot).on(depotAuslieferung.depotId, depot.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, depotAuslieferung.id)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(korb.aboId, depotlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(depotlieferungAbo.id, zusatzAbo.hauptAboId)
        .where.eq(depotAuslieferung.id, auslieferungId)
    }.one(depotAuslieferungMapping(depotAuslieferung))
      .toManies(
        rs => depotMapping.opt(depot)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((auslieferung, depots, koerbe, abos, abotypen, kunden, personen, zusatzAbos) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, depots.head, koerbe, abos, abotypen, kunden, personenDetails, zusatzAbos)
      }).single

  }

  protected def getTourAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, abos, abotypen, kunden, _, zusatzAbos) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)

      copyTo[TourAuslieferung, TourAuslieferungDetail](auslieferung, "tour" -> tour, "koerbe" -> korbDetails)
    }
  }

  protected def getTourAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, abos, abotypen, kunden, personen, zusatzAbos) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden, personen, zusatzAbos).sortBy(_.abotyp.name)

      copyTo[TourAuslieferung, TourAuslieferungReport](auslieferung, "tour" -> tour, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getTourAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (TourAuslieferung, Tour, Seq[Korb], Seq[HeimlieferungAbo], Seq[Abotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo]) => A) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(tourMapping as tour).on(tourAuslieferung.tourId, tour.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(korb.aboId, heimlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(heimlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(heimlieferungAbo.id, zusatzAbo.hauptAboId)
        .where.eq(tourAuslieferung.id, auslieferungId)
        .orderBy(korb.sort)
    }.one(tourAuslieferungMapping(tourAuslieferung))
      .toManies(
        rs => tourMapping.opt(tour)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((auslieferung, tour, koerbe, abos, abotypen, kunden, personen, zusatzAbos) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, tour.head, koerbe, abos, abotypen, kunden, personenDetails, zusatzAbos)
      }).single

  }

  protected def getPostAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, abos, abotypen, kunden, _, zusatzAbos) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden, zusatzAbos)

      copyTo[PostAuslieferung, PostAuslieferungDetail](auslieferung, "koerbe" -> korbDetails)
    }
  }

  protected def getPostAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, abos, abotypen, kunden, personen, zusatzAbos) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden, personen, zusatzAbos).sortBy(_.abotyp.name)

      copyTo[PostAuslieferung, PostAuslieferungReport](auslieferung, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getPostAuslieferungQuery[A](auslieferungId: AuslieferungId)(f: (PostAuslieferung, Seq[Korb], Seq[PostlieferungAbo], Seq[Abotyp], Seq[Kunde], Seq[PersonDetail], Seq[ZusatzAbo]) => A) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, postAuslieferung.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(korb.aboId, postlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(postlieferungAbo.kundeId, kunde.id)
        .leftJoin(personMapping as person).on(kunde.id, person.kundeId)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(postlieferungAbo.id, zusatzAbo.hauptAboId)
        .where.eq(postAuslieferung.id, auslieferungId)
    }.one(postAuslieferungMapping(postAuslieferung))
      .toManies(
        rs => korbMapping.opt(korb)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs),
        rs => personMapping.opt(person)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map((auslieferung, koerbe, abos, abotypen, kunden, personen, zusatzAbos) => {
        val personenDetails = personen map (p => copyTo[Person, PersonDetail](p))
        f(auslieferung, koerbe, abos, abotypen, kunden, personenDetails, zusatzAbos)
      }).single
  }

  private def getKorbDetails(koerbe: Seq[Korb], abos: Seq[Abo], abotypen: Seq[Abotyp], kunden: Seq[Kunde], zusatzAbos: Seq[ZusatzAbo]): Seq[KorbDetail] = {
    koerbe.map { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
        zs = (zusatzAbos filter (_.hauptAboId == korbAbo.id))
        zusatzKoerbe = koerbe filter (k => zs.contains(k.aboId)) map (zk => copyTo[Korb, ZusatzKorbDetail](zk, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde, "zusatzKoerbe" -> Nil))
      } yield copyTo[Korb, KorbDetail](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde, "zusatzKoerbe" -> zusatzKoerbe)
    }.flatten
  }

  private def getKorbReports(koerbe: Seq[Korb], abos: Seq[Abo], abotypen: Seq[Abotyp], kunden: Seq[Kunde], personen: Seq[PersonDetail], zusatzAbos: Seq[ZusatzAbo]): Seq[KorbReport] = {
    koerbe.map { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
      } yield {
        val ansprechpersonen = personen.filter(_.kundeId == kunde.id)
        val zusatzAbosString = (zusatzAbos filter (_.hauptAboId == korbAbo.id) map (_.abotypName)).mkString(", ")
        val kundeReport = copyTo[Kunde, KundeReport](kunde, "personen" -> ansprechpersonen)
        copyTo[Korb, KorbReport](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kundeReport, "zusatzAbosString" -> zusatzAbosString)
      }
    }.flatten
  }

  protected def getDepotAuslieferungQuery(depotId: DepotId, datum: DateTime) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .where.eq(depotAuslieferung.depotId, depotId).and.eq(depotAuslieferung.datum, datum)
    }.map(depotAuslieferungMapping(depotAuslieferung)).single
  }

  protected def getTourAuslieferungQuery(tourId: TourId, datum: DateTime) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .where.eq(tourAuslieferung.tourId, tourId).and.eq(tourAuslieferung.datum, datum)
    }.map(tourAuslieferungMapping(tourAuslieferung)).single
  }

  protected def getPostAuslieferungQuery(datum: DateTime) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .where.eq(postAuslieferung.datum, datum)
    }.map(postAuslieferungMapping(postAuslieferung)).single
  }

  protected def getVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .where.eq(vertrieb.id, vertriebId)
    }.map(vertriebMapping(vertrieb)).single
  }

  protected def getVertriebeQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .leftJoin(depotlieferungMapping as depotlieferung).on(depotlieferung.vertriebId, vertrieb.id)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .leftJoin(heimlieferungMapping as heimlieferung).on(heimlieferung.vertriebId, vertrieb.id)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .leftJoin(postlieferungMapping as postlieferung).on(postlieferung.vertriebId, vertrieb.id)
        .where.eq(vertrieb.abotypId, abotypId)
    }.one(vertriebMapping(vertrieb))
      .toManies(
        rs => postlieferungMapping.opt(postlieferung)(rs),
        rs => heimlieferungMapping.opt(heimlieferung)(rs),
        rs => depotlieferungMapping.opt(depotlieferung)(rs),
        rs => depotMapping.opt(depot)(rs),
        rs => tourMapping.opt(tour)(rs)
      )
      .map({ (vertrieb, pl, hls, dls, depots, touren) =>
        val dl = dls.map { lieferung =>
          depots.find(_.id == lieferung.depotId).headOption map { depot =>
            val summary = copyTo[Depot, DepotSummary](depot)
            copyTo[Depotlieferung, DepotlieferungDetail](lieferung, "depot" -> summary)
          }
        }.flatten
        val hl = hls.map { lieferung =>
          touren.find(_.id == lieferung.tourId).headOption map { tour =>
            copyTo[Heimlieferung, HeimlieferungDetail](lieferung, "tour" -> tour)
          }
        }.flatten

        copyTo[Vertrieb, VertriebVertriebsarten](vertrieb, "depotlieferungen" -> dl, "heimlieferungen" -> hl, "postlieferungen" -> pl)
      }).list
  }

  protected def getProjektVorlagenQuery() = {
    withSQL {
      select
        .from(projektVorlageMapping as projektVorlage)
        .orderBy(projektVorlage.name)
    }.map(projektVorlageMapping(projektVorlage)).list
  }

  protected def getEinladungQuery(token: String) = {
    withSQL {
      select
        .from(einladungMapping as einladung)
        .where.eq(einladung.uid, token)
    }.map(einladungMapping(einladung)).single
  }

  protected def getSingleDepotlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.id, id)
    }.map(depotlieferungAboMapping(depotlieferungAbo)).single
  }

  protected def getSingleHeimlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.id, id)
    }.map(heimlieferungAboMapping(heimlieferungAbo)).single
  }

  protected def getSinglePostlieferungAboQuery(id: AboId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.id, id)
    }.map(postlieferungAboMapping(postlieferungAbo)).single
  }

  protected def getSingleZusatzAboQuery(id: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.id, id)
    }.map(zusatzAboMapping(zusatzAbo)).single
  }

  protected def getZusatzAbosQuery(hauptaboId: AboId) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.hauptAboId, hauptaboId)
    }.map(zusatzAboMapping(zusatzAbo)).list

  }

  // MODIFY and DELETE Queries

  protected def deleteLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(lieferpositionMapping as lieferpositionShort)
        .where.eq(lieferpositionShort.lieferungId, id)
    }
  }

  protected def deleteKoerbeQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(korbMapping as korbShort)
        .where.eq(korbShort.lieferungId, id)
    }
  }

  protected def deleteZusatzAbosQuery(hauptAboId: AboId) = {
    withSQL {
      delete
        .from(zusatzAboMapping as zusatzAbo)
        .where.eq(zusatzAbo.hauptAboId, hauptAboId)
    }
  }

  protected def getAktivierteAbosQuery = {
    val today = LocalDate.now.toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, today)
        .and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.ge(depotlieferungAbo.ende, today) }
        .and.eq(depotlieferungAbo.aktiv, false)
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, today)
          .and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.ge(heimlieferungAbo.ende, today) }
          .and.eq(heimlieferungAbo.aktiv, false)).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, today)
            .and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.ge(postlieferungAbo.ende, today) }
            .and.eq(postlieferungAbo.aktiv, false)
        ).union(select(zusatzAbo.id).from(zusatzAboMapping as zusatzAbo)
            .where.le(zusatzAbo.start, today)
            .and.withRoundBracket { _.isNull(zusatzAbo.ende).or.ge(zusatzAbo.ende, today) }
            .and.eq(zusatzAbo.aktiv, false))
    }.map(res => AboId(res.long(1))).list
  }

  protected def getDeaktivierteAbosQuery = {
    val yesterday = LocalDate.now.minusDays(1).toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, yesterday)
        .and.withRoundBracket { _.isNotNull(depotlieferungAbo.ende).and.le(depotlieferungAbo.ende, yesterday) }
        .and.eq(depotlieferungAbo.aktiv, true)
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, yesterday)
          .and.withRoundBracket { _.isNotNull(heimlieferungAbo.ende).and.le(heimlieferungAbo.ende, yesterday) }
          .and.eq(heimlieferungAbo.aktiv, true)).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, yesterday)
            .and.withRoundBracket { _.isNotNull(postlieferungAbo.ende).and.le(postlieferungAbo.ende, yesterday) }
            .and.eq(postlieferungAbo.aktiv, true)
        ).union(
            select(zusatzAbo.id).from(zusatzAboMapping as zusatzAbo)
              .where.le(zusatzAbo.start, yesterday)
              .and.withRoundBracket { _.isNotNull(zusatzAbo.ende).and.le(zusatzAbo.ende, yesterday) }
              .and.eq(zusatzAbo.aktiv, true)
          )
    }.map(res => AboId(res.long(1))).list
  }

  protected def getLieferungenOffenByAbotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, abotypId)
        .and.eq(lieferung.status, Offen)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLastClosedLieferplanungenDetailQuery = {
    withSQL {
      select
        .from(lieferplanungMapping as lieferplanung)
        .leftJoin(lieferungMapping as lieferung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.withRoundBracket { _.eq(lieferplanung.status, Abgeschlossen).or.eq(lieferplanung.status, Verrechnet) }
        .orderBy(lieferplanung.id).desc
    }
      .one(lieferplanungMapping(lieferplanung))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map((lieferplanung, lieferungen, lieferpositionen) => {
        val lieferungenDetails = lieferungen map { l =>
          val p = lieferpositionen.filter(_.lieferungId == l.id).map(p => copyTo[Lieferposition, LieferpositionOpen](p)).toSeq
          copyTo[Lieferung, LieferungOpenDetail](l, "lieferpositionen" -> p)
        }
        copyTo[Lieferplanung, LieferplanungOpenDetail](lieferplanung, "lieferungen" -> lieferungenDetails)
      }).list
  }

}
