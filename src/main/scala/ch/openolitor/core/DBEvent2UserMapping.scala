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
import spray.json._
import ch.openolitor.core.models._
import ch.openolitor.core.ws._
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.StammdatenJsonProtocol
import ch.openolitor.stammdaten.models._
import ch.openolitor.buchhaltung.models._
import ch.openolitor.buchhaltung.BuchhaltungJsonProtocol
import ch.openolitor.reports.ReportsJsonProtocol
import ch.openolitor.reports.models._

object DBEvent2UserMapping extends DefaultJsonProtocol {
  def props(): Props = Props(classOf[DBEvent2UserMapping])

  implicit def dbEventCreateWriter[E <: Product](implicit writer: JsonWriter[E]) = new RootJsonWriter[DBEvent[E]] {
    def write(obj: DBEvent[E]): JsValue =
      JsObject(
        "entity" -> JsString(obj.entity.productPrefix),
        "data" -> writer.write(obj.entity)
      )
  }
}

/**
 * Redirect all dbevents to the client itself
 */
class DBEvent2UserMapping extends Actor
    with ActorLogging
    with ClientReceiver
    with StammdatenJsonProtocol
    with BuchhaltungJsonProtocol
    with ReportsJsonProtocol
    with AkkaEventStream {
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
    case e @ EntityModified(personId, entity: Vertrieb, _) => send(personId, e.asInstanceOf[DBEvent[Vertrieb]])
    case e @ EntityCreated(personId, entity: Vertrieb) => send(personId, e.asInstanceOf[DBEvent[Vertrieb]])
    case e @ EntityDeleted(personId, entity: Vertrieb) => send(personId, e.asInstanceOf[DBEvent[Vertrieb]])

    case e @ EntityModified(personId, entity: Abotyp, _) => send(personId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityCreated(personId, entity: Abotyp) => send(personId, e.asInstanceOf[DBEvent[Abotyp]])
    case e @ EntityDeleted(personId, entity: Abotyp) => send(personId, e.asInstanceOf[DBEvent[Abotyp]])

    case e @ EntityModified(personId, entity: ZusatzAbotyp, _) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbotyp]])
    case e @ EntityCreated(personId, entity: ZusatzAbotyp) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbotyp]])
    case e @ EntityDeleted(personId, entity: ZusatzAbotyp) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbotyp]])

    case e @ EntityModified(personId, entity: ZusatzAbo, _) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbo]])
    case e @ EntityCreated(personId, entity: ZusatzAbo) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbo]])
    case e @ EntityDeleted(personId, entity: ZusatzAbo) => send(personId, e.asInstanceOf[DBEvent[ZusatzAbo]])

    case e @ EntityModified(personId, entity: Abo, _) => send(personId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityCreated(personId, entity: Abo) => send(personId, e.asInstanceOf[DBEvent[Abo]])
    case e @ EntityDeleted(personId, entity: Abo) => send(personId, e.asInstanceOf[DBEvent[Abo]])

    case e @ EntityModified(personId, entity: Abwesenheit, _) => send(personId, e.asInstanceOf[DBEvent[Abwesenheit]])
    case e @ EntityCreated(personId, entity: Abwesenheit) => send(personId, e.asInstanceOf[DBEvent[Abwesenheit]])
    case e @ EntityDeleted(personId, entity: Abwesenheit) => send(personId, e.asInstanceOf[DBEvent[Abwesenheit]])

    case e @ EntityModified(personId, entity: Person, _) =>
      val personDetail = copyTo[Person, PersonDetail](e.asInstanceOf[DBEvent[Person]].entity)
      send(personId, EntityModified[PersonDetail](personId, personDetail, personDetail).asInstanceOf[DBEvent[PersonDetail]])
    case e @ EntityCreated(personId, entity: Person) => send(personId, e.asInstanceOf[DBEvent[Person]])
    case e @ EntityDeleted(personId, entity: Person) =>
      val personDetail = copyTo[Person, PersonDetail](e.asInstanceOf[DBEvent[Person]].entity)
      send(personId, EntityDeleted[PersonDetail](personId, personDetail).asInstanceOf[DBEvent[PersonDetail]])

    case e @ EntityModified(personId, entity: Kunde, _) => send(personId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityCreated(personId, entity: Kunde) => send(personId, e.asInstanceOf[DBEvent[Kunde]])
    case e @ EntityDeleted(personId, entity: Kunde) => send(personId, e.asInstanceOf[DBEvent[Kunde]])

    case e @ EntityModified(personId, entity: Pendenz, _) => send(personId, e.asInstanceOf[DBEvent[Pendenz]])
    case e @ EntityCreated(personId, entity: Pendenz) => send(personId, e.asInstanceOf[DBEvent[Pendenz]])

    case e @ EntityModified(personId, entity: Depot, _) => send(personId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityCreated(personId, entity: Depot) => send(personId, e.asInstanceOf[DBEvent[Depot]])
    case e @ EntityDeleted(personId, entity: Depot) => send(personId, e.asInstanceOf[DBEvent[Depot]])

    case e @ EntityModified(personId, entity: Tour, _) => send(personId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityCreated(personId, entity: Tour) => send(personId, e.asInstanceOf[DBEvent[Tour]])
    case e @ EntityDeleted(personId, entity: Tour) => send(personId, e.asInstanceOf[DBEvent[Tour]])

    case e @ EntityModified(personId, entity: CustomKundentyp, _) => send(personId, e.asInstanceOf[DBEvent[CustomKundentyp]])
    case e @ EntityCreated(personId, entity: CustomKundentyp) => send(personId, e.asInstanceOf[DBEvent[CustomKundentyp]])
    case e @ EntityDeleted(personId, entity: CustomKundentyp) => send(personId, e.asInstanceOf[DBEvent[CustomKundentyp]])

    case e @ EntityCreated(personId, entity: Lieferung) => send(personId, e.asInstanceOf[DBEvent[Lieferung]])
    case e @ EntityModified(personId, entity: Lieferung, _) => send(personId, e.asInstanceOf[DBEvent[Lieferung]])
    case e @ EntityDeleted(personId, entity: Lieferung) => send(personId, e.asInstanceOf[DBEvent[Lieferung]])

    case e @ EntityCreated(personId, entity: Lieferplanung) => send(personId, e.asInstanceOf[DBEvent[Lieferplanung]])
    case e @ EntityModified(personId, entity: Lieferplanung, _) => send(personId, e.asInstanceOf[DBEvent[Lieferplanung]])
    case e @ EntityDeleted(userId, entity: Lieferplanung) => send(userId, e.asInstanceOf[DBEvent[Lieferplanung]])
    case e @ DataEvent(userId, entity: LieferplanungCreated) => send(userId, e.asInstanceOf[DBEvent[LieferplanungCreated]])

    case e @ EntityCreated(personId, entity: Bestellung) => send(personId, e.asInstanceOf[DBEvent[Bestellung]])
    case e @ EntityModified(personId, entity: Bestellung, _) => send(personId, e.asInstanceOf[DBEvent[Bestellung]])

    case e @ EntityCreated(personId, entity: Depotlieferung) => send(personId, e.asInstanceOf[DBEvent[Depotlieferung]])
    case e @ EntityModified(personId, entity: Depotlieferung, _) => send(personId, e.asInstanceOf[DBEvent[Depotlieferung]])
    case e @ EntityDeleted(personId, entity: Depotlieferung) => send(personId, e.asInstanceOf[DBEvent[Depotlieferung]])

    case e @ EntityCreated(personId, entity: Heimlieferung) => send(personId, e.asInstanceOf[DBEvent[Heimlieferung]])
    case e @ EntityModified(personId, entity: Heimlieferung, _) => send(personId, e.asInstanceOf[DBEvent[Heimlieferung]])
    case e @ EntityDeleted(personId, entity: Heimlieferung) => send(personId, e.asInstanceOf[DBEvent[Heimlieferung]])

    case e @ EntityCreated(personId, entity: Postlieferung) => send(personId, e.asInstanceOf[DBEvent[Postlieferung]])
    case e @ EntityModified(personId, entity: Postlieferung, _) => send(personId, e.asInstanceOf[DBEvent[Postlieferung]])
    case e @ EntityDeleted(personId, entity: Postlieferung) => send(personId, e.asInstanceOf[DBEvent[Postlieferung]])

    case e @ EntityCreated(personId, entity: Produkt) => send(personId, e.asInstanceOf[DBEvent[Produkt]])
    case e @ EntityModified(personId, entity: Produkt, _) => send(personId, e.asInstanceOf[DBEvent[Produkt]])
    case e @ EntityDeleted(personId, entity: Produkt) => send(personId, e.asInstanceOf[DBEvent[Produkt]])

    case e @ EntityCreated(personId, entity: Produktekategorie) => send(personId, e.asInstanceOf[DBEvent[Produktekategorie]])
    case e @ EntityModified(personId, entity: Produktekategorie, _) => send(personId, e.asInstanceOf[DBEvent[Produktekategorie]])
    case e @ EntityDeleted(personId, entity: Produktekategorie) => send(personId, e.asInstanceOf[DBEvent[Produktekategorie]])

    case e @ EntityCreated(personId, entity: Produzent) => send(personId, e.asInstanceOf[DBEvent[Produzent]])
    case e @ EntityModified(personId, entity: Produzent, _) => send(personId, e.asInstanceOf[DBEvent[Produzent]])
    case e @ EntityDeleted(personId, entity: Produzent) => send(personId, e.asInstanceOf[DBEvent[Produzent]])

    case e @ EntityCreated(personId, entity: Projekt) => send(personId, e.asInstanceOf[DBEvent[Projekt]])
    case e @ EntityModified(personId, entity: Projekt, _) => send(personId, e.asInstanceOf[DBEvent[Projekt]])

    case e @ EntityCreated(personId, entity: Rechnung) => send(personId, e.asInstanceOf[DBEvent[Rechnung]])
    case e @ EntityModified(personId, entity: Rechnung, _) => send(personId, e.asInstanceOf[DBEvent[Rechnung]])
    case e @ EntityDeleted(personId, entity: Rechnung) => send(personId, e.asInstanceOf[DBEvent[Rechnung]])

    case e @ EntityCreated(personId, entity: RechnungsPosition) => send(personId, e.asInstanceOf[DBEvent[RechnungsPosition]])
    case e @ EntityModified(personId, entity: RechnungsPosition, _) => send(personId, e.asInstanceOf[DBEvent[RechnungsPosition]])
    case e @ EntityDeleted(personId, entity: RechnungsPosition) => send(personId, e.asInstanceOf[DBEvent[RechnungsPosition]])

    case e @ EntityModified(personId, entity: RechnungsPositionAssignToRechnung, _) => send(personId, e.asInstanceOf[DBEvent[RechnungsPositionAssignToRechnung]])

    case e @ EntityCreated(userId, entity: ZahlungsImport) => send(userId, e.asInstanceOf[DBEvent[ZahlungsImport]])
    case e @ EntityDeleted(userId, entity: ZahlungsImport) => send(userId, e.asInstanceOf[DBEvent[ZahlungsImport]])

    case e @ EntityModified(userId, entity: ZahlungsEingang, _) => send(userId, e.asInstanceOf[DBEvent[ZahlungsEingang]])
    case e @ EntityDeleted(userId, entity: ZahlungsEingang) => send(userId, e.asInstanceOf[DBEvent[ZahlungsEingang]])

    case e @ EntityCreated(userId, entity: ProjektVorlage) => send(userId, e.asInstanceOf[DBEvent[ProjektVorlage]])
    case e @ EntityModified(userId, entity: ProjektVorlage, _) => send(userId, e.asInstanceOf[DBEvent[ProjektVorlage]])
    case e @ EntityDeleted(userId, entity: ProjektVorlage) => send(userId, e.asInstanceOf[DBEvent[ProjektVorlage]])

    case e @ EntityModified(userId, entity: DepotAuslieferung, _) => send(userId, e.asInstanceOf[DBEvent[DepotAuslieferung]])
    case e @ EntityModified(userId, entity: TourAuslieferung, _) => send(userId, e.asInstanceOf[DBEvent[TourAuslieferung]])
    case e @ EntityModified(userId, entity: PostAuslieferung, _) => send(userId, e.asInstanceOf[DBEvent[PostAuslieferung]])

    case e @ EntityModified(userId, entity: Sammelbestellung, _) => send(userId, e.asInstanceOf[DBEvent[Sammelbestellung]])

    // Reports Modul

    case e @ EntityCreated(userId, entity: Report) => send(userId, e.asInstanceOf[DBEvent[Report]])
    case e @ EntityModified(userId, entity: Report, _) => send(userId, e.asInstanceOf[DBEvent[Report]])
    case e @ EntityDeleted(userId, entity: Report) => send(userId, e.asInstanceOf[DBEvent[Report]])

    case x => // send nothing
  }
}
