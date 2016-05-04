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
case object InBearbeitung extends LieferungStatus
case object Bearbeitet extends LieferungStatus

object LieferungStatus {
  def apply(value: String): LieferungStatus = {
    Vector(Ungeplant, Offen, InBearbeitung, Bearbeitet).find(_.toString == value).getOrElse(Offen)
  }
}

sealed trait KorbStatus

case object WirdGeliefert extends KorbStatus
case object Geliefert extends KorbStatus
case object FaelltAusAbwesend extends KorbStatus
case object FaelltAusSaldoZuTief extends KorbStatus

object KorbStatus {
  def apply(value: String): KorbStatus = {
    Vector(WirdGeliefert, Geliefert, FaelltAusAbwesend, FaelltAusSaldoZuTief).find(_.toString == value).getOrElse(WirdGeliefert)
  }
}

case class LieferplanungId(id: Long) extends BaseId

case class Lieferplanung(
  id: LieferplanungId,
  nr: Int,
  bemerkungen: Option[String],
  abotypDepotTour: String,
  status: LieferungStatus,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[LieferplanungId]

case class LieferplanungModify(
  nr: Int,
  bemerkungen: Option[String],
  status: LieferungStatus
) extends JSONSerializable

case class LieferplanungCreate(
  bemerkungen: Option[String],
  status: LieferungStatus
) extends JSONSerializable

case class LieferungId(id: Long) extends BaseId

case class Lieferung(
  id: LieferungId,
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebsartId: VertriebsartId,
  vertriebsartBeschrieb: String,
  status: LieferungStatus,
  datum: DateTime,
  anzahlAbwesenheiten: Int,
  durchschnittspreis: BigDecimal,
  anzahlLieferungen: Int,
  anzahlKoerbeZuLiefern: Int,
  anzahlKoerbeNichtZuLiefern: Int,
  zielpreis: Option[BigDecimal],
  preisTotal: BigDecimal,
  lieferplanungId: Option[LieferplanungId],
  lieferplanungNr: Option[Int],
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[LieferungId]

case class LieferungModify(
  abotypId: AbotypId,
  abotypBeschrieb: String,
  vertriebsartId: VertriebsartId,
  vertriebsartBeschrieb: String,
  status: LieferungStatus,
  datum: DateTime,
  durchschnittspreis: BigDecimal,
  anzahlLieferungen: Int,
  preisTotal: BigDecimal,
  lieferplanungId: Option[LieferplanungId],
  lieferplanungNr: Option[Int]
) extends JSONSerializable

case class LieferungAbotypCreate(
  abotypId: AbotypId,
  vertriebsartId: VertriebsartId,
  datum: DateTime
) extends JSONSerializable

case class LieferpositionId(id: Long) extends BaseId

case class Lieferposition(
  id: LieferpositionId,
  lieferungId: LieferungId,
  produktId: ProduktId,
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
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[LieferpositionId]

case class LieferpositionModify(
  lieferungId: LieferungId,
  produktId: ProduktId,
  produktBeschrieb: String,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: Option[BigDecimal],
  preis: Option[BigDecimal],
  anzahl: Int
) extends JSONSerializable

case class BestellungId(id: Long) extends BaseId

case class Bestellung(
  id: BestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  lieferplanungNr: Int,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  preisTotal: BigDecimal,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[BestellungId]

case class BestellungModify(
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  lieferplanungNr: Int,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  preisTotal: BigDecimal
) extends JSONSerializable

case class BestellungenCreate(
  lieferplanungId: LieferplanungId
) extends JSONSerializable

case class BestellpositionId(id: Long) extends BaseId

case class Bestellposition(
  id: BestellpositionId,
  bestellungId: BestellungId,
  produktId: ProduktId,
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[BestellpositionId]

case class BestellpositionModify(
  bestellungId: BestellungId,
  produktId: ProduktId,
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int
) extends JSONSerializable

case class KorbId(id: Long) extends BaseId

case class Korb(
  id: KorbId,
  lieferungId: LieferungId,
  aboId: AboId,
  status: KorbStatus,
  //modification flags
  erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId
) extends BaseEntity[KorbId]