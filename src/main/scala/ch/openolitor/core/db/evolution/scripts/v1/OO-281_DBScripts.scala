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
package ch.openolitor.core.db.evolution.scripts.v1

import ch.openolitor.core.db.evolution.Script
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.SystemConfig
import scalikejdbc._
import scala.util.Try
import scala.util.Success
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.core.models.PersonId
import org.joda.time.DateTime
import ch.openolitor.core.db.evolution.scripts.DefaultDBScripts
import ch.openolitor.core.models.PersonId

object OO281_DBScripts extends DefaultDBScripts {
  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      logger.debug(s"Create table KontoDaten")

      sql"""create table ${kontoDatenMapping.table} (
        id BIGINT not null,
        iban VARCHAR(34),
        referenz_nummer_prefix varchar(27),
        teilnehmer_nummer varchar(9),
        erstelldat datetime not null,
        ersteller BIGINT not null,
        modifidat datetime not null,
        modifikator BIGINT not null)""".execute.apply()

      Success(true)
    }
  }

  val StammdatenInitialData = new Script with LazyLogging with StammdatenDBMappings with DefaultDBScripts {

    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {
      lazy val config = sysConfig.mandantConfiguration.config
      lazy val iban = config.getStringOption(s"buchhaltung.iban")
      lazy val referenznummerPrefix = config.getStringOption(s"buchhaltung.referenznummer-prefix")
      lazy val teilnehmernummer = config.getStringOption(s"buchhaltung.teilnehmernummer")

      logger.debug(s"Initial data for KontoDaten")

      val pid = sysConfig.mandantConfiguration.dbSeeds.get(classOf[PersonId]).getOrElse(1L)
      implicit val personId = PersonId(pid)

      sql"""insert into ${kontoDatenMapping.table}
        (id,   iban,  referenz_nummer_prefix,   teilnehmer_nummer,      erstelldat,   ersteller,       modifidat, modifikator) values
        (1, ${iban}, ${referenznummerPrefix}, ${teilnehmernummer}, ${DateTime.now}, ${personId}, ${DateTime.now}, ${personId})""".execute.apply()

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts, StammdatenInitialData)

}
