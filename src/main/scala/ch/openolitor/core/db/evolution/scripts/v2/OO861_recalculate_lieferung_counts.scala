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
package ch.openolitor.core.db.evolution.scripts.v2

import ch.openolitor.core.db.evolution.scripts.recalculations.RecalculateAnzahlSaldoZuTiefLieferung
import ch.openolitor.core.db.evolution.scripts.recalculations.RecalculateAnzahlAbwesenheitenLieferung
import ch.openolitor.core.db.evolution.scripts.recalculations.RecalculateAnzahlGeliefertLieferung

object OO861_recalculate_lieferung_counts {
  val scripts = Seq(
    RecalculateAnzahlSaldoZuTiefLieferung.scripts,
    RecalculateAnzahlAbwesenheitenLieferung.scripts,
    RecalculateAnzahlGeliefertLieferung.scripts
  )
}
