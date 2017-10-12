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
package ch.openolitor.stammdaten.reporting

import ch.openolitor.core.reporting._
import ch.openolitor.core.reporting.models._
import ch.openolitor.stammdaten._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories.StammdatenReadRepositoryAsyncComponent
import ch.openolitor.core.ActorReferences
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.filestore._
import scala.concurrent.Future
import ch.openolitor.core.models.PersonId
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.Macros._
import ch.openolitor.core.filestore._
import org.joda.time.DateTime
import ch.openolitor.core.jobs.JobQueueService.JobId

trait AuslieferungEtikettenReportService extends AsyncConnectionPoolContextAware with ReportService with StammdatenJsonProtocol {
  self: StammdatenReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent =>
  def generateAuslieferungEtikettenReports(fileType: FileType)(config: ReportConfig[AuslieferungId])(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[AuslieferungId]]] = {
    generateReports[AuslieferungId, MultiReport[AuslieferungReportEntry]](
      config,
      auslieferungenByIds,
      fileType,
      None,
      _.id,
      GeneriertAuslieferung,
      x => Some(x.id.id.toString),
      name(fileType),
      _.projekt.sprache,
      JobId("Auslieferungs-Etikette(n)")
    )
  }

  private def name(fileType: FileType)(auslieferung: MultiReport[AuslieferungReportEntry]) = {
    val now = new DateTime()
    s"lieferetiketten_${now}"
  }

  private def auslieferungenByIds(auslieferungIds: Seq[AuslieferungId]): Future[(Seq[ValidationError[AuslieferungId]], Seq[MultiReport[AuslieferungReportEntry]])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val projektReport = copyTo[Projekt, ProjektReport](projekt)
        val f = stammdatenReadRepository.getMultiAuslieferungReport(auslieferungIds, projektReport) map { result =>
          (Seq(), List(result))
        }

        f.onFailure {
          case e => e.printStackTrace()
        }

        f
      } getOrElse Future { (Seq(ValidationError[AuslieferungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }
}
