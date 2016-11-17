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
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

trait KundenportalRepositoryQueries extends LazyLogging with StammdatenDBMappings with BuchhaltungDBMappings {

  //Stammdaten
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

  //Buchhaltung
  lazy val rechnung = rechnungMapping.syntax("rechnung")

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
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferplanung.id, lieferung.lieferplanungId)
        .where.eq(lieferung.abotypId, parameter(id))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, lieferung))
        .and.withRoundBracket { _.eq(lieferung.status, parameter(Abgeschlossen)).or.eq(lieferung.status, parameter(Verrechnet)) }
        .orderBy(lieferung.datum).desc
    }
      .one(lieferungMapping(lieferung))
      .toManies(
        rs => abotypMapping.opt(aboTyp)(rs),
        rs => lieferpositionMapping.opt(lieferposition)(rs),
        rs => lieferplanungMapping.opt(lieferplanung)(rs)
      )
      .map((lieferung, abotyp, lieferposition, lieferplanung) => {
        val bemerkung = lieferplanung match {
          case Nil => None
          case x => x.head.bemerkungen
        }
        copyTo[Lieferung, LieferungDetail](lieferung, "abotyp" -> abotyp.headOption, "lieferpositionen" -> lieferposition, "lieferplanungBemerkungen" -> bemerkung)
      })
  }

  protected def getLieferungenDetailQuery(id: LieferungId) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .join(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferplanung.id, lieferung.lieferplanungId)
        .where.eq(lieferung.id, parameter(id))
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
      }.single
  }

  protected def getRechnungenQuery(implicit owner: Subject) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where.eq(rechnung.kundeId, parameter(owner.kundeId))
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).list
  }

  protected def getRechnungDetailQuery(id: RechnungId)(implicit owner: Subject) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnung.aboId, depotlieferungAbo.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnung.aboId, heimlieferungAbo.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnung.aboId, postlieferungAbo.id)
        .where.eq(rechnung.id, parameter(id))
        .and.eq(rechnung.kundeId, parameter(owner.kundeId))
        .orderBy(rechnung.rechnungsDatum)
    }.one(rechnungMapping(rechnung))
      .toManies(
        rs => kundeMapping.opt(kunde)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs)
      )
      .map({ (rechnung, kunden, pl, hl, dl) =>
        val kunde = kunden.head
        val abo = (pl ++ hl ++ dl).head
        copyTo[Rechnung, RechnungDetail](rechnung, "kunde" -> kunde, "abo" -> abo)
      }).single
  }
}
