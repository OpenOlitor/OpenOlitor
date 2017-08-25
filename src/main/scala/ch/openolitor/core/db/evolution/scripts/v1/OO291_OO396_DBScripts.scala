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

import scala.util.{ Try, Success }
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.evolution.Script
import ch.openolitor.stammdaten.StammdatenDBMappings
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

object OO291_OO396_DBScripts {
  import scalikejdbc._
  GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
    enabled = true,
    singleLineMode = false,
    printUnprocessedStackTrace = false,
    stackTraceDepth = 15,
    logLevel = 'debug,
    warningEnabled = false,
    warningThresholdMillis = 3000L,
    warningLogLevel = 'warn
  )

  val StammdatenScripts = new Script with LazyLogging with StammdatenDBMappings {
    def execute(sysConfig: SystemConfig)(implicit session: DBSession): Try[Boolean] = {

      // insert Kundentypen welche bisher fix im Code waren
      sql"""INSERT INTO Kundentyp (id, kundentyp, anzahl_verknuepfungen, erstelldat, ersteller, modifidat, modifikator) VALUES
            (1000, 'Vereinsmitglied', 0, '2016-01-01 00:00:00', 100, '2016-01-01 00:00:00', 100),
            (1001, 'Goenner', 0, '2016-01-01 00:00:00', 100, '2016-01-01 00:00:00', 100),
            (1002, 'Genossenschafterin', 0, '2016-01-01 00:00:00', 100, '2016-01-01 00:00:00', 100)""".execute.apply()

      // update der anzahl_verknüpfungen pro Kundentyp
      val kundentypen: Seq[String] = sql"SELECT kundentyp from Kundentyp".map(rs => rs.string("kundentyp")).list.apply()

      val counts: Seq[Int] = kundentypen.map { kundentyp =>
        val kundentypUnsafe = SQLSyntax.createUnsafely(s"%$kundentyp%")
        sql"SELECT COUNT(*) c FROM Kunde WHERE typen LIKE '$kundentypUnsafe'".map(rs => rs.int("c")).single.apply().getOrElse(0)
      }

      (kundentypen zip counts).foreach {
        case (typ, count) => {
          val kundentypUnsafe = SQLSyntax.createUnsafely(s"'$typ'")
          sql"UPDATE Kundentyp SET anzahl_verknuepfungen = ${count} WHERE kundentyp = $kundentypUnsafe".update.apply()
        }
      }

      Success(true)
    }
  }

  val scripts = Seq(StammdatenScripts)
}
