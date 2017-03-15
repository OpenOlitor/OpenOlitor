package ch.openolitor.core.eventsourcing

import zangelo.spray.json.AutoProductFormats
import ch.openolitor.core.JSONSerializable

trait PersistenceJsonProtocol extends AutoProductFormats[JSONSerializable] {

}