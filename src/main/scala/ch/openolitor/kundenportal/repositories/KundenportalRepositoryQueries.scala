package ch.openolitor.kundenportal.repositories

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
import ch.openolitor.core.security.Subject

trait KundenportalRepositoryQueries extends LazyLogging with StammdatenDBMappings {

  lazy val projekt = projektMapping.syntax("projekt")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val kundentyp = customKundentypMapping.syntax("kundentyp")
  lazy val person = personMapping.syntax("pers")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")
  lazy val korb = korbMapping.syntax("korb")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val vertrieb = vertriebMapping.syntax("vertrieb")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")

  protected def getProjektQuery = {
    withSQL {
      select
        .from(projektMapping as projekt)
    }.map(projektMapping(projekt)).single
  }

  protected def getDepotlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(depotlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(depotlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(depotlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(depotlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
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
      }).list
  }

  protected def getHeimlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(heimlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(heimlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(heimlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(heimlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(heimlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
    }
      .one(heimlieferungAboMapping(heimlieferungAbo))
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
      }).list
  }

  protected def getPostlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(postlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(postlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(postlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(postlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(postlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, parameter(Ungeplant)).or.eq(lieferplanung.status, parameter(Offen)))
    }
      .one(postlieferungAboMapping(postlieferungAbo))
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
      }).list
  }

  protected def getLieferungenByAbotypQuery(id: AbotypId, filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.abotypId, parameter(id))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
        .and.withRoundBracket { _.eq(lieferung.status, parameter(Abgeschlossen)).or.eq(lieferung.status, parameter(Verrechnet)) }
        .orderBy(lieferung.datum)
    }
      .one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map((lieferung, abotyp, lieferposition) => {
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> lieferposition)
      })
  }

  protected def getLieferungenDetailQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .join(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .where.eq(lieferung.id, parameter(id))
    }.one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs)
      )
      .map { (lieferung, abotyp, positionen) =>
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> positionen)
      }.single
  }
}