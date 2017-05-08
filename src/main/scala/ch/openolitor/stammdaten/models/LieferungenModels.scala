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
package ch.openolitor.stammdaten.models

import org.joda.time.DateTime
import ch.openolitor.core.models._
import java.util.UUID
import ch.openolitor.core.JSONSerializable

sealed trait LieferungStatus

case object Ungeplant extends LieferungStatus
case object Offen extends LieferungStatus
case object Abgeschlossen extends LieferungStatus
case object Verrechnet extends LieferungStatus

object LieferungStatus {
  def apply(value: String): LieferungStatus = {
    Vector(Ungeplant, Offen, Abgeschlossen, Verrechnet) find (_.toString == value) getOrElse (Offen)
  }
}

case class LieferplanungId(id: Long) extends BaseId

case class Lieferplanung(
  id: LieferplanungId,
  bemerkungen: Option[String],
  abotypDepotTour: String,
  status: LieferungStatus,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferplanungId]

case class LieferplanungOpenDetail(
  id: LieferplanungId,
  bemerkungen: Option[String],
  abotypDepotTour: String,
  status: LieferungStatus,
  //details
  lieferungen: Seq[LieferungOpenDetail],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
//
) extends BaseEntity[LieferplanungId]

case class LieferplanungModify(
  bemerkungen: Option[String]
) extends JSONSerializable

case class LieferplanungCreate(
  bemerkungen: Option[String]
) extends JSONSerializable

trait BaseLieferungId extends BaseId

case class LieferungId(id: Long) extends BaseLieferungId {
  def getLieferungOnLieferplanungId(): LieferungOnLieferplanungId = {
    LieferungOnLieferplanungId(id)
  }
}

case class LieferungOnLieferplanungId(id: Long) extends BaseLieferungId {
  def getLieferungId(): LieferungId = {
    LieferungId(id)
  }
}

case class LieferplanungCreated(id: LieferplanungId) extends Product with JSONSerializable

// datum entspricht einem Zeitpunkt
case class Lieferung(
  id: LieferungId,
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  status: LieferungStatus,
  datum: DateTime,
  durchschnittspreis: BigDecimal,
  anzahlLieferungen: Int,
  anzahlKoerbeZuLiefern: Int,
  anzahlAbwesenheiten: Int,
  anzahlSaldoZuTief: Int,
  zielpreis: Option[BigDecimal],
  preisTotal: BigDecimal,
  lieferplanungId: Option[LieferplanungId],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferungId]

case class LieferungDetail(
  id: LieferungId,
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  status: LieferungStatus,
  datum: DateTime,
  anzahlKoerbeZuLiefern: Int,
  anzahlAbwesenheiten: Int,
  anzahlSaldoZuTief: Int,
  zielpreis: Option[BigDecimal],
  preisTotal: BigDecimal,
  lieferplanungId: Option[LieferplanungId],
  abotyp: Option[Abotyp],
  lieferpositionen: Seq[Lieferposition],
  lieferplanungBemerkungen: Option[String],
  //value for actual geschaeftsjahr
  durchschnittspreis: BigDecimal,
  anzahlLieferungen: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferungId]

case class LieferungOpenDetail(
  id: LieferungId,
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebId: VertriebId,
  vertriebBeschrieb: Option[String],
  status: LieferungStatus,
  datum: DateTime,
  lieferplanungId: Option[LieferplanungId],
  lieferpositionen: Seq[LieferpositionOpen],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferungId]

case class LieferungModify(
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebId: VertriebId,
  vertriebsartBeschrieb: Option[String],
  status: LieferungStatus,
  datum: DateTime,
  preisTotal: BigDecimal,
  lieferplanungId: Option[LieferplanungId]
) extends JSONSerializable

case class LieferungPlanungAdd(
  id: LieferungId,
  lieferplanungId: LieferplanungId
) extends JSONSerializable

case class LieferungPlanungRemove() extends JSONSerializable

case class LieferungAbotypCreate(
  abotypId: AbotypId,
  vertriebId: VertriebId,
  datum: DateTime
) extends JSONSerializable

case class LieferungenAbotypCreate(abotypId: AbotypId, vertriebId: VertriebId, daten: Seq[DateTime]) extends JSONSerializable

case class LieferpositionId(id: Long) extends BaseId

case class Lieferposition(
  id: LieferpositionId,
  lieferungId: LieferungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: Option[BigDecimal],
  preis: Option[BigDecimal],
  anzahl: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferpositionId]

case class LieferpositionOpen(
  id: LieferpositionId,
  lieferungId: LieferungId,
  produktBeschrieb: String,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: Option[BigDecimal],
  preis: Option[BigDecimal],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[LieferpositionId]

case class LieferpositionModify(
  lieferungId: LieferungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: Option[BigDecimal],
  preis: Option[BigDecimal],
  anzahl: Int
) extends JSONSerializable

case class LieferplanungDataModify(
  id: LieferplanungId,
  newSammelbestellungen: Set[SammelbestellungCreate],
  lieferungen: Set[LieferungPositionenModify]
) extends JSONSerializable

case class LieferplanungPositionenModify(
  id: LieferplanungId,
  lieferungen: Set[LieferungPositionenModify]
) extends JSONSerializable

case class LieferungPositionenModify(
  id: LieferungId,
  lieferpositionen: LieferpositionenModify
) extends JSONSerializable

case class LieferpositionenModify(
  preisTotal: Option[BigDecimal],
  lieferpositionen: List[LieferpositionModify]
) extends JSONSerializable
