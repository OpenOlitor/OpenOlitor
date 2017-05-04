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
package ch.openolitor.stammdaten

import ch.openolitor.core.models._
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.repositories._
import org.joda.time.DateTime
import scalikejdbc._
import ch.openolitor.util.IdUtil

trait SammelbestellungenHandler extends StammdatenDBMappings {
  this: StammdatenWriteRepositoryComponent =>

  def createOrUpdateSammelbestellungen(id: SammelbestellungId, create: SammelbestellungModify)(implicit personId: PersonId, session: DBSession) = {
    stammdatenWriteRepository.getById(produzentMapping, create.produzentId) map { produzent =>

      val sammelbestellung = stammdatenWriteRepository.getById(sammelbestellungMapping, id) getOrElse {
        val newSammelbestellung = Sammelbestellung(
          id,
          create.produzentId,
          produzent.kurzzeichen,
          create.lieferplanungId,
          Abgeschlossen,
          create.datum,
          None,
          None,
          0,
          produzent.mwstSatz,
          0,
          0,
          DateTime.now,
          personId,
          DateTime.now,
          personId
        )
        stammdatenWriteRepository.insertEntity[Sammelbestellung, SammelbestellungId](newSammelbestellung)
        newSammelbestellung
      }

      // delete all Bestellpositionen from Bestellungen (Bestellungen are maintained even if nothing is ordered/bestellt)
      stammdatenWriteRepository.getBestellpositionenBySammelbestellung(id) map {
        position =>
          stammdatenWriteRepository.deleteEntity[Bestellposition, BestellpositionId](position.id)
      }

      val groupedLieferungen = stammdatenWriteRepository.getLieferungenDetails(create.lieferplanungId) groupBy (_.abotyp.get.adminProzente)

      val totalsToAggregate = groupedLieferungen map {
        case (adminProzente, lieferungen) =>
          val anzahlKoerbeZuLiefern = lieferungen.map(l => (l.id, l.anzahlKoerbeZuLiefern)).toMap

          val bestellung = stammdatenWriteRepository.getBestellung(id, adminProzente) getOrElse {
            val newBestellung = Bestellung(
              BestellungId(IdUtil.positiveRandomId),
              id,
              0,
              produzent.mwstSatz,
              0,
              0,
              adminProzente,
              0,
              0,
              DateTime.now,
              personId,
              DateTime.now,
              personId
            )
            stammdatenWriteRepository.insertEntity[Bestellung, BestellungId](newBestellung)
            newBestellung
          }

          val positionen = stammdatenWriteRepository.getLieferpositionenByLieferplanAndProduzent(create.lieferplanungId, create.produzentId).
            //group by same produkt, menge and preis
            groupBy(x => (x.produktId, x.menge, x.preis)).map {
              case ((produktId, menge, preis), positionen) =>
                positionen.map(lp => anzahlKoerbeZuLiefern.get(lp.lieferungId).getOrElse(0)).sum match {
                  case 0 => //don't add position
                    None
                  case anzahl =>
                    positionen.headOption map { lieferposition =>
                      Bestellposition(
                        BestellpositionId(IdUtil.positiveRandomId),
                        bestellung.id,
                        lieferposition.produktId,
                        lieferposition.produktBeschrieb,
                        lieferposition.preisEinheit,
                        lieferposition.einheit,
                        menge.getOrElse(0),
                        preis.map(_ * anzahl),
                        anzahl,
                        DateTime.now,
                        personId,
                        DateTime.now,
                        personId
                      )
                    }
                }
            }.flatten

          positionen.map { bestellposition =>
            stammdatenWriteRepository.insertEntity[Bestellposition, BestellpositionId](bestellposition)
          }

          val total = positionen.map(_.preis).flatten.sum
          val adminProzenteAbzug = bestellung.adminProzente / 100 * total
          val totalNachAbzugAdminProzente = total - adminProzenteAbzug
          // mwst auf total ohne adminanteil
          val mwst = bestellung.steuerSatz.map(_ / 100 * totalNachAbzugAdminProzente).getOrElse(BigDecimal(0))
          val totalInkl = totalNachAbzugAdminProzente + mwst

          //update total on bestellung, steuer and totalSteuer
          val copy = bestellung.copy(preisTotal = total, steuer = mwst, totalSteuer = totalInkl, adminProzenteAbzug = adminProzenteAbzug, totalNachAbzugAdminProzente = totalNachAbzugAdminProzente)
          stammdatenWriteRepository.updateEntity[Bestellung, BestellungId](copy)
          (total, mwst, totalInkl)
      }
      val totals = totalsToAggregate.foldLeft((BigDecimal(0), BigDecimal(0), BigDecimal(0))) { case ((accA, accB, accC), (a, b, c)) => (accA + a, accB + b, accC + c) }

      val copy = sammelbestellung.copy(preisTotal = totals._1, steuer = totals._2, totalSteuer = totals._3)
      stammdatenWriteRepository.updateEntity[Sammelbestellung, SammelbestellungId](copy)
    }

  }
}
