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
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol

trait RechnungReportData extends AsyncConnectionPoolContextAware with BuchhaltungJsonProtocol {
  self: BuchhaltungReadRepositoryAsyncComponent with ActorReferences with StammdatenReadRepositoryAsyncComponent =>

  def rechungenById(rechnungIds: Seq[RechnungId]): Future[(Seq[ValidationError[RechnungId]], Seq[RechnungDetailReport])] = {
    stammdatenReadRepository.getProjekt flatMap { maybeProjekt =>
      stammdatenReadRepository.getKontoDaten flatMap { maybeKontoDaten =>
        maybeProjekt flatMap { projekt =>
          maybeKontoDaten map { kontoDaten =>
            val results = Future.sequence(rechnungIds.map { rechnungId =>
              buchhaltungReadRepository.getRechnungDetail(rechnungId).map(_.map { rechnung =>
                rechnung.status match {
                  case Storniert =>
                    Left(ValidationError[RechnungId](rechnungId, s"Für stornierte Rechnungen können keine Berichte mehr erzeugt werden"))
                  case Bezahlt =>
                    Left(ValidationError[RechnungId](rechnungId, s"Für bezahlte Rechnungen können keine Berichte mehr erzeugt werden"))
                  case _ =>
                    val projektReport = copyTo[Projekt, ProjektReport](projekt)
                    Right(copyTo[RechnungDetail, RechnungDetailReport](rechnung, "projekt" -> projektReport, "kontoDaten" -> kontoDaten))
                }

              }.getOrElse(Left(ValidationError[RechnungId](rechnungId, s"Rechnung konnte nicht gefunden werden"))))
            })
            results.map(_.partition(_.isLeft) match {
              case (a, b) => (a.map(_.left.get), b.map(_.right.get))
            })
          }
        } getOrElse Future { (Seq(ValidationError[RechnungId](null, s"Projekt konnte nicht geladen werden")), Seq()) }
      }
    }
  }
}