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
package ch.openolitor.core.security

import spray.json._
import zangelo.spray.json.AutoProductFormats
import ch.openolitor.core.JSONSerializable
import ch.openolitor.stammdaten.StammdatenJsonProtocol

trait LoginJsonProtocol extends StammdatenJsonProtocol with AutoProductFormats[JSONSerializable] {
  implicit val loginStatusFormat = new JsonFormat[LoginStatus] {
    def write(obj: LoginStatus): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): LoginStatus =
      json match {
        case JsString(value) => LoginStatus(value).getOrElse(sys.error(s"Unknown LoginStatus:$value"))
        case pt => sys.error(s"Unknown LoginStatus:$pt")
      }
  }
}