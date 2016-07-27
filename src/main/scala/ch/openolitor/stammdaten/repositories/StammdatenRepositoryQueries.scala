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

trait StammdatenRepositoryQueries extends LazyLogging with StammdatenDBMappings {

  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val person = personMapping.syntax("pers")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
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

  lazy val lieferpositionShort = lieferpositionMapping.syntax
  lazy val korbShort = korbMapping.syntax

  protected def getAbotypenQuery = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
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
      .map({ (kunde, pl, hl, dl, personen, pendenzen) =>
        val abos = pl ++ hl ++ dl
        val personenWihoutPwd = personen.map(p => copyTo[Person, PersonDetail](p))

        copyTo[Kunde, KundeDetail](kunde, "abos" -> abos, "pendenzen" -> pendenzen, "ansprechpersonen" -> personenWihoutPwd)
      }).single
  }

  protected def getPersonenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(personMapping as person)
        .where.eq(person.kundeId, parameter(kundeId))
        .orderBy(person.sort)
    }.map(personMapping(person)).list
  }

  protected def getAbotypDetailQuery(id: AbotypId) = {
    withSQL {
      select
        .from(abotypMapping as aboTyp)
        .where.eq(aboTyp.id, parameter(id))
    }.map(abotypMapping(aboTyp)).single
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

  protected def getDepotlieferungAbosQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
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
        .where.eq(abwesenheit.lieferungId, lieferungId).and.eq(abwesenheit.aboId, aboId)
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
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.lieferplanungId, parameter(id))
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map { (lieferung, abotyp, positionen) =>
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> positionen)
      }.list
  }

  protected def getLieferungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .where.eq(lieferung.lieferplanungId, parameter(id))
    }.map(lieferungMapping(lieferung)).list
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
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp, "lieferpositionen" -> emptyPosition)
      }.list
  }

  protected def getBestellungenQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(bestellungMapping as bestellung)
        .where.eq(bestellung.lieferplanungId, parameter(id))
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

  protected def getBestellpositionenByLieferplanQuery(id: LieferplanungId) = {
    withSQL {
      select
        .from(bestellpositionMapping as bestellposition)
        .leftJoin(bestellungMapping as bestellung).on(bestellposition.bestellungId, bestellung.id)
        .where.eq(bestellung.lieferplanungId, parameter(id))
    }.map(bestellpositionMapping(bestellposition)).list
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

  protected def getKorbQuery(lieferungId: LieferungId, aboId: AboId) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .where.eq(korb.lieferungId, parameter(lieferungId))
        .and.eq(korb.aboId, parameter(aboId)).and.not.eq(korb.status, parameter(Geliefert))
    }.map(korbMapping(korb)).single
  }

  protected def getKoerbeQuery(lieferungId: LieferungId, vertriebsartId: VertriebsartId, status: KorbStatus) = {
    withSQL {
      select
        .from(korbMapping as korb)
        .innerJoin(lieferungMapping as lieferung).on(lieferung.id, korb.lieferungId)
        .innerJoin(vertriebMapping as vertrieb).on(vertrieb.id, lieferung.vertriebId)
        .leftJoin(depotlieferungMapping as depotlieferung).on(depotlieferung.vertriebId, vertrieb.id)
        .leftJoin(heimlieferungMapping as heimlieferung).on(heimlieferung.vertriebId, vertrieb.id)
        .leftJoin(postlieferungMapping as postlieferung).on(postlieferung.vertriebId, vertrieb.id)
        .where.eq(korb.lieferungId, parameter(lieferungId)).and.eq(korb.status, parameter(status))
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

  protected def getDepotAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
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
        val korbReports = koerbe map { korb =>
          val korbAbo = abos.filter(_.id == korb.aboId).head
          val abotyp = abotypen.filter(_.id == korbAbo.abotypId).head
          val kunde = copyTo[Kunde, KundeReport](kunden.filter(_.id == korbAbo.kundeId).head)

          copyTo[Korb, KorbReport](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde)
        }

        val depot = copyTo[Depot, DepotReport](depots.head)
        copyTo[DepotAuslieferung, DepotAuslieferungReport](auslieferung, "depot" -> depot, "koerbe" -> korbReports, "projekt" -> projekt)
      }).single
  }

  protected def getTourAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .join(tourMapping as tour).on(tourAuslieferung.tourId, tour.id)
        .leftJoin(korbMapping as korb).on(korb.auslieferungId, tourAuslieferung.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(korb.aboId, heimlieferungAbo.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(kundeMapping as kunde).on(heimlieferungAbo.kundeId, kunde.id)
        .where.eq(tourAuslieferung.id, parameter(auslieferungId))
    }.one(tourAuslieferungMapping(tourAuslieferung))
      .toManies(
        rs => tourMapping.opt(tour)(rs),
        rs => korbMapping.opt(korb)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => kundeMapping.opt(kunde)(rs)
      )
      .map((auslieferung, tour, koerbe, abos, abotypen, kunden) => {
        val korbReports = koerbe map { korb =>
          val korbAbo = abos.filter(_.id == korb.aboId).head
          val abotyp = abotypen.filter(_.id == korbAbo.abotypId).head
          val kunde = copyTo[Kunde, KundeReport](kunden.filter(_.id == korbAbo.kundeId).head)

          copyTo[Korb, KorbReport](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde)
        }

        copyTo[TourAuslieferung, TourAuslieferungReport](auslieferung, "tour" -> tour.head, "koerbe" -> korbReports, "projekt" -> projekt)
      }).single
  }

  protected def getPostAuslieferungReportQuery(auslieferungId: AuslieferungId, projekt: ProjektReport) = {
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
        val korbReports = koerbe map { korb =>
          val korbAbo = abos.filter(_.id == korb.aboId).head
          val abotyp = abotypen.filter(_.id == korbAbo.abotypId).head
          val kunde = copyTo[Kunde, KundeReport](kunden.filter(_.id == korbAbo.kundeId).head)

          copyTo[Korb, KorbReport](korb, "abo" -> korbAbo, "abotyp" -> abotyp, "kunde" -> kunde)
        }

        copyTo[PostAuslieferung, PostAuslieferungReport](auslieferung, "koerbe" -> korbReports, "projekt" -> projekt)
      }).single
  }

  protected def getDepotAuslieferungQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(depotAuslieferungMapping as depotAuslieferung)
        .where.eq(depotAuslieferung.lieferungId, parameter(lieferungId))
    }.map(depotAuslieferungMapping(depotAuslieferung)).single
  }

  protected def getTourAuslieferungQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(tourAuslieferungMapping as tourAuslieferung)
        .where.eq(tourAuslieferung.lieferungId, parameter(lieferungId))
    }.map(tourAuslieferungMapping(tourAuslieferung)).single
  }

  protected def getPostAuslieferungQuery(lieferungId: LieferungId) = {
    withSQL {
      select
        .from(postAuslieferungMapping as postAuslieferung)
        .where.eq(postAuslieferung.lieferungId, parameter(lieferungId))
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
          val depot = depots.find(_.id == lieferung.depotId).head
          val summary = copyTo[Depot, DepotSummary](depot)
          copyTo[Depotlieferung, DepotlieferungDetail](lieferung, "depot" -> summary)
        }
        val hl = hls.map { lieferung =>
          val tour = touren.find(_.id == lieferung.tourId).head
          copyTo[Heimlieferung, HeimlieferungDetail](lieferung, "tour" -> tour)
        }

        copyTo[Vertrieb, VertriebVertriebsarten](vertrieb, "depotlieferungen" -> dl, "heimlieferungen" -> hl, "postlieferungen" -> pl)
      }).list
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

}