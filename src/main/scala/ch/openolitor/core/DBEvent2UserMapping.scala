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

import akka.actor._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import spray.json._
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models._
import ch.openolitor.stammdaten.StammdatenReadRepository
import ch.openolitor.buchhaltung.models.Rechnung
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol

object DBEvent2UserMapping extends DefaultJsonProtocol {
  def props(): Props = Props(classOf[DBEvent2UserMapping])

  implicit def dbEventCreateWriter[E <: BaseEntity[_ <: BaseId]](implicit writer: JsonWriter[E]) = new RootJsonWriter[DBEvent[E]] {
    def write(obj: DBEvent[E]): JsValue =
      JsObject("type" -> JsString(obj.productPrefix),
        "entity" -> JsString(obj.entity.productPrefix),
        "data" -> writer.write(obj.entity))
  }
}

/**
 * Redirect all dbevents to the client itself
 */
class DBEvent2UserMapping extends Actor
    with ActorLogging
    with ClientReceiver
    with StammdatenJsonProtocol
    with BuchhaltungJsonProtocol {
  import DBEvent2UserMapping._

  override val system = context.system

  override def preStart() {
    super.preStart()
    //register ourself as listener to sendtoclient commands
    context.system.eventStream.subscribe(self, classOf[DBEvent[_]])
  }

  override def postStop() {
    context.system.eventStream.unsubscribe(self, classOf[DBEvent[_]])
    super.postStop()
  }

  val receive: Receive = {
    //TODO: resolve module based json formats of entities, maybe create module based sealed interfaces?
    case e @ EntityModified(userId, entity: Abotyp, _) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityCreated(userId, entity: Abotyp) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityDeleted(userId, entity: Abotyp) => send(userId, e.asInstanceOf[DBEvent[Abotyp]])

    case e @ EntityModified(userId, entity: Abo, _) => send(userId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityCreated(userId, entity: Abo) => send(userId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityDeleted(userId, entity: Abo) => send(userId, e.asInstanceOf[DBEvent[Abo]])

    //    case e @ EntityModified(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])
    case e @ EntityCreated(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])
    case e @ EntityDeleted(userId, entity: Person) => send(userId, e.asInstanceOf[DBEvent[Person]])

    case e @ EntityModified(userId, entity: Kunde, _) => send(userId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityCreated(userId, entity: Kunde) => send(userId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityDeleted(userId, entity: Kunde) => send(userId, e.asInstanceOf[DBEvent[Kunde]])

    case e @ EntityModified(userId, entity: Pendenz, _) => send(userId, e.asInstanceOf[DBEvent[Pendenz]])
    case e @ EntityCreated(userId, entity: Pendenz) => send(userId, e.asInstanceOf[DBEvent[Pendenz]])

    case e @ EntityModified(userId, entity: Depot, _) => send(userId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityCreated(userId, entity: Depot) => send(userId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityDeleted(userId, entity: Depot) => send(userId, e.asInstanceOf[DBEvent[Depot]])

    case e @ EntityModified(userId, entity: Tour, _) => send(userId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityCreated(userId, entity: Tour) => send(userId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityDeleted(userId, entity: Tour) => send(userId, e.asInstanceOf[DBEvent[Tour]])

    case e @ EntityModified(userId, entity: CustomKundentyp, _) => send(userId, e.asInstanceOf[DBEvent[CustomKundentyp]])
    case e @ EntityCreated(userId, entity: CustomKundentyp) => send(userId, e.asInstanceOf[DBEvent[CustomKundentyp]])
    case e @ EntityDeleted(userId, entity: CustomKundentyp) => send(userId, e.asInstanceOf[DBEvent[CustomKundentyp]])

    case e @ EntityCreated(userId, entity: Lieferung) => send(userId, e.asInstanceOf[DBEvent[Lieferung]])
    case e @ EntityDeleted(userId, entity: Lieferung) => send(userId, e.asInstanceOf[DBEvent[Lieferung]])

    case e @ EntityCreated(userId, entity: Depotlieferung) => send(userId, e.asInstanceOf[DBEvent[Depotlieferung]])
    case e @ EntityModified(userId, entity: Depotlieferung, _) => send(userId, e.asInstanceOf[DBEvent[Depotlieferung]])
    case e @ EntityDeleted(userId, entity: Depotlieferung) => send(userId, e.asInstanceOf[DBEvent[Depotlieferung]])

    case e @ EntityCreated(userId, entity: Heimlieferung) => send(userId, e.asInstanceOf[DBEvent[Heimlieferung]])
    case e @ EntityModified(userId, entity: Heimlieferung, _) => send(userId, e.asInstanceOf[DBEvent[Heimlieferung]])
    case e @ EntityDeleted(userId, entity: Heimlieferung) => send(userId, e.asInstanceOf[DBEvent[Heimlieferung]])

    case e @ EntityCreated(userId, entity: Postlieferung) => send(userId, e.asInstanceOf[DBEvent[Postlieferung]])
    case e @ EntityModified(userId, entity: Postlieferung, _) => send(userId, e.asInstanceOf[DBEvent[Postlieferung]])
    case e @ EntityDeleted(userId, entity: Postlieferung) => send(userId, e.asInstanceOf[DBEvent[Postlieferung]])

    case e @ EntityCreated(userId, entity: Produkt) => send(userId, e.asInstanceOf[DBEvent[Produkt]])
    case e @ EntityModified(userId, entity: Produkt, _) => send(userId, e.asInstanceOf[DBEvent[Produkt]])
    case e @ EntityDeleted(userId, entity: Produkt) => send(userId, e.asInstanceOf[DBEvent[Produkt]])

    case e @ EntityCreated(userId, entity: Produktekategorie) => send(userId, e.asInstanceOf[DBEvent[Produktekategorie]])
    case e @ EntityModified(userId, entity: Produktekategorie, _) => send(userId, e.asInstanceOf[DBEvent[Produktekategorie]])
    case e @ EntityDeleted(userId, entity: Produktekategorie) => send(userId, e.asInstanceOf[DBEvent[Produktekategorie]])

    case e @ EntityCreated(userId, entity: Produzent) => send(userId, e.asInstanceOf[DBEvent[Produzent]])
    case e @ EntityModified(userId, entity: Produzent, _) => send(userId, e.asInstanceOf[DBEvent[Produzent]])
    case e @ EntityDeleted(userId, entity: Produzent) => send(userId, e.asInstanceOf[DBEvent[Produzent]])

    case e @ EntityCreated(userId, entity: Projekt) => send(userId, e.asInstanceOf[DBEvent[Projekt]])
    case e @ EntityModified(userId, entity: Projekt, _) => send(userId, e.asInstanceOf[DBEvent[Projekt]])

    case e @ EntityCreated(userId, entity: Rechnung) => send(userId, e.asInstanceOf[DBEvent[Rechnung]])
    case e @ EntityModified(userId, entity: Rechnung, _) => send(userId, e.asInstanceOf[DBEvent[Rechnung]])
    case e @ EntityDeleted(userId, entity: Rechnung) => send(userId, e.asInstanceOf[DBEvent[Rechnung]])

    case x => log.debug(s"receive unknown event $x")
  }
}