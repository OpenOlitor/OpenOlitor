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

import ch.openolitor.core.models._
import scalikejdbc._
import sqls.{ distinct, count }
import ch.openolitor.core.db._
import ch.openolitor.core.repositories._
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import org.joda.time.DateTime
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.FilterExpr
import org.joda.time.LocalDate

trait StammdatenRepositoryQueries extends LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferungJoin = lieferungMapping.syntax("lieferungJ")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")
  lazy val bestellung = bestellungMapping.syntax("bestellung")
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
  lazy val produkt = produktMapping.syntax("produkt")
  lazy val produktekategorie = produktekategorieMapping.syntax("produktekategorie")
  lazy val produzent = produzentMapping.syntax("produzent")
  lazy val projekt = projektMapping.syntax("projekt")
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

  protected def getKundenQuery = {
    withSQL {
      select
        .from(kundeMapping as kunde)
        .orderBy(kunde.bezeichnung)
    }.map(kundeMapping(kunde)).list
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
        .where.eq(kunde.id, parameter(id))
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
        .where.eq(kunde.id, parameter(kundeId))
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

  protected def getPersonenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, parameter(kundeId))
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
        .where.eq(aboTyp.id, parameter(id))
    }.map(abotypMapping(aboTyp)).single
  }

  protected def getDepotlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.abotypId, parameter(abotypId))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.abotypId, parameter(abotypId))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.abotypId, parameter(abotypId))
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, parameter(vertriebId))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, parameter(vertriebId))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosByVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, parameter(vertriebId))
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }

  protected def getDepotlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .leftJoin(depotMapping as depot).on(depotlieferung.depotId, depot.id)
        .where.eq(depotlieferung.vertriebId, parameter(vertriebId))
    }.one(depotlieferungMapping(depotlieferung)).toOne(depotMapping.opt(depot)).map { (vertriebsart, depot) =>
      val depotSummary = DepotSummary(depot.head.id, depot.head.name, depot.head.kurzzeichen)
      copyTo[Depotlieferung, DepotlieferungDetail](vertriebsart, "depot" -> depotSummary)
    }.list
  }

  protected def getDepotlieferungQuery(depotId: DepotId) = {
    withSQL {
      select
        .from(depotlieferungMapping as depotlieferung)
        .where.eq(depotlieferung.depotId, parameter(depotId))
    }.map(depotlieferungMapping(depotlieferung)).list
  }

  protected def getHeimlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .leftJoin(tourMapping as tour).on(heimlieferung.tourId, tour.id)
        .where.eq(heimlieferung.vertriebId, parameter(vertriebId))
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.list
  }

  protected def getHeimlieferungQuery(tourId: TourId) = {
    withSQL {
      select
        .from(heimlieferungMapping as heimlieferung)
        .where.eq(heimlieferung.tourId, parameter(tourId))
    }.map(heimlieferungMapping(heimlieferung)).list
  }

  protected def getPostlieferungQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.vertriebId, parameter(vertriebId))
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
        .where.eq(depotlieferung.id, parameter(vertriebsartId))
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
        .where.eq(heimlieferung.id, parameter(vertriebsartId))
    }.one(heimlieferungMapping(heimlieferung)).toOne(tourMapping.opt(tour)).map { (vertriebsart, tour) =>
      copyTo[Heimlieferung, HeimlieferungDetail](vertriebsart, "tour" -> tour.get)
    }.single
  }

  protected def getPostlieferungQuery(vertriebsartId: VertriebsartId) = {
    withSQL {
      select
        .from(postlieferungMapping as postlieferung)
        .where.eq(postlieferung.id, parameter(vertriebsartId))
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
        .where.eq(vertrieb.abotypId, parameter(abotypId)).and.eq(lieferung.vertriebId, parameter(vertriebId)).and.isNull(lieferung.lieferplanungId)
        .orderBy(lieferung.datum)
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getUngeplanteLieferungenQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, parameter(abotypId)).and.isNull(lieferung.lieferplanungId)
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
        .where.eq(depot.id, parameter(id))
    }.map(depotMapping(depot)).single
  }

  protected def getDepotDetailReportQuery(id: DepotId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(depotMapping as depot)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.depotId, depot.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .where.eq(depot.id, parameter(id))
    }
      .one(depotMapping(depot))
      .toManies(
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => kundeMapping.opt(kunde)(rs)
      )
      .map((depot, abos, kunden) => {
        val abosReport = abos.map { abo =>
          kunden.filter(_.id == abo.kundeId).headOption map { kunde =>
            val kundeReport = copyTo[Kunde, KundeReport](kunde)
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
        .where.eq(depotlieferungAbo.depotId, parameter(id))
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
        .leftJoin(lieferungMapping as lieferung).on(depotlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(depotlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(depotlieferungAbo.id, parameter(id))
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
        ))
    }
      .one(depotlieferungAboMapping(depotlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
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
        .leftJoin(lieferungMapping as lieferung).on(heimlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(heimlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(heimlieferungAbo.id, parameter(id))
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
        ))
    }.one(heimlieferungAboMapping(heimlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
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
        .leftJoin(lieferungMapping as lieferung).on(postlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(postlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(postlieferungAbo.id, parameter(id))
        .and(sqls.toAndConditionOpt(
          ausstehend map (_ => sqls.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
        ))
    }.one(postlieferungAboMapping(postlieferungAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
        val sortedAbw = abw.sortBy(_.datum)
        val sortedLieferungen = lieferungen.sortBy(_.datum)
        copyTo[PostlieferungAbo, PostlieferungAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> aboTyp.headOption, "vertrieb" -> vertriebe.headOption)
      }).single
  }

  protected def countAbwesendQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.lieferungId, parameter(lieferungId)).and.eq(abwesenheit.aboId, parameter(aboId))
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def countAbwesendQuery(aboId: AboId, datum: LocalDate) = {
    withSQL {
      select(count(distinct(abwesenheit.id)))
        .from(abwesenheitMapping as abwesenheit)
        .where.eq(abwesenheit.datum, parameter(datum)).and.eq(abwesenheit.aboId, parameter(aboId))
        .limit(1)
    }.map(_.int(1)).single
  }

  protected def getAktiveDepotlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.vertriebId, parameter(vertriebId))
        .and.le(depotlieferungAbo.start, parameter(lieferdatum))
        .and.withRoundBracket { _.isNull(depotlieferungAbo.ende).or.ge(depotlieferungAbo.ende, parameter(lieferdatum)) }
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getAktiveHeimlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.vertriebId, parameter(vertriebId))
        .and.le(heimlieferungAbo.start, parameter(lieferdatum))
        .and.withRoundBracket { _.isNull(heimlieferungAbo.ende).or.ge(heimlieferungAbo.ende, parameter(lieferdatum)) }
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getAktivePostlieferungAbosQuery(vertriebId: VertriebId, lieferdatum: DateTime) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.vertriebId, parameter(vertriebId))
        .and.le(postlieferungAbo.start, parameter(lieferdatum))
        .and.withRoundBracket { _.isNull(postlieferungAbo.ende).or.ge(postlieferungAbo.ende, parameter(lieferdatum)) }
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
        .where.eq(pendenz.kundeId, parameter(id))
    }.map(pendenzMapping(pendenz)).list
  }

  protected def getPendenzDetailQuery(id: PendenzId) = {
    withSQL {
      select
        .from(pendenzMapping as pendenz)
        .where.eq(pendenz.id, parameter(id))
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
        .where.eq(produzent.id, parameter(id))
    }.map(produzentMapping(produzent)).single
  }

  protected def getProduzentDetailReportQuery(id: ProduzentId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.id, parameter(id))
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
        .where.eq(tour.id, parameter(id))
        .orderBy(tourlieferung.sort)
    }.one(tourMapping(tour))
      .toMany(
        rs => tourlieferungMapping.opt(tourlieferung)(rs)
      )
      .map({ (tour, tourlieferungen) =>
        copyTo[Tour, TourDetail](tour, "tourlieferungen" -> tourlieferungen)
      }).single
  }

  protected def getTourlieferungenQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, parameter(tourId))
        .orderBy(tourlieferung.sort)
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByKundeQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.kundeId, parameter(kundeId))
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getTourlieferungenByTourQuery(tourId: TourId) = {
    withSQL {
      select
        .from(tourlieferungMapping as tourlieferung)
        .where.eq(tourlieferung.tourId, parameter(tourId))
    }.map(tourlieferungMapping(tourlieferung)).list
  }

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }

  protected def getProduktProduzentenQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduzentMapping as produktProduzent)
        .where.eq(produktProduzent.produktId, parameter(id))
    }.map(produktProduzentMapping(produktProduzent)).list
  }

  protected def getProduktProduktekategorienQuery(id: ProduktId) = {
    withSQL {
      select
        .from(produktProduktekategorieMapping as produktProduktekategorie)
        .where.eq(produktProduktekategorie.produktId, parameter(id))
    }.map(produktProduktekategorieMapping(produktProduktekategorie)).list
  }

  protected def getProduzentDetailByKurzzeichenQuery(kurzzeichen: String) = {
    withSQL {
      select
        .from(produzentMapping as produzent)
        .where.eq(produzent.kurzzeichen, parameter(kurzzeichen))
    }.map(produzentMapping(produzent)).single
  }

  protected def getProduktekategorieByBezeichnungQuery(bezeichnung: String) = {
    withSQL {
      select
        .from(produktekategorieMapping as produktekategorie)
        .where.eq(produktekategorie.beschreibung, parameter(bezeichnung))
    }.map(produktekategorieMapping(produktekategorie)).single
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
        .where.eq(lieferplanung.id, parameter(id))
    }.map(lieferplanungMapping(lieferplanung)).single
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
        .where.eq(lieferung.abotypId, parameter(abotypId)).and.not.eq(lieferung.status, parameter(Ungeplant))
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getLieferungenDetailsQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .join(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, parameter(id))
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => lieferplanungMapping.opt(lieferplanung)(rs)
      )
      .map { (lieferung, abotyp, positionen, lieferplanung) =>
        val bemerkung = lieferplanung match {
          case Nil => None
          case x => x.head.bemerkungen
        }
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> positionen, "lieferplanungBemerkungen" -> bemerkung)
      }.list
  }

  protected def getLieferungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.lieferplanungId, parameter(id))
        .orderBy(lieferung.datum).desc
    }.map(lieferungMapping(lieferung)).list
  }

  protected def getLieferungenQuery(id: VertriebId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, parameter(id))
    }.map(lieferungMapping(lieferung)).list
  }

  protected def sumPreisTotalGeplanteLieferungenVorherQuery(vertriebId: VertriebId, datum: DateTime) = {
    sql"""
      select
        sum(${lieferung.preisTotal})
      from
        ${lieferungMapping as lieferung}
      where
        ${lieferung.vertriebId} = ${vertriebId.id}
        and ${lieferung.lieferplanungId} IS NOT NULL
        and ${lieferung.datum} < ${datum}
      """
      .map(x => BigDecimal(x.bigDecimal(1))).single
  }

  protected def getGeplanteLieferungVorherQuery(vertriebId: VertriebId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, parameter(vertriebId))
        .and.not.isNull(lieferung.lieferplanungId)
        .and.lt(lieferung.datum, parameter(datum))
        .orderBy(lieferung.datum).desc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def getGeplanteLieferungNachherQuery(vertriebId: VertriebId, datum: DateTime) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.vertriebId, parameter(vertriebId))
        .and.not.isNull(lieferung.lieferplanungId)
        .and.gt(lieferung.datum, parameter(datum))
        .orderBy(lieferung.datum).asc
        .limit(1)
    }.map(lieferungMapping(lieferung)).single
  }

  protected def countEarlierLieferungOffenQuery(id: LieferplanungId) = {
    sql"""
      select
        count(${lieferung.datum})
      from
        ${lieferungMapping as lieferung}
      left outer join
        (
         select
          ${lieferung.vertriebId} as vertriebId, min(${lieferung.datum}) as mindat
         from
          ${lieferungMapping as lieferung}
         where
          ${lieferung.status} = 'Offen'
         group by
          ${lieferung.vertriebId}
        )
        as l1 on ${lieferung.vertriebId} = l1.vertriebId
      where
        ${lieferung.lieferplanungId} = ${id.id}
        and ${lieferung.status} = 'Offen'
        and ${lieferung.datum} > mindat
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

  protected def getBestellungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.lieferplanungId, parameter(id))
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getBestellungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, bestellung))
    }.map(bestellungMapping(bestellung)).list
  }

  protected def getBestellungByProduzentLieferplanungDatumQuery(produzentId: ProduzentId, lieferplanungId: LieferplanungId, datum: DateTime) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.produzentId, parameter(produzentId))
        .and.eq(bestellung.lieferplanungId, parameter(lieferplanungId))
        .and.eq(bestellung.datum, parameter(datum))
    }.map(bestellungMapping(bestellung)).single
  }

  protected def getBestellpositionenQuery(id: BestellungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, parameter(id))
    }.map(bestellpositionMapping(bestellposition)).list
  }

  protected def getBestellungDetailQuery(id: BestellungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .join(produzentMapping as produzent).on(bestellung.produzentId, produzent.id)
        .leftJoin(bestellpositionMapping as bestellposition).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(bestellposition.bestellungId, parameter(id))
    }.one(bestellungMapping(bestellung))
      .toManies(
        rs => produzentMapping.opt(produzent)(rs),
        rs => bestellpositionMapping.opt(bestellposition)(rs)
      )
      .map((bestellung, produzenten, positionen) => {
        copyTo[Bestellung, BestellungDetail](bestellung, "positionen" -> positionen, "produzent" -> produzenten.head)
      }).single
  }

  protected def getLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, parameter(id))
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferantQuery(id: ProduzentId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.produzentId, parameter(id))
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getAboIdsQuery(lieferungId: LieferungId, korbStatus: KorbStatus) = {
    withSQL {
      select(korb.aboId)
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, parameter(lieferungId))
        .and.eq(korb.status, parameter(korbStatus))
    }.map(res => AboId(res.long(1))).list
  }

  protected def getBestellpositionByBestellungProduktQuery(bestellungId: BestellungId, produktId: ProduktId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .where.eq(bestellposition.bestellungId, parameter(bestellungId))
        .and.eq(bestellposition.produktId, parameter(produktId))
    }.map(bestellpositionMapping(bestellposition)).single
  }

  protected def getLieferpositionenByLieferplanQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, parameter(id))
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferplanAndProduzentQuery(id: LieferplanungId, produzentId: ProduzentId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .leftJoin(lieferungMapping as lieferung).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, parameter(id)).and.eq(lieferposition.produzentId, parameter(produzentId))
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getLieferpositionenByLieferungQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferpositionMapping as lieferposition)
        .where.eq(lieferposition.lieferungId, parameter(id))
    }.map(lieferpositionMapping(lieferposition)).list
  }

  protected def getKorbQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, parameter(lieferungId))
        .and.eq(korb.aboId, parameter(aboId)).and.not.eq(korb.status, parameter(Geliefert))
    }.map(korbMapping(korb)).single
  }

  protected def getKoerbeQuery(datum: DateTime, vertriebsartId: VertriebsartId, status: KorbStatus) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(depotlieferungAbo.id, korb.aboId)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(heimlieferungAbo.id, korb.aboId)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(postlieferungAbo.id, korb.aboId)
        .where.eq(lieferung.datum, parameter(datum)).and.eq(korb.status, parameter(status)).and.
        withRoundBracket(_.eq(depotlieferungAbo.vertriebsartId, parameter(vertriebsartId)).
          or.eq(heimlieferungAbo.vertriebsartId, parameter(vertriebsartId)).
          or.eq(postlieferungAbo.vertriebsartId, parameter(vertriebsartId)))
    }.one(korbMapping(korb))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => depotlieferungMapping.opt(depotlieferung)(rs),
        rs => heimlieferungMapping.opt(heimlieferung)(rs),
        rs => postlieferungMapping.opt(postlieferung)(rs)
      )
      .map { (korb, _, _, _, _, _) => korb }
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
        .where.eq(lieferung.datum, parameter(datum)).and.eq(korb.status, parameter(status)).and.
        withRoundBracket(_.in(depotlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(heimlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)).
          or.in(postlieferungAbo.vertriebsartId, vertriebsartIds.map(_.id)))
    }.one(korbMapping(korb))
      .toManies(
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs),
        rs => depotlieferungMapping.opt(depotlieferung)(rs),
        rs => heimlieferungMapping.opt(heimlieferung)(rs),
        rs => postlieferungMapping.opt(postlieferung)(rs)
      )
      .map { (korb, _, _, _, _, _) => korb }
      .list
  }

  protected def getKoerbeQuery(auslieferungId: AuslieferungId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.auslieferungId, parameter(auslieferungId))
    }.map(korbMapping(korb))
      .list
  }

  protected def getDepotAuslieferungenQuery = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
    }.map(depotAuslieferungMapping(depotAuslieferung)).list
  }

  protected def getTourAuslieferungenQuery = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
    }.map(tourAuslieferungMapping(tourAuslieferung)).list
  }

  protected def getPostAuslieferungenQuery = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
    }.map(postAuslieferungMapping(postAuslieferung)).list
  }

  protected def getDepotAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, abos, abotypen, kunden) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden)

      copyTo[DepotAuslieferung, DepotAuslieferungDetail](auslieferung, "depot" -> depot, "koerbe" -> korbDetails)
    }
  }

  protected def getDepotAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getDepotAuslieferungQuery(auslieferungId) { (auslieferung, depot, koerbe, abos, abotypen, kunden) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden)

      val depotReport = copyTo[Depot, DepotReport](depot)
      copyTo[DepotAuslieferung, DepotAuslieferungReport](auslieferung, "depot" -> depotReport, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getDepotAuslieferungQuery[A <: Auslieferung](auslieferungId: AuslieferungId)(f: (DepotAuslieferung, Depot, Seq[Korb], Seq[DepotlieferungAbo], Seq[Abotyp], Seq[Kunde]) => A) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .join(depotMapping as depot).on(depotAuslieferung.depotId, depot.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, depotAuslieferung.id)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(korb.aboId, depotlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(depotlieferungAbo.kundeId, kunde.id)
        .where.eq(depotAuslieferung.id, parameter(auslieferungId))
    }.one(depotAuslieferungMapping(depotAuslieferung))
      .toManies(
        rs => depotMapping.opt(depot)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs)
      )
      .map((auslieferung, depots, koerbe, abos, abotypen, kunden) => {
        f(auslieferung, depots.head, koerbe, abos, abotypen, kunden)
      }).single
  }

  protected def getTourAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, abos, abotypen, kunden) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden)

      copyTo[TourAuslieferung, TourAuslieferungDetail](auslieferung, "tour" -> tour, "koerbe" -> korbDetails)
    }
  }

  protected def getTourAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getTourAuslieferungQuery(auslieferungId) { (auslieferung, tour, koerbe, abos, abotypen, kunden) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden)

      copyTo[TourAuslieferung, TourAuslieferungReport](auslieferung, "tour" -> tour, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getTourAuslieferungQuery[A <: Auslieferung](auslieferungId: AuslieferungId)(f: (TourAuslieferung, Tour, Seq[Korb], Seq[HeimlieferungAbo], Seq[Abotyp], Seq[Kunde]) => A) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(tourMapping as tour).on(tourAuslieferung.tourId, tour.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(korb.aboId, heimlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(heimlieferungAbo.kundeId, kunde.id)
        .where.eq(tourAuslieferung.id, parameter(auslieferungId))
        .orderBy(korb.sort)
    }.one(tourAuslieferungMapping(tourAuslieferung))
      .toManies(
        rs => tourMapping.opt(tour)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs)
      )
      .map((auslieferung, tour, koerbe, abos, abotypen, kunden) => {
        f(auslieferung, tour.head, koerbe, abos, abotypen, kunden)
      }).single
  }

  protected def getPostAuslieferungDetailQuery(auslieferungId: AuslieferungId) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, abos, abotypen, kunden) =>
      val korbDetails = getKorbDetails(koerbe, abos, abotypen, kunden)

      copyTo[PostAuslieferung, PostAuslieferungDetail](auslieferung, "koerbe" -> korbDetails)
    }
  }

  protected def getPostAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    getPostAuslieferungQuery(auslieferungId) { (auslieferung, koerbe, abos, abotypen, kunden) =>
      val korbReports = getKorbReports(koerbe, abos, abotypen, kunden)

      copyTo[PostAuslieferung, PostAuslieferungReport](auslieferung, "koerbe" -> korbReports, "projekt" -> projekt)
    }
  }

  private def getPostAuslieferungQuery[A <: Auslieferung](auslieferungId: AuslieferungId)(f: (PostAuslieferung, Seq[Korb], Seq[PostlieferungAbo], Seq[Abotyp], Seq[Kunde]) => A) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, postAuslieferung.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(korb.aboId, postlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(postlieferungAbo.kundeId, kunde.id)
        .where.eq(postAuslieferung.id, parameter(auslieferungId))
    }.one(postAuslieferungMapping(postAuslieferung))
      .toManies(
        rs => korbMapping.opt(korb)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs)
      )
      .map((auslieferung, koerbe, abos, abotypen, kunden) => {
        f(auslieferung, koerbe, abos, abotypen, kunden)
      }).single
  }

  private def getKorbDetails(koerbe: Seq[Korb], abos: Seq[Abo], abotypen: Seq[Abotyp], kunden: Seq[Kunde]): Seq[KorbDetail] = {
    koerbe.map { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
      } yield copyTo[Korb, KorbDetail](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde)
    }.flatten
  }

  private def getKorbReports(koerbe: Seq[Korb], abos: Seq[Abo], abotypen: Seq[Abotyp], kunden: Seq[Kunde]): Seq[KorbReport] = {
    koerbe.map { korb =>
      for {
        korbAbo <- abos.filter(_.id == korb.aboId).headOption
        abotyp <- abotypen.filter(_.id == korbAbo.abotypId).headOption
        kunde <- kunden.filter(_.id == korbAbo.kundeId).headOption
      } yield {
        val kundeReport = copyTo[Kunde, KundeReport](kunde)
        copyTo[Korb, KorbReport](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kundeReport)
      }
    }.flatten
  }

  protected def getDepotAuslieferungQuery(depotId: DepotId, datum: DateTime) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .where.eq(depotAuslieferung.depotId, parameter(depotId)).and.eq(depotAuslieferung.datum, parameter(datum))
    }.map(depotAuslieferungMapping(depotAuslieferung)).single
  }

  protected def getTourAuslieferungQuery(tourId: TourId, datum: DateTime) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .where.eq(tourAuslieferung.tourId, parameter(tourId)).and.eq(tourAuslieferung.datum, parameter(datum))
    }.map(tourAuslieferungMapping(tourAuslieferung)).single
  }

  protected def getPostAuslieferungQuery(datum: DateTime) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .where.eq(postAuslieferung.datum, parameter(datum))
    }.map(postAuslieferungMapping(postAuslieferung)).single
  }

  protected def getVertriebQuery(vertriebId: VertriebId) = {
    withSQL {
      select
        .from(vertriebMapping as vertrieb)
        .where.eq(vertrieb.id, parameter(vertriebId))
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
        .where.eq(vertrieb.abotypId, parameter(abotypId))
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
        .where.eq(einladung.uid, parameter(token))
    }.map(einladungMapping(einladung)).single
  }

  protected def getSingleDepotlieferungAbo(id: AboId) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.eq(depotlieferungAbo.id, parameter(id))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).single
  }

  protected def getSingleHeimlieferungAbo(id: AboId) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.id, parameter(id))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).single
  }

  protected def getSinglePostlieferungAbo(id: AboId) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.id, parameter(id))
    }.map(postlieferungAboMapping(postlieferungAbo)).single
  }

  // MODIFY and DELETE Queries

  protected def deleteLieferpositionenQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(lieferpositionMapping as lieferpositionShort)
        .where.eq(lieferpositionShort.lieferungId, parameter(id))
    }
  }

  protected def deleteKoerbeQuery(id: LieferungId) = {
    withSQL {
      delete
        .from(korbMapping as korbShort)
        .where.eq(korbShort.lieferungId, parameter(id))
    }
  }

  protected def getAktivierteAbosQuery = {
    val today = LocalDate.now.toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, parameter(today))
        .and.ge(depotlieferungAbo.ende, parameter(today))
        .and.eq(depotlieferungAbo.aktiv, parameter(false))
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, parameter(today))
          .and.ge(heimlieferungAbo.ende, parameter(today))
          .and.eq(heimlieferungAbo.aktiv, parameter(false))).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, parameter(today))
            .and.ge(postlieferungAbo.ende, parameter(today))
            .and.eq(postlieferungAbo.aktiv, parameter(false))
        )
    }.map(res => AboId(res.long(1))).list
  }

  protected def getDeaktivierteAbosQuery = {
    val yesterday = LocalDate.now.minusDays(1).toDateTimeAtStartOfDay

    withSQL {
      select(depotlieferungAbo.id)
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where.le(depotlieferungAbo.start, parameter(yesterday))
        .and.le(depotlieferungAbo.ende, parameter(yesterday))
        .and.eq(depotlieferungAbo.aktiv, parameter(true))
        .union(select(heimlieferungAbo.id).from(heimlieferungAboMapping as heimlieferungAbo)
          .where.le(heimlieferungAbo.start, parameter(yesterday))
          .and.le(heimlieferungAbo.ende, parameter(yesterday))
          .and.eq(heimlieferungAbo.aktiv, parameter(true))).union(
          select(postlieferungAbo.id).from(postlieferungAboMapping as postlieferungAbo)
            .where.le(postlieferungAbo.start, parameter(yesterday))
            .and.le(postlieferungAbo.ende, parameter(yesterday))
            .and.eq(postlieferungAbo.aktiv, parameter(true))
        )
    }.map(res => AboId(res.long(1))).list
  }

  protected def getLieferungenOffenByAbotypQuery(abotypId: AbotypId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.abotypId, parameter(abotypId))
        .and.eq(lieferung.status, parameter(Offen))
    }.map(lieferungMapping(lieferung)).list
  }
}
