package ch.openolitor.core.eventsourcing

import stamina._
import stamina.json._
import spray.json._
import ch.openolitor.core.models._
import java.util.UUID
import org.joda.time._
import org.joda.time.format._
import ch.openolitor.core.BaseJsonProtocol
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.JSONSerializable
import zangelo.spray.json.AutoProductFormats
import scala.collection.immutable.TreeMap
import ch.openolitor.core.domain.EntityStoreJsonProtocol
import ch.openolitor.core.JSONSerializable

trait CoreEventStoreSerializer extends BaseJsonProtocol with EntityStoreJsonProtocol {
  implicit val personIdPersister = persister[PersonId]("person-id")

  val corePersisters = List(personIdPersister)
}