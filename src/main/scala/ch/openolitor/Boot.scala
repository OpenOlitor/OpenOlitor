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
package ch.openolitor

import akka.actor.{ ActorSystem, Props, ActorRef }
import akka.pattern.ask
import akka.io.IO
import spray.can.Http
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import collection.JavaConversions._
import scalaz._
import Scalaz._
import com.typesafe.config.Config
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.stammdaten.StammdatenEntityStoreView

object Boot extends App {

  case class MandantConfiguration(key: String, name: String, interface: String, port: Integer)

  val config = ConfigFactory.load()

  // instanciate actor system per mandant, with mandantenspecific configuration
  val configs = getMandantConfiguration(config)
  implicit val timeout = Timeout(5.seconds)
  startServices(configs)

  //TODO: start proxy service routing to mandant instances
  val proxyService = Option(config.getBoolean("openolitor.run-proxy-service")).getOrElse(false)

  def getMandantConfiguration(config: Config): NonEmptyList[MandantConfiguration] = {
    val mandanten = config.getStringList("openolitor.mandanten").toList
    mandanten.toNel.map(_.map { mandant =>
      val ifc = Option(config.getString(s"openolitor.$mandant.interface")).getOrElse("localhost")
      val port = Option(config.getInt(s"openolitor.$mandant.port")).getOrElse(9000)
      val name = Option(config.getString(s"openolitor.$mandant.name")).getOrElse(mandant)

      MandantConfiguration(mandant, name, ifc, port)
    }).getOrElse {
      //default if no list of mandanten is configured
      val ifc = Option(config.getString("openolitor.interface")).getOrElse("localhost")
      val port = Option(config.getInt("openolitor.port")).getOrElse(9000)

      NonEmptyList(MandantConfiguration("m1", "openolitor", ifc, port))
    }
  }

  /**
   * Jeder Mandant wird in einem eigenen Akka System gestartet.
   */
  def startServices(configs: NonEmptyList[MandantConfiguration]): Unit = {
    configs.map { cfg =>
      val configKey = s"openolitor.${cfg.key}"
      implicit val app = ActorSystem(cfg.name, config.getConfig(configKey).withFallback(config))

      //initialuze root actors
      val duration = Duration.create(5, SECONDS);
      val system = app.actorOf(SystemActor.props(configKey), "system")
      val entityStore = Await.result(system ? SystemActor.Child(EntityStore.props), duration).asInstanceOf[ActorRef]
      val stammdatenEntityStoreView = Await.result(system ? SystemActor.Child(StammdatenEntityStoreView.props), duration).asInstanceOf[ActorRef]

      //initialize global persistentviews
      stammdatenEntityStoreView ! EntityStoreView.Startup

      // create and start our service actor
      val service = app.actorOf(RouteServiceActor.props(entityStore), "route-service")

      // start a new HTTP server on port 9005 with our service actor as the handler
      IO(Http) ? Http.Bind(service, interface = cfg.interface, port = cfg.port)
    }
  }
}
