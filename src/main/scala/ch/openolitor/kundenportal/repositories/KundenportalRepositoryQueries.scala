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

import scalikejdbc._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.Macros._
import ch.openolitor.util.DateTimeUtil._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.core.security.Subject
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

trait KundenportalRepositoryQueries extends LazyLogging with StammdatenDBMappings with BuchhaltungDBMappings {

  //Stammdaten
  lazy val projekt = projektMapping.syntax("projekt")
  lazy val kontoDaten = kontoDatenMapping.syntax("kontoDaten")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val kundentyp = customKundentypMapping.syntax("kundentyp")
  lazy val person = personMapping.syntax("pers")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val zusatzAbo = zusatzAboMapping.syntax("zusatzAbo")
  lazy val abwesenheit = abwesenheitMapping.syntax("abwesenheit")
  lazy val korb = korbMapping.syntax("korb")
  lazy val lieferung = lieferungMapping.syntax("lieferung")
  lazy val lieferplanung = lieferplanungMapping.syntax("lieferplanung")
  lazy val aboTyp = abotypMapping.syntax("atyp")
  lazy val zusatzAboTyp = zusatzAbotypMapping.syntax("zatyp")
  lazy val vertrieb = vertriebMapping.syntax("vertrieb")
  lazy val lieferposition = lieferpositionMapping.syntax("lieferposition")

  //Buchhaltung
  lazy val rechnung = rechnungMapping.syntax("rechnung")
  lazy val rechnungsPosition = rechnungsPositionMapping.syntax("rechnungsPosition")

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

  protected def getDepotlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(depotlieferungAboMapping as depotlieferungAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(depotlieferungAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(depotlieferungAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(abotypMapping as aboTyp).on(depotlieferungAbo.abotypId, aboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(depotlieferungAbo.vertriebId, vertrieb.id)
        .where.eq(depotlieferungAbo.kundeId, owner.kundeId)
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
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
        .where.eq(heimlieferungAbo.kundeId, owner.kundeId)
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
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
        .where.eq(postlieferungAbo.kundeId, owner.kundeId)
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
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

  protected def getZusatzabosQuery(aboId: AboId, filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(zusatzAboMapping as zusatzAbo)
        .leftJoin(abwesenheitMapping as abwesenheit).on(zusatzAbo.id, abwesenheit.aboId)
        .leftJoin(lieferungMapping as lieferung).on(zusatzAbo.abotypId, lieferung.abotypId)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferung.lieferplanungId, lieferplanung.id)
        .leftJoin(zusatzAbotypMapping as zusatzAboTyp).on(zusatzAbo.abotypId, zusatzAboTyp.id)
        .leftJoin(vertriebMapping as vertrieb).on(zusatzAbo.vertriebId, vertrieb.id)
        .where.eq(zusatzAbo.kundeId, owner.kundeId).and.eq(zusatzAbo.hauptAboId, aboId)
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, zusatzAbo))
        .and.withRoundBracket(_.isNull(lieferung.lieferplanungId).or.eq(lieferplanung.status, Ungeplant).or.eq(lieferplanung.status, Offen))
    }
      .one(zusatzAboMapping(zusatzAbo))
      .toManies(
        rs => abwesenheitMapping.opt(abwesenheit)(rs),
        rs => lieferungMapping.opt(lieferung)(rs),
        rs => zusatzAbotypMapping.opt(zusatzAboTyp)(rs),
        rs => vertriebMapping.opt(vertrieb)(rs)
      )
      .map((abo, abw, lieferungen, aboTyp, vertriebe) => {
        val sortedAbw = abw.filter(_.aboId == abo.id).sortBy(_.datum)
        val sortedLieferungen = lieferungen.filter(_.abotypId == abo.abotypId).sortBy(_.datum)
        val filteredZusatzAboTypen = aboTyp.filter(_.id == abo.abotypId)
        val filteredVertriebe = vertriebe.filter(_.id == abo.vertriebId)
        copyTo[ZusatzAbo, ZusatzAboDetail](abo, "abwesenheiten" -> sortedAbw, "lieferdaten" -> sortedLieferungen,
          "abotyp" -> filteredZusatzAboTypen.headOption, "vertrieb" -> filteredVertriebe.headOption)
      }).list
  }

  protected def getLieferungenByAbotypQuery(id: AbotypId, filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(lieferungMapping as lieferung)
        .leftJoin(abotypMapping as aboTyp).on(lieferung.abotypId, aboTyp.id)
        .leftJoin(lieferpositionMapping as lieferposition).on(lieferposition.lieferungId, lieferung.id)
        .leftJoin(lieferplanungMapping as lieferplanung).on(lieferplanung.id, lieferung.lieferplanungId)
        .where.eq(lieferung.abotypId, id)
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, lieferung))
        .and.withRoundBracket { _.eq(lieferung.status, Abgeschlossen).or.eq(lieferung.status, Verrechnet) }
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
        .where.eq(lieferung.id, id)
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
        .where.eq(rechnung.kundeId, owner.kundeId)
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).list
  }

  protected def getRechnungDetailQuery(id: RechnungId)(implicit owner: Subject) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
        .leftJoin(rechnungsPositionMapping as rechnungsPosition).on(rechnung.id, rechnungsPosition.rechnungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnungsPosition.aboId, depotlieferungAbo.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnungsPosition.aboId, heimlieferungAbo.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnungsPosition.aboId, postlieferungAbo.id)
        .where.eq(rechnung.id, id)
        .and.eq(rechnung.kundeId, owner.kundeId)
        .orderBy(rechnung.rechnungsDatum)
    }.one(rechnungMapping(rechnung))
      .toManies(
        rs => kundeMapping.opt(kunde)(rs),
        rs => rechnungsPositionMapping.opt(rechnungsPosition)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs)
      )
      .map({ (rechnung, kunden, rechnungsPositionen, pl, hl, dl) =>
        val kunde = kunden.head
        val abos = pl ++ hl ++ dl
        val rechnungsPositionenDetail = {
          for {
            rechnungsPosition <- rechnungsPositionen
            abo <- abos.find(_.id == rechnungsPosition.aboId.orNull)
          } yield {
            copyTo[RechnungsPosition, RechnungsPositionDetail](rechnungsPosition, "abo" -> abo)
          }
        }.sortBy(_.sort.getOrElse(0))

        copyTo[Rechnung, RechnungDetail](rechnung, "kunde" -> kunde, "rechnungsPositionen" -> rechnungsPositionenDetail)
      }).single
  }
}
