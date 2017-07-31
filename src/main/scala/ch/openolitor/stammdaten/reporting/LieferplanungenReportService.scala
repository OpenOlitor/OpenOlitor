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

trait LieferplanungReportService extends AsyncConnectionPoolContextAware with ReportService with StammdatenJsonProtocol {
  self: StammdatenReadRepositoryAsyncComponent with ActorReferences with FileStoreComponent =>
  def generateLieferplanungReports(fileType: FileType)(config: ReportConfig[LieferplanungId])(implicit personId: PersonId): Future[Either[ServiceFailed, ReportServiceResult[LieferplanungId]]] = {
    generateReports[LieferplanungId, LieferplanungReport](
      config,
      lieferplanungenByIds,
      fileType,
      None,
      _.id,
      GeneriertLieferplanung,
      x => Some(x.id.id.toString),
      name(fileType),
      _.projekt.sprache,
      JobId("Lieferplanung-Report")
    )
  }

  private def name(fileType: FileType)(r: LieferplanungReport) = s"lp_${r.id}_${filenameDateFormat.print(System.currentTimeMillis())}"

  private def lieferplanungenByIds(ids: Seq[LieferplanungId]): Future[(Seq[ValidationError[LieferplanungId]], Seq[LieferplanungReport])] = {
    stammdatenReadRepository.getProjekt flatMap {
      _ map { projekt =>
        val projektReport = copyTo[Projekt, ProjektReport](projekt)

        val results = Future.sequence(ids.map { id =>
          stammdatenReadRepository.getLieferplanungReport(id, projektReport).map(_.map { r =>
            Right(r)
          }.getOrElse(Left(ValidationError[LieferplanungId](id, s"Die Lieferplanung konnte nicht gefunden werden"))))
        })
        results.map(_.partition(_.isLeft) match {
          case (a, b) => (a.map(_.left.get), b.map(_.right.get))
        })
      } getOrElse Future { (Seq(ValidationError[LieferplanungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
    }
  }
}
