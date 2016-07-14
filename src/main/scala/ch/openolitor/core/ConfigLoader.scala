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
package ch.openolitor.core

import com.typesafe.config._
import scala.collection.JavaConversions._

/**
 * The OO-ConfigLoader is used to represent environmental config lists of the form:
 *   "ENV_VAR_ID": {
 *     "db": [
 *       {
 *         "credentials": {
 *           "host": "",
 *         },
 *         "name": "unique-name",
 *       },
 * as a configuration object which can be accessed using the index of the desired list object
 * (${?ENV_VAR_ID.db-object.0.credentials.host})
 * and using the name if configured
 * (${?ENV_VAR_ID.db-object.unique-name.credentials.host}).
 */
object ConfigLoader {
  val ObjectIdentifier = "-object"
  val NameIdentifier = "name"

  def loadConfig: Config = {
    loadEnvironmentConfigs()
  }

  def loadEnvironmentConfigs() = {
    val envConfigIds = ConfigFactory.load("envvars").getStringList("environment-config-list")

    val merged = (envConfigIds foldLeft ConfigFactory.empty) {
      case (result, envConfigId) =>
        sys.env.get(envConfigId) map { external =>
          result.withFallback(transformToObjectConfig(ConfigFactory.parseString(external)).atPath(envConfigId))
        } getOrElse {
          result
        }
    }

    val result = ConfigFactory.defaultApplication(ConfigParseOptions.defaults().setAllowMissing(true)).withFallback(merged)
    ConfigFactory.load(result)
  }

  def transformToObjectConfig(config: Config): Config = {
    val result = config.entrySet.foldLeft(ConfigFactory.empty) {
      case (result, m) =>
        if (m.getValue.isInstanceOf[ConfigList]) {
          result.withValue(m.getKey, m.getValue).withValue(m.getKey + ObjectIdentifier, listToObject(m.getValue.asInstanceOf[ConfigList]))
        } else {
          result.withValue(m.getKey, m.getValue)
        }
    }

    result
  }

  /**
   * Creates list objects identified by their index and name if present.
   */
  def listToObject(config: ConfigList): ConfigObject = {
    val configObject = (config.indices zip config flatMap {
      case (i, v) =>
        val name = v.asInstanceOf[ConfigObject].get(NameIdentifier)
        val namedObject = if (name != null) (name.unwrapped.toString, v) :: Nil else Nil
        (i.toString, v) :: namedObject
    }).toMap

    ConfigValueFactory.fromMap(configObject)
  }
}