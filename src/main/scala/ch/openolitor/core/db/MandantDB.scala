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
import ch.openolitor.core.MandantConfiguration

/**
 * Mandant specific dbs
 */
case class MandantDBs(mandantConfiguration: MandantConfiguration) extends DBs
    with TypesafeConfigReader
    with TypesafeConfig
    with EnvPrefix
    with DbNameFixer {

  override lazy val config = mandantConfiguration.config

  def connectionPool(name: Any, url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings())(implicit factory: ConnectionPoolFactory = ConnectionPool.DEFAULT_CONNECTION_POOL_FACTORY): ConnectionPool = {

    import scalikejdbc.JDBCUrl._

    val (_factory, factoryName) = Option(settings.connectionPoolFactoryName).flatMap { name =>
      ConnectionPoolFactoryRepository.get(name).map(f => (f, name))
    }.getOrElse((factory, "<default>"))

    // register new pool or replace existing pool
    // Heroku support
    url match {
      case HerokuPostgresRegexp(_user, _password, _host, _dbname) =>
        val _url = "jdbc:postgresql://%s/%s".format(_host, _dbname)
        _factory.apply(_url, _user, _password, settings)
      case url @ HerokuMySQLRegexp(_user, _password, _host, _dbname) =>
        val defaultProperties = """?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"""
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        val _url = "jdbc:mysql://%s/%s".format(_host, _dbname + addDefaultPropertiesIfNeeded)
        _factory.apply(_url, _user, _password, settings)
      case _ =>
        // strip ?user,pw
        val _url = fixDbName(url)
        _factory.apply(_url, user, password, settings)
    }
  }

  def loadConnectionPool(dbName: Symbol = ConnectionPool.DEFAULT_NAME): ConnectionPool = {
    val JDBCSettings(url, user, password, driver) = readJDBCSettings(dbName)
    val cpSettings = readConnectionPoolSettings(dbName)
    Class.forName(driver)
    connectionPool(dbName, url, user, password, cpSettings)
  }

  def connectionPoolContext(): ConnectionPoolContext = {
    val context = for (dbName <- dbNames) yield (Symbol(dbName), loadConnectionPool(Symbol(dbName)))
    //: _* converts list into a varargs parameter of type tuple2
    MultipleConnectionPoolContext(context: _*)
  }
}