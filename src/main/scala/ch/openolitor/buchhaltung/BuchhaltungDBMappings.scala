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
package ch.openolitor.buchhaltung

import java.util.UUID
import ch.openolitor.core.models._
import ch.openolitor.core.repositories.BaseRepository
import ch.openolitor.core.repositories.BaseRepository._
import ch.openolitor.core.repositories.ParameterBinderMapping
import ch.openolitor.buchhaltung.models._
import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.repositories.DBMappings
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.SqlBinder
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.core.scalax._
import ch.openolitor.stammdaten.StammdatenDBMappings

//DB Model bindig
trait BuchhaltungDBMappings extends DBMappings with StammdatenDBMappings {
  import TypeBinder._

  // DB type binders for read operations
  implicit val rechnungIdBinder: TypeBinder[RechnungId] = baseIdTypeBinder(RechnungId.apply _)

  implicit val rechnungStatusTypeBinder: TypeBinder[RechnungStatus] = string.map(RechnungStatus.apply)

  //DB parameter binders for write and query operationsit
  implicit val rechnungStatusBinder = toStringSqlBinder[RechnungStatus]

  implicit val rechnungIdSqlBinder = baseIdSqlBinder[RechnungId]

  implicit val rechnungMapping = new BaseEntitySQLSyntaxSupport[Rechnung] {
    override val tableName = "Rechnung"

    override lazy val columns = autoColumns[Rechnung]()

    def apply(rn: ResultName[Rechnung])(rs: WrappedResultSet): Rechnung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Rechnung): Seq[Any] =
      parameters(Rechnung.unapply(entity).get)

    override def updateParameters(rechnung: Rechnung) = {
      super.updateParameters(rechnung) ++ Seq(
        column.kundeId -> parameter(rechnung.kundeId),
        column.aboId -> parameter(rechnung.aboId),
        column.titel -> parameter(rechnung.titel),
        column.anzahlLieferungen -> parameter(rechnung.anzahlLieferungen),
        column.waehrung -> parameter(rechnung.waehrung),
        column.betrag -> parameter(rechnung.betrag),
        column.einbezahlterBetrag -> parameter(rechnung.einbezahlterBetrag),
        column.rechnungsDatum -> parameter(rechnung.rechnungsDatum),
        column.faelligkeitsDatum -> parameter(rechnung.faelligkeitsDatum),
        column.eingangsDatum -> parameter(rechnung.eingangsDatum),
        column.status -> parameter(rechnung.status),
        column.referenzNummer -> parameter(rechnung.referenzNummer),
        column.esrNummer -> parameter(rechnung.esrNummer),
        column.strasse -> parameter(rechnung.strasse),
        column.hausNummer -> parameter(rechnung.hausNummer),
        column.adressZusatz -> parameter(rechnung.adressZusatz),
        column.plz -> parameter(rechnung.plz),
        column.ort -> parameter(rechnung.ort))
    }
  }
}
