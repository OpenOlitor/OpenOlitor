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

case class BestellungId(id: Long) extends BaseId

case class Bestellung(
  id: BestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  status: LieferungStatus,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  datumVersendet: Option[DateTime],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellungId]

case class BestellungDetail(
  id: BestellungId,
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  status: LieferungStatus,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  datumVersendet: Option[DateTime],
  positionen: Seq[Bestellposition],
  produzent: Produzent,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellungId]

case class BestellungAusgeliefert(
  datum: DateTime,
  ids: Seq[BestellungId]
) extends JSONSerializable

case class BestellungModify(
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  lieferplanungId: LieferplanungId,
  datum: DateTime,
  datumAbrechnung: Option[DateTime],
  preisTotal: BigDecimal
) extends JSONSerializable

@Deprecated
case class BestellungenCreate(
  lieferplanungId: LieferplanungId
) extends JSONSerializable

case class BestellungCreate(
  produzentId: ProduzentId,
  lieferplanungId: LieferplanungId,
  datum: DateTime
) extends JSONSerializable

case class BestellpositionId(id: Long) extends BaseId

case class Bestellposition(
  id: BestellpositionId,
  bestellungId: BestellungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int,
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[BestellpositionId]

case class BestellpositionModify(
  bestellungId: BestellungId,
  produktId: Option[ProduktId],
  produktBeschrieb: String,
  preisEinheit: Option[BigDecimal],
  einheit: Liefereinheit,
  menge: BigDecimal,
  preis: Option[BigDecimal],
  anzahl: Int
) extends JSONSerializable

case class ProduzentenabrechnungReport(
  produzentId: ProduzentId,
  produzentKurzzeichen: String,
  produzent: ProduzentDetailReport,
  bestellungenDetails: Seq[BestellungDetail],
  preisTotal: BigDecimal,
  steuerSatz: Option[BigDecimal],
  steuer: BigDecimal,
  totalSteuer: BigDecimal,
  //Zusatzinformationen
  projekt: ProjektReport
) extends JSONSerializable
