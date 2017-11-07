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

import ch.openolitor.stammdaten.models.AboId
import ch.openolitor.buchhaltung.models._
import scalikejdbc._
import ch.openolitor.core.repositories.DBMappings
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.stammdaten.StammdatenDBMappings
import ch.openolitor.core.repositories.BaseParameter

//DB Model bindig
trait BuchhaltungDBMappings extends DBMappings with StammdatenDBMappings with BaseParameter {

  // DB type binders for read operations
  implicit val rechnungIdBinder: Binders[RechnungId] = baseIdBinders(RechnungId.apply _)
  implicit val rechnungsPositionIdBinder: Binders[RechnungsPositionId] = baseIdBinders(RechnungsPositionId.apply _)
  implicit val optionRechnungsPositionIdBinder: Binders[Option[RechnungsPositionId]] = optionBaseIdBinders(RechnungsPositionId.apply _)
  implicit val zahlungsImportIdBinder: Binders[ZahlungsImportId] = baseIdBinders(ZahlungsImportId.apply _)
  implicit val zahlungsEingangIdBinder: Binders[ZahlungsEingangId] = baseIdBinders(ZahlungsEingangId.apply _)

  implicit val rechnungStatusBinders: Binders[RechnungStatus] = toStringBinder(RechnungStatus.apply)
  implicit val rechnungsPositionStatusBinders: Binders[RechnungsPositionStatus.RechnungsPositionStatus] = toStringBinder(RechnungsPositionStatus.apply)
  implicit val rechnungsPositionTypBinders: Binders[RechnungsPositionTyp.RechnungsPositionTyp] = toStringBinder(RechnungsPositionTyp.apply)
  implicit val optionRechnungIdBinder: Binders[Option[RechnungId]] = optionBaseIdBinders(RechnungId.apply _)
  implicit val optionAboIdBinder: Binders[Option[AboId]] = optionBaseIdBinders(AboId.apply _)

  implicit val zahlungsEingangStatusBinders: Binders[ZahlungsEingangStatus] = toStringBinder(ZahlungsEingangStatus.apply)

  // declare parameterbinderfactories for enum type to allow dynamic type convertion of enum subtypes
  implicit def rechnungStatusParameterBinderFactory[A <: RechnungStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def rechnungsPositionStatusStatusParameterBinderFactory[A <: RechnungsPositionStatus.RechnungsPositionStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def rechnungsPositionTypStatusParameterBinderFactory[A <: RechnungsPositionTyp.RechnungsPositionTyp]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  implicit def zahlungsEingangStatusParameterBinderFactory[A <: ZahlungsEingangStatus]: ParameterBinderFactory[A] = ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)

  implicit val rechnungMapping = new BaseEntitySQLSyntaxSupport[Rechnung] {
    override val tableName = "Rechnung"

    override lazy val columns = autoColumns[Rechnung]()

    def apply(rn: ResultName[Rechnung])(rs: WrappedResultSet): Rechnung =
      autoConstruct(rs, rn)

    def parameterMappings(entity: Rechnung): Seq[ParameterBinder] =
      parameters(Rechnung.unapply(entity).get)

    override def updateParameters(entity: Rechnung) = {
      super.updateParameters(entity) ++ Seq(
        column.kundeId -> entity.kundeId,
        column.titel -> entity.titel,
        column.betrag -> entity.betrag,
        column.einbezahlterBetrag -> entity.einbezahlterBetrag,
        column.waehrung -> entity.waehrung,
        column.rechnungsDatum -> entity.rechnungsDatum,
        column.faelligkeitsDatum -> entity.faelligkeitsDatum,
        column.eingangsDatum -> entity.eingangsDatum,
        column.status -> entity.status,
        column.referenzNummer -> entity.referenzNummer,
        column.esrNummer -> entity.esrNummer,
        column.fileStoreId -> entity.fileStoreId,
        column.anzahlMahnungen -> entity.anzahlMahnungen,
        column.mahnungFileStoreIds -> entity.mahnungFileStoreIds,
        column.strasse -> entity.strasse,
        column.hausNummer -> entity.hausNummer,
        column.adressZusatz -> entity.adressZusatz,
        column.plz -> entity.plz,
        column.ort -> entity.ort
      )
    }
  }

  implicit val rechnungsPositionMapping = new BaseEntitySQLSyntaxSupport[RechnungsPosition] {
    override val tableName = "RechnungsPosition"

    override lazy val columns = autoColumns[RechnungsPosition]()

    def apply(rn: ResultName[RechnungsPosition])(rs: WrappedResultSet): RechnungsPosition =
      autoConstruct(rs, rn)

    def parameterMappings(entity: RechnungsPosition): Seq[ParameterBinder] =
      parameters(RechnungsPosition.unapply(entity).get)

    override def updateParameters(entity: RechnungsPosition) = {
      super.updateParameters(entity) ++ Seq(
        column.rechnungId -> entity.rechnungId,
        column.parentRechnungsPositionId -> entity.parentRechnungsPositionId,
        column.aboId -> entity.aboId,
        column.kundeId -> entity.kundeId,
        column.betrag -> entity.betrag,
        column.waehrung -> entity.waehrung,
        column.anzahlLieferungen -> entity.anzahlLieferungen,
        column.beschrieb -> entity.beschrieb,
        column.status -> entity.status,
        column.typ -> entity.typ,
        column.sort -> entity.sort
      )
    }
  }

  implicit val zahlungsImportMapping = new BaseEntitySQLSyntaxSupport[ZahlungsImport] {
    override val tableName = "ZahlungsImport"

    override lazy val columns = autoColumns[ZahlungsImport]()

    def apply(rn: ResultName[ZahlungsImport])(rs: WrappedResultSet): ZahlungsImport =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsImport): Seq[ParameterBinder] =
      parameters(ZahlungsImport.unapply(entity).get)

    override def updateParameters(entity: ZahlungsImport) = {
      super.updateParameters(entity) ++ Seq(
        column.file -> entity.file,
        column.anzahlZahlungsEingaenge -> entity.anzahlZahlungsEingaenge,
        column.anzahlZahlungsEingaengeErledigt -> entity.anzahlZahlungsEingaengeErledigt
      )
    }
  }

  implicit val zahlungsEingangMapping = new BaseEntitySQLSyntaxSupport[ZahlungsEingang] {
    override val tableName = "ZahlungsEingang"

    override lazy val columns = autoColumns[ZahlungsEingang]()

    def apply(rn: ResultName[ZahlungsEingang])(rs: WrappedResultSet): ZahlungsEingang =
      autoConstruct(rs, rn)

    def parameterMappings(entity: ZahlungsEingang): Seq[ParameterBinder] =
      parameters(ZahlungsEingang.unapply(entity).get)

    override def updateParameters(entity: ZahlungsEingang) = {
      super.updateParameters(entity) ++ Seq(
        column.erledigt -> entity.erledigt,
        column.bemerkung -> entity.bemerkung
      )
    }
  }
}
