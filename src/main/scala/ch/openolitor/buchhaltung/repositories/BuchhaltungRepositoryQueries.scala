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
package ch.openolitor.buchhaltung.repositories

import scalikejdbc._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.util.parsing.FilterExpr
import ch.openolitor.util.querybuilder.UriQueryParamToSQLSyntaxBuilder
import ch.openolitor.buchhaltung.BuchhaltungDBMappings

trait BuchhaltungRepositoryQueries extends LazyLogging with BuchhaltungDBMappings with StammdatenDBMappings {
  lazy val rechnung = rechnungMapping.syntax("rechnung")
  lazy val rechnungsPosition = rechnungsPositionMapping.syntax("rechnungsPosition")
  lazy val kunde = kundeMapping.syntax("kunde")
  lazy val zahlungsImport = zahlungsImportMapping.syntax("zahlungsImport")
  lazy val zahlungsEingang = zahlungsEingangMapping.syntax("zahlungsEingang")
  lazy val depotlieferungAbo = depotlieferungAboMapping.syntax("depotlieferungAbo")
  lazy val heimlieferungAbo = heimlieferungAboMapping.syntax("heimlieferungAbo")
  lazy val postlieferungAbo = postlieferungAboMapping.syntax("postlieferungAbo")
  lazy val zusatzAbo = zusatzAboMapping.syntax("zusatzAbo")
  lazy val kontoDaten = kontoDatenMapping.syntax("kontoDaten")

  protected def getRechnungenQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, rechnung))
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).list
  }

  protected def getRechnungsPositionQuery(filter: Option[FilterExpr]) = {
    withSQL {
      select
        .from(rechnungsPositionMapping as rechnungsPosition)
        .where(UriQueryParamToSQLSyntaxBuilder.build(filter, rechnungsPosition))
        .orderBy(rechnungsPosition.id)
    }.map(rechnungsPositionMapping(rechnungsPosition)).list
  }

  protected def getRechnungsPositionenByRechnungsIdQuery(rechnungId: RechnungId) = {
    withSQL {
      select
        .from(rechnungsPositionMapping as rechnungsPosition)
        .where.eq(rechnungsPosition.rechnungId, rechnungId)
        .orderBy(rechnungsPosition.id)
    }.map(rechnungsPositionMapping(rechnungsPosition)).list
  }

  protected def getKundenRechnungenQuery(kundeId: KundeId) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where.eq(rechnung.kundeId, kundeId)
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).list
  }

  protected def getRechnungDetailQuery(id: RechnungId) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .leftJoin(kundeMapping as kunde).on(rechnung.kundeId, kunde.id)
        .leftJoin(rechnungsPositionMapping as rechnungsPosition).on(rechnung.id, rechnungsPosition.rechnungId)
        .leftJoin(depotlieferungAboMapping as depotlieferungAbo).on(rechnungsPosition.aboId, depotlieferungAbo.id)
        .leftJoin(heimlieferungAboMapping as heimlieferungAbo).on(rechnungsPosition.aboId, heimlieferungAbo.id)
        .leftJoin(postlieferungAboMapping as postlieferungAbo).on(rechnungsPosition.aboId, postlieferungAbo.id)
        .leftJoin(zusatzAboMapping as zusatzAbo).on(rechnungsPosition.aboId, zusatzAbo.id)
        .where.eq(rechnung.id, id)
        .orderBy(rechnung.rechnungsDatum)
    }.one(rechnungMapping(rechnung))
      .toManies(
        rs => kundeMapping.opt(kunde)(rs),
        rs => rechnungsPositionMapping.opt(rechnungsPosition)(rs),
        rs => postlieferungAboMapping.opt(postlieferungAbo)(rs),
        rs => heimlieferungAboMapping.opt(heimlieferungAbo)(rs),
        rs => depotlieferungAboMapping.opt(depotlieferungAbo)(rs),
        rs => zusatzAboMapping.opt(zusatzAbo)(rs)
      )
      .map({ (rechnung, kunden, rechnungsPositionen, pl, hl, dl, zusatzAbos) =>
        val kunde = kunden.head
        val abos = pl ++ hl ++ dl ++ zusatzAbos
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

  protected def getRechnungByReferenznummerQuery(referenzNummer: String) = {
    withSQL {
      select
        .from(rechnungMapping as rechnung)
        .where.eq(rechnung.referenzNummer, referenzNummer)
        .orderBy(rechnung.rechnungsDatum)
    }.map(rechnungMapping(rechnung)).single
  }

  protected def getZahlungsImportsQuery = {
    withSQL {
      select
        .from(zahlungsImportMapping as zahlungsImport)
    }.map(zahlungsImportMapping(zahlungsImport)).list
  }

  protected def getZahlungsImportDetailQuery(id: ZahlungsImportId) = {
    withSQL {
      select
        .from(zahlungsImportMapping as zahlungsImport)
        .leftJoin(zahlungsEingangMapping as zahlungsEingang).on(zahlungsImport.id, zahlungsEingang.zahlungsImportId)
        .where.eq(zahlungsImport.id, id)
    }.one(zahlungsImportMapping(zahlungsImport))
      .toMany(
        rs => zahlungsEingangMapping.opt(zahlungsEingang)(rs)
      )
      .map({ (zahlungsImport, zahlungsEingaenge) =>
        copyTo[ZahlungsImport, ZahlungsImportDetail](zahlungsImport, "zahlungsEingaenge" -> zahlungsEingaenge)
      }).single
  }

  protected def getZahlungsEingangByReferenznummerQuery(referenzNummer: String) = {
    withSQL {
      select
        .from(zahlungsEingangMapping as zahlungsEingang)
        .where.eq(zahlungsEingang.referenzNummer, referenzNummer)
        .orderBy(zahlungsEingang.modifidat).desc
    }.map(zahlungsEingangMapping(zahlungsEingang)).first
  }

  protected def getKontoDatenQuery = {
    withSQL {
      select
        .from(kontoDatenMapping as kontoDaten)
    }.map(kontoDatenMapping(kontoDaten)).single
  }
}
