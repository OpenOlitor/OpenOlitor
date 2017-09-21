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
package ch.openolitor.util

import com.typesafe.config.Config
import scala.collection.JavaConversions._

object ConfigUtil {
  /**
   * Enhanced typesafe config adding support to read config keys as option
   */
  implicit class MyConfig(self: Config) {

    private def getOption[T](path: String)(get: String => T): Option[T] = {
      if (self != null && path != null && self.hasPath(path)) {
        Some(get(path))
      } else {
        None
      }
    }

    def getStringOption(path: String): Option[String] = getOption(path)(path => self.getString(path))
    def getStringListOption(path: String): Option[List[String]] = getOption(path)(path => self.getStringList(path).toList)
    def getIntOption(path: String): Option[Int] = getOption(path)(path => self.getInt(path))
    def getBooleanOption(path: String): Option[Boolean] = getOption(path)(path => self.getBoolean(path))
    def getLongOption(path: String): Option[Long] = getOption(path)(path => self.getLong(path))
  }
}