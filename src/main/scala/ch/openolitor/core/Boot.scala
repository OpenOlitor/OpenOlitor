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

import scalikejdbc.config._
import akka.actor.{ ActorSystem, Props, ActorRef }
import akka.pattern.ask
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import collection.JavaConversions._
import scalaz._
import scalaz.Scalaz._
import com.typesafe.config.Config
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.stammdaten._
import scalikejdbc.ConnectionPoolContext
import ch.openolitor.core.db._
import org.slf4j.Logger
import akka.event.slf4j.Logger
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.models.PersonId
import java.util.UUID
import ch.openolitor.core.ws.ClientMessagesServer
import resource._
import java.net.ServerSocket
import spray.http.Uri
import ch.openolitor.core.proxy.ProxyServiceActor
import util.Properties
import ch.openolitor.core.db.evolution.Evolution
import ch.openolitor.core.domain.EntityStore.CheckDBEvolution
import scala.util._
import ch.openolitor.buchhaltung.BuchhaltungEntityStoreView
import ch.openolitor.buchhaltung.BuchhaltungDBEventEntityListener
import ch.openolitor.core.models.BaseId
import spray.caching.LruCache
import ch.openolitor.core.security.Subject
import ch.openolitor.core.reporting._
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import ch.openolitor.core.mailservice.MailService
import ch.openolitor.buchhaltung.BuchhaltungReportEventListener
import ch.openolitor.core.calculations.OpenOlitorCalculations
import ch.openolitor.core.calculations.Calculations.InitializeCalculation
import ch.openolitor.util.AirbrakeNotifier

case class SystemConfig(mandantConfiguration: MandantConfiguration, cpContext: ConnectionPoolContext, asyncCpContext: MultipleAsyncConnectionPoolContext)

case class MandantConfiguration(key: String, name: String, interface: String, port: Integer, wsPort: Integer, dbSeeds: Map[Class[_], Long], config: Config) {
  val configKey = s"openolitor.${key}"

  def wsUri = s"ws://$interface:$wsPort"
  def uri = s"http://$interface:$port"
}

object Boot extends App with LazyLogging {
  case class MandantSystem(config: MandantConfiguration, system: ActorSystem)

  def freePort: Int = synchronized {
    managed(new ServerSocket(0)).map { socket =>
      socket.setReuseAddress(true)
      socket.getLocalPort()
    }.opt.getOrElse(sys.error(s"Couldn't aquire new free server port"))
  }

  logger.debug(s"application_name: " + sys.env.get("application_config"))
  logger.debug(s"config-file java prop: " + sys.props.get("config-file"))
  logger.debug(s"port: " + sys.env.get("PORT"))

  val config = ConfigLoader.loadConfig

  val systemPersonId = PersonId(0)

  // instanciate actor system per mandant, with mandantenspecific configuration
  val ooConfig = config.getConfig("openolitor")
  val configs = getMandantConfiguration(ooConfig)
  implicit val timeout = Timeout(5.seconds)

  val mandanten = startServices(configs)

  val nonConfigPort = Option(System.getenv("PORT")).getOrElse("8080")

  lazy val rootPort = config.getStringOption("openolitor.port").getOrElse(nonConfigPort).toInt

  logger.debug(s"rootPort: " + rootPort)

  lazy val rootInterface = config.getStringOption("openolitor.interface").getOrElse("0.0.0.0")
  val proxyService = config.getBooleanOption("openolitor.run-proxy-service").getOrElse(false)
  //start proxy service 
  if (proxyService) {
    startProxyService(mandanten, ooConfig)
  }

  def getMandantConfiguration(ooConfig: Config): NonEmptyList[MandantConfiguration] = {
    val mandanten = ooConfig.getStringList("mandanten").toList

    mandanten.toNel.map(_.zipWithIndex.map {
      case (mandant, index) =>
        val mandantConfig = ooConfig.getConfig(mandant).withFallback(ooConfig)

        val ifc = mandantConfig.getStringOption(s"interface").getOrElse(rootInterface)
        val port = ooConfig.getIntOption(s"$mandant.port").getOrElse(freePort)
        val wsPort = ooConfig.getIntOption(s"$mandant.webservicePort").getOrElse(freePort)
        val name = ooConfig.getStringOption(s"$mandant.name").getOrElse(mandant)

        MandantConfiguration(mandant, name, ifc, port, wsPort, dbSeeds(mandantConfig), mandantConfig)
    }).getOrElse {
      //default if no list of mandanten is configured
      val ifc = rootInterface
      val port = rootPort
      val wsPort = ooConfig.getIntOption("webservicePort").getOrElse(9001)

      NonEmptyList(MandantConfiguration("m1", "openolitor", ifc, port, wsPort, dbSeeds(ooConfig), ooConfig))
    }
  }

  def startProxyService(mandanten: NonEmptyList[MandantSystem], config: Config) = {
    implicit val proxySystem = ActorSystem("oo-proxy", config)

    val proxyService = proxySystem.actorOf(ProxyServiceActor.props(mandanten), "oo-proxy-service")
    IO(UHttp) ? Http.Bind(proxyService, interface = rootInterface, port = rootPort)
    logger.debug(s"oo-proxy-system: configured proxy listener on port ${rootPort}")
  }

  def startAirbrakeService(implicit systemConfig: SystemConfig) = {
    implicit val airbrakeNotifierSystem = ActorSystem(s"oo-airbrake-notifier-system-${systemConfig.mandantConfiguration.name}")
    val airbrakeNotifier = airbrakeNotifierSystem.actorOf(AirbrakeNotifier.props, "oo-airbrake-notifier")
    airbrakeNotifier
  }

  /**
   * Jeder Mandant wird in einem eigenen Akka System gestartet.
   */
  def startServices(configs: NonEmptyList[MandantConfiguration]): NonEmptyList[MandantSystem] = {
    configs.map { cfg =>
      implicit val app = ActorSystem(cfg.name, config.getConfig(cfg.configKey).withFallback(config))

      implicit val sysCfg = systemConfig(cfg)

      // declare token cache used in multiple locations in app
      val loginTokenCache = LruCache[Subject](
        maxCapacity = 10000,
        timeToLive = 1 day,
        timeToIdle = 4 hours
      )

      // initialise root actors
      val duration = Duration.create(1, SECONDS);
      val airbrakeNotifier = startAirbrakeService

      val system = app.actorOf(SystemActor.props(airbrakeNotifier), "oo-system")
      logger.debug(s"oo-system:$system")
      val entityStore = Await.result(system ? SystemActor.Child(EntityStore.props(new Evolution(sysCfg)), "entity-store"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> entityStore:$entityStore")
      val eventStore = Await.result(system ? SystemActor.Child(SystemEventStore.props, "event-store"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> eventStore:$eventStore")
      val mailService = Await.result(system ? SystemActor.Child(MailService.props, "mail-service"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system:$system -> eventStore:$mailService")

      val stammdatenEntityStoreView = Await.result(system ? SystemActor.Child(StammdatenEntityStoreView.props(mailService, entityStore), "stammdaten-entity-store-view"), duration).asInstanceOf[ActorRef]
      val fileStoreComponent = new DefaultFileStoreComponent(cfg.name, sysCfg, app)
      val reportSystem = Await.result(system ? SystemActor.Child(ReportSystem.props(fileStoreComponent.fileStore, sysCfg), "report-system"), duration).asInstanceOf[ActorRef]

      //start actor listening events
      val stammdatenDBEventListener = Await.result(system ? SystemActor.Child(StammdatenDBEventEntityListener.props, "stammdaten-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
      val stammdatenMailSentListener = Await.result(system ? SystemActor.Child(StammdatenMailListener.props, "stammdaten-mail-listener"), duration).asInstanceOf[ActorRef]
      val stammdatenGeneratedEventsListener = Await.result(system ? SystemActor.Child(StammdatenGeneratedEventsListener.props, "stammdaten-generated-events-listener"), duration).asInstanceOf[ActorRef]

      val buchhaltungEntityStoreView = Await.result(system ? SystemActor.Child(BuchhaltungEntityStoreView.props(entityStore), "buchhaltung-entity-store-view"), duration).asInstanceOf[ActorRef]
      val buchhaltungDBEventListener = Await.result(system ? SystemActor.Child(BuchhaltungDBEventEntityListener.props, "buchhaltung-dbevent-entity-listener"), duration).asInstanceOf[ActorRef]
      val buchhaltungReportEventListener = Await.result(system ? SystemActor.Child(BuchhaltungReportEventListener.props(entityStore), "buchhaltung-report-event-listener"), duration).asInstanceOf[ActorRef]

      //start websocket service
      val clientMessages = Await.result(system ? SystemActor.Child(ClientMessagesServer.props(loginTokenCache), "ws-client-messages"), duration).asInstanceOf[ActorRef]

      //start actor mapping dbevents to client messages
      val dbEventClientMessageMapper = Await.result(system ? SystemActor.Child(DBEvent2UserMapping.props, "db-event-mapper"), duration).asInstanceOf[ActorRef]

      val calculations = Await.result(system ? SystemActor.Child(OpenOlitorCalculations.props(entityStore), "calculations"), duration).asInstanceOf[ActorRef]

      //initialize global persistentviews
      logger.debug(s"oo-system: send Startup to entityStoreview")
      eventStore ? DefaultMessages.Startup
      stammdatenEntityStoreView ? DefaultMessages.Startup
      buchhaltungEntityStoreView ? DefaultMessages.Startup

      // create and start our service actor
      val service = Await.result(system ? SystemActor.Child(RouteServiceActor.props(entityStore, eventStore, mailService, reportSystem, fileStoreComponent.fileStore, airbrakeNotifier, loginTokenCache), "route-service"), duration).asInstanceOf[ActorRef]
      logger.debug(s"oo-system: route-service:$service")

      // start a new HTTP server on port 9005 with our service actor as the handler
      IO(UHttp) ? Http.Bind(service, interface = cfg.interface, port = cfg.port)
      logger.debug(s"oo-system: configured listener on port ${cfg.port}")

      //start new websocket service
      IO(UHttp) ? Http.Bind(clientMessages, interface = cfg.interface, port = cfg.wsPort)
      logger.debug(s"oo-system: configured ws listener on port ${cfg.wsPort}")

      calculations ! InitializeCalculation

      MandantSystem(cfg, app)
    }
  }

  def dbSeeds(config: Config) = {
    val models = config.getStringList("db.default.seed.models")
    val mappings: Seq[(Class[_], Long)] = models.map { model =>
      Class.forName(model) -> config.getLong(s"db.default.seed.mappings.$model")
    }.toSeq
    mappings.toMap
  }

  def systemConfig(mandant: MandantConfiguration) = SystemConfig(mandant, connectionPoolContext(mandant), asyncConnectionPoolContext(mandant))

  def connectionPoolContext(mandantConfig: MandantConfiguration) = MandantDBs(mandantConfig).connectionPoolContext

  def asyncConnectionPoolContext(mandantConfig: MandantConfiguration) = AsyncMandantDBs(mandantConfig).connectionPoolContext
}
