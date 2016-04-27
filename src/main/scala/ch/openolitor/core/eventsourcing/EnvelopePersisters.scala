package ch.openolitor.core.eventsourcing

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import stamina._
import migrations._
import ch.openolitor.core.domain._
import spray.json._
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.models.BaseId
import com.typesafe.scalalogging.LazyLogging

package events {

  sealed abstract class PersistedEventPersister[T <: PersistetEvent: ClassTag, V <: Version: VersionInfo](key: String, entityPersisters: Persisters) extends Persister[T, V](key) with LazyLogging {

    def persist(t: T): Persisted = {
      Persisted(key, currentVersion, toBytes(t))
    }

    def unpersist(p: Persisted): T = {
      if (canUnpersist(p)) fromBytes(p.bytes)
      else throw new IllegalArgumentException(s"Cannot unpersist")
    }

    def toBytes(t: T): ByteString

    def fromBytes(bytes: ByteString): T

    def persistEntity[E <: AnyRef](entity: E): JsValue = {
      logger.debug(s"persistEntity:$entity")
      entityPersisters.canPersist(entity) match {
        case true =>
          //build entity json
          val persisted = entityPersisters.persist(entity)
          val data = toJson(persisted.bytes)
          JsObject(
            "key" -> JsString(persisted.key),
            "version" -> JsNumber(persisted.version),
            "data" -> data
          )
        case _ => throw new IllegalArgumentException(s"No persister found for entity:${entity}")
      }
    }

    def unpersistEntity[E <: AnyRef](json: JsValue): E = {
      json.asJsObject.getFields("key", "version", "data") match {
        case Seq(JsString(key), JsNumber(version), data) =>
          val persisted = Persisted(key, version.toInt, fromJson(data))
          entityPersisters.canUnpersist(persisted) match {
            case true => entityPersisters.unpersist(persisted).asInstanceOf[E]
            case false => throw new IllegalArgumentException(s"No unpersister found for key:$key, version:$version, data:$data")
          }
        case x => throw new DeserializationException(s"Entity data expected, received:$x")
      }
    }

    def toJson(bytes: ByteString): JsValue = JsonParser(ParserInput(bytes.toArray))

    def fromJson(json: JsValue): ByteString = ByteString(json.toString)
  }

  class EntityInsertEventPersister[V <: Version: VersionInfo](entityPersisters: Persisters)
      extends PersistedEventPersister[EntityInsertedEvent[BaseId, AnyRef], V]("entity-inserted", entityPersisters) with EntityStoreJsonProtocol with BaseJsonProtocol
      with LazyLogging {

    def toBytes(t: EntityInsertedEvent[BaseId, AnyRef]): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)

      //lookup persister for entity
      val entity = persistEntity(t.entity)
      val id = persistEntity(t.id)

      fromJson(JsObject(
        "meta" -> meta,
        "id" -> id,
        "entity" -> entity
      ))
    }

    def fromBytes(bytes: ByteString): EntityInsertedEvent[BaseId, AnyRef] = {
      toJson(bytes).asJsObject.getFields("meta", "id", "entity") match {
        case Seq(metaJson, idJson, entityJson) =>
          val meta = metadataFormat.read(metaJson)
          val id: BaseId = unpersistEntity(idJson)

          val entity = unpersistEntity[AnyRef](entityJson)
          val event = EntityInsertedEvent(meta, id, entity)
          event
        case x => throw new DeserializationException(s"EntityInsertedEvent data expected, received:$x")
      }
    }
  }

  class EntityUpdatedEventPersister[V <: Version: VersionInfo](entityPersisters: Persisters)
      extends PersistedEventPersister[EntityUpdatedEvent[BaseId, AnyRef], V]("entity-updated", entityPersisters) with EntityStoreJsonProtocol with BaseJsonProtocol
      with LazyLogging {

    def toBytes(t: EntityUpdatedEvent[BaseId, AnyRef]): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val id = persistEntity(t.id)

      //lookup persister for entity
      val entity = persistEntity[AnyRef](t.entity)

      logger.debug(s"Persist update:$meta, $id, $entity")
      val json = JsObject(
        "meta" -> meta,
        "id" -> id,
        "entity" -> entity
      )
      fromJson(json)
    }

    def fromBytes(bytes: ByteString): EntityUpdatedEvent[BaseId, AnyRef] = {
      toJson(bytes).asJsObject.getFields("meta", "id", "entity") match {
        case Seq(metaJson, idJson, entityJson) =>
          val meta = metadataFormat.read(metaJson)
          val id: BaseId = unpersistEntity(idJson)

          val entity = unpersistEntity[AnyRef](entityJson)
          EntityUpdatedEvent(meta, id, entity)
        case x => throw new DeserializationException(s"EntityUpdatedEvent data expected, received:$x")
      }
    }
  }

  class EntityDeletedEventPersister[V <: Version: VersionInfo](entityPersisters: Persisters)
      extends PersistedEventPersister[EntityDeletedEvent[BaseId], V]("entity-deleted", entityPersisters) with EntityStoreJsonProtocol with BaseJsonProtocol {

    def toBytes(t: EntityDeletedEvent[BaseId]): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val id = persistEntity(t.id)

      fromJson(JsObject(
        "meta" -> meta,
        "id" -> id
      ))
    }

    def fromBytes(bytes: ByteString): EntityDeletedEvent[BaseId] = {
      toJson(bytes).asJsObject.getFields("meta", "id") match {
        case Seq(metaJson, idJson) =>
          val meta = metadataFormat.read(metaJson)
          val id: BaseId = unpersistEntity(idJson)

          EntityDeletedEvent(meta, id)
        case x => throw new DeserializationException(s"EntityDeletedEvent data expected, received:$x")
      }
    }
  }
}