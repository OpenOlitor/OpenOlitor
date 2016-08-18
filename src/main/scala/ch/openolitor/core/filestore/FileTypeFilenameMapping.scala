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
package ch.openolitor.core.filestore

trait FileTypeFilenameMapping {
  def defaultFileTypeId(fileType: FileType) = {
    fileType match {
      case VorlageRechnung => "Rechnung.odt"
      case VorlageDepotLieferschein => "DepotLieferschein.odt"
      case VorlageTourLieferschein => "TourLieferschein.odt"
      case VorlagePostLieferschein => "PostLieferschein.odt"
      case VorlageDepotLieferetiketten => "DepotLieferetiketten.odt"
      case VorlageTourLieferetiketten => "TourLieferetiketten.odt"
      case VorlagePostLieferetiketten => "PostLieferetiketten.odt"
      case VorlageMahnung => "Mahnung.odt"
      case VorlageBestellung => "Bestellung.odt"
      case _ => "undefined.odt"
    }
  }
}