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

import ch.openolitor.core.models.VorlageType

sealed trait FileType extends Product {
  val bucket: FileStoreBucket
}

case object VorlageRechnung extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageDepotLieferschein extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageTourLieferschein extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlagePostLieferschein extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageDepotLieferetiketten extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageTourLieferetiketten extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlagePostLieferetiketten extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageMahnung extends FileType with VorlageType { val bucket = VorlagenBucket }
case object VorlageBestellung extends FileType with VorlageType { val bucket = VorlagenBucket }
case object GeneriertRechnung extends FileType { val bucket = GeneriertBucket }
case object GeneriertAuslieferung extends FileType { val bucket = GeneriertBucket }
case object GeneriertMahnung extends FileType { val bucket = GeneriertBucket }
case object GeneriertBestellung extends FileType { val bucket = GeneriertBucket }
case object ProjektStammdaten extends FileType { val bucket = StammdatenBucket }
case object ZahlungsImportDaten extends FileType { val bucket = ZahlungsImportBucket }
case object UnknownFileType extends FileType with VorlageType { lazy val bucket = sys.error("This FileType has no bucket") }

object FileType {
  val AllFileTypes = VorlageType.AllVorlageTypes ++ List(
    GeneriertRechnung,
    GeneriertAuslieferung,
    GeneriertMahnung,
    GeneriertBestellung,
    ProjektStammdaten,
    ZahlungsImportDaten
  )

  def apply(value: String): FileType = {
    AllFileTypes.find(_.toString.toLowerCase == value.toLowerCase).getOrElse(UnknownFileType)
  }
}