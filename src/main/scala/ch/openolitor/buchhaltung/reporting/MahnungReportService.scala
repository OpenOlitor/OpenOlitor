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
package ch.openolitor.buchhaltung.reporting

import ch.openolitor.buchhaltung.models.RechnungId
import scalaz._
import scalaz.Scalaz._
import scala.concurrent.Future
import ch.openolitor.buchhaltung.models._
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.filestore._
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.ReportSystem._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models.Projekt
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.stammdaten.models.ProjektReport
import ch.openolitor.core.models.PersonId
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.buchhaltung.repositories.BuchhaltungReadRepositoryAsyncComponent
import scala.Left
import scala.Right
import ch.openolitor.core.jobs.JobQueueService.JobId

trait MahnungReportService extends AsyncConnectionPoolContextAware with ReportService with BuchhaltungJsonProtocol with RechnungReportData {
  self: BuchhaltungReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent with StammdatenReadRepositoryAsyncComponent =>

  def generateMahnungReports(config: ReportConfig[RechnungId])(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[RechnungId]]] = {
    generateReports[RechnungId, RechnungDetailReport](
      config,
      rechungenById,
      VorlageMahnung,
      None,
      _.id,
      GeneriertMahnung,
      x => Some(x.id.id.toString),
      name,
      _.projekt.sprache,
      JobId("Mahnung(en)")
    )
  }

  private def name(rechnung: RechnungDetailReport) = {
    // die Anzahl der Mahnungen wird erst durch die Aktion "Mahnung verschickt" inkrementiert
    s"rechnung_nr_${rechnung.id.id}_mahnung_${rechnung.anzahlMahnungen + 1}";
  }
}