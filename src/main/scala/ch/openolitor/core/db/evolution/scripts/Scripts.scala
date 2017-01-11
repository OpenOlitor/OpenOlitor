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
package ch.openolitor.core.db.evolution.scripts

object Scripts {
  val current =
    V1Scripts.scripts ++
      OO205_DBScripts.scripts ++
      OO215_DBScripts.scripts ++
      OO219_DBScripts.scripts ++
      OO228_DBScripts.scripts ++
      OO219_DBScripts_FilestoreReference.scripts ++
      OO220_DBScripts.scripts ++
      OO297_DBScripts.scripts ++
      OO311_DBScripts.scripts ++
      OO314_DBScripts.scripts ++
      OO325_DBScripts.scripts ++
      OO326_DBScripts.scripts ++
      OO328_DBScripts.scripts ++
      OO327_DBScripts.scripts ++
      OO254_DBScripts.scripts ++
      OO152_DBScripts.scripts ++
      OO330_DBScripts.scripts ++
      OO337_DBScripts.scripts ++
      OO382_DBScripts.scripts ++
      OO106_DBScripts_Mahnungen.scripts ++
      OO374_DBScripts.scripts ++
      OO374_DBScripts_aktiv_to_abo.scripts ++
      OO461_DBScripts.scripts ++
      OO468_DBScripts.scripts ++
      OO433_DBScripts.scripts
}
