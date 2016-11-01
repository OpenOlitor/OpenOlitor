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
package ch.openolitor.core.db

import scalikejdbc.config._
import scalikejdbc._
import scalikejdbc.async._
import ch.openolitor.core.MandantConfiguration
import ch.openolitor.util.ConfigUtil._

/**
 * Mandant specific dbs for async scalikejdbc framework
 */
case class AsyncMandantDBs(mandantConfiguration: MandantConfiguration) extends DBs
    with TypesafeConfigReader
    with TypesafeConfig
    with EnvPrefix
    with DbNameFixer {

  override lazy val config = mandantConfiguration.config
  lazy val maxQueueSize = config.getIntOption("db.default.maxQueueSize") getOrElse 1000

  def connectionPool(name: Any, url: String, user: String, password: String,
    settings: AsyncConnectionPoolSettings = AsyncConnectionPoolSettings()): AsyncConnectionPool =
    AsyncConnectionPoolFactory.apply(url, user, password, settings)

  implicit def toAsyncConnectionPoolSettings(cpSettings: ConnectionPoolSettings): AsyncConnectionPoolSettings = AsyncConnectionPoolSettings(maxPoolSize = cpSettings.maxSize, maxQueueSize)

  def loadConnectionPool(dbName: Symbol = ConnectionPool.DEFAULT_NAME): AsyncConnectionPool = {
    val JDBCSettings(url, user, password, driver) = readJDBCSettings(dbName)
    val cpSettings = readConnectionPoolSettings(dbName)
    Class.forName(driver)
    val _url = fixDbName(url)
    connectionPool(dbName, _url, user, password, cpSettings)
  }

  def connectionPoolContext(): MultipleAsyncConnectionPoolContext = {
    val context = for (dbName <- dbNames) yield (Symbol(dbName), loadConnectionPool(Symbol(dbName)))
    //: _* converts list into a varargs parameter of type tuple2
    MultipleAsyncConnectionPoolContext(context: _*)
  }
}
