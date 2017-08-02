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
import ch.openolitor.core.jobs.JobQueueService.JobId

trait AuslieferungLieferscheinReportService extends AsyncConnectionPoolContextAware with ReportService with StammdatenJsonProtocol {
  self: StammdatenReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent =>
  def generateAuslieferungLieferscheinReports(fileType: FileType)(config: ReportConfig[AuslieferungId])(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[AuslieferungId]]] = {
    generateReports[AuslieferungId, AuslieferungReport](
      config,
      auslieferungById,
      fileType,
      None,
      _.id,
      GeneriertAuslieferung,
      x => Some(x.id.id.toString),
      name(fileType),
      _.projekt.sprache,
      JobId("Auslieferungs-Lieferschein(e)")
    )
  }

  def name(fileType: FileType)(auslieferung: AuslieferungReport) = {
    fileType match {
      case VorlageDepotLieferschein => s"depot_lieferschein_nr_${auslieferung.id.id}_${auslieferung.datum}"
      case VorlageTourLieferschein => s"tour_lieferschein_nr_${auslieferung.id.id}_${auslieferung.datum}"
      case VorlagePostLieferschein => s"post_lieferschein_nr_${auslieferung.id.id}_${auslieferung.datum}"

      case _ => s"auslieferung_nr_${auslieferung.id.id}_${auslieferung.datum}"
    }
  }

  def auslieferungById(auslieferungIds: Seq[AuslieferungId]): Future[(Seq[ValidationError[AuslieferungId]], Seq[AuslieferungReport])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val projektReport = copyTo[Projekt, ProjektReport](projekt)
        val results = Future.sequence(auslieferungIds.map { auslieferungId =>
          stammdatenReadRepository.getAuslieferungReport(auslieferungId, projektReport).map(_.map { auslieferung =>
            Right(auslieferung)
          }.getOrElse(Left(ValidationError[AuslieferungId](auslieferungId, s"Auslieferung konnte nicht gefunden werden"))))
        })
        results.map(_.partition(_.isLeft) match {
          case (a, b) => (a.map(_.left.get), b.map(_.right.get))
        })
      } getOrElse Future { (Seq(ValidationError[AuslieferungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }
}
