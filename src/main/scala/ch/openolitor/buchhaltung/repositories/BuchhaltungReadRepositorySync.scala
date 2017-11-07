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
import ch.openolitor.core.repositories._
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.buchhaltung.models._

trait BuchhaltungReadRepositorySync extends BaseReadRepositorySync {
  def getRechnungen(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung]
  def getKundenRechnungen(kundeId: KundeId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung]
  def getRechnungDetail(id: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[RechnungDetail]
  def getRechnungByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[Rechnung]

  def getRechnungsPositionenByRechnungsId(rechnungId: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[RechnungsPosition]

  def getZahlungsImports(implicit session: DBSession, cpContext: ConnectionPoolContext): List[ZahlungsImport]
  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsImportDetail]
  def getZahlungsEingangByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsEingang]

  def getKontoDaten(implicit session: DBSession): Option[KontoDaten]
}

trait BuchhaltungReadRepositorySyncImpl extends BuchhaltungReadRepositorySync with LazyLogging with BuchhaltungRepositoryQueries {
  def getRechnungen(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung] = {
    getRechnungenQuery(None).apply()
  }

  def getKundenRechnungen(kundeId: KundeId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[Rechnung] = {
    getKundenRechnungenQuery(kundeId).apply()
  }

  def getRechnungDetail(id: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[RechnungDetail] = {
    getRechnungDetailQuery(id).apply()
  }

  def getRechnungByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[Rechnung] = {
    getRechnungByReferenznummerQuery(referenzNummer).apply()
  }

  def getRechnungsPositionenByRechnungsId(rechnungId: RechnungId)(implicit session: DBSession, cpContext: ConnectionPoolContext): List[RechnungsPosition] = {
    getRechnungsPositionenByRechnungsIdQuery(rechnungId).apply()
  }

  def getZahlungsImports(implicit session: DBSession, cpContext: ConnectionPoolContext): List[ZahlungsImport] = {
    getZahlungsImportsQuery.apply()
  }

  def getZahlungsImportDetail(id: ZahlungsImportId)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsImportDetail] = {
    getZahlungsImportDetailQuery(id).apply()
  }

  def getZahlungsEingangByReferenznummer(referenzNummer: String)(implicit session: DBSession, cpContext: ConnectionPoolContext): Option[ZahlungsEingang] = {
    getZahlungsEingangByReferenznummerQuery(referenzNummer)();
  }

  def getKontoDaten(implicit session: DBSession): Option[KontoDaten] = {
    getKontoDatenQuery.apply()
  }
}
