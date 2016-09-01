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
        .where.eq(depotlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, depotlieferungAbo))
    }.map(depotlieferungAboMapping(depotlieferungAbo)).list
  }

  protected def getHeimlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(heimlieferungAboMapping as heimlieferungAbo)
        .where.eq(heimlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, heimlieferungAbo))
    }.map(heimlieferungAboMapping(heimlieferungAbo)).list
  }

  protected def getPostlieferungAbosQuery(filter: Option[FilterExpr])(implicit owner: Subject) = {
    withSQL {
      select
        .from(postlieferungAboMapping as postlieferungAbo)
        .where.eq(postlieferungAbo.kundeId, parameter(owner.kundeId))
        .and(UriQueryParamToSQLSyntaxBuilder.build(filter, postlieferungAbo))
    }.map(postlieferungAboMapping(postlieferungAbo)).list
  }
}