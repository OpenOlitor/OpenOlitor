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
package ch.openolitor.core.eventsourcing

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import stamina._
import stamina.json._
import ch.openolitor.core.domain._
import spray.json._
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.core.domain.EntityStore._
import ch.openolitor.core.models.BaseId
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.mailservice._
import ch.openolitor.core.mailservice.MailService._
import zangelo.spray.json.AutoProductFormats

package object events extends DefaultJsonProtocol {
  import spray.json.lenses.JsonLenses._

  val V1toV2metaDataMigration: JsonMigrator[V2] = from[V1].to[V2] { meta1 =>
    // write seqNr to transactionNr, set seqNr to 1
    val oldSeq = meta1.extract[Long]('meta / 'seqNr)
    meta1.update('meta / 'transactionNr ! set[Long](oldSeq)).update('meta / 'seqNr ! set[Long](1L))
  }
}

package events {

  sealed abstract class PersistedEventPersisterVn[T <: PersistentEvent: ClassTag, V <: Version: VersionInfo: MigratableVersion](key: String, entityPersisters: Persisters, migrator: JsonMigrator[V]) extends PersistedEventPersister[T, V](key, entityPersisters) {
    override def canUnpersist(p: Persisted): Boolean = p.key == key && migrator.canMigrate(p.version)

    override def unpersist(p: Persisted): T = {
      if (canUnpersist(p)) unpersist(migrator.migrate(toJson(p.bytes), p.version))
      else throw new IllegalArgumentException(s"Cannot unpersist:$p")
    }
  }

  sealed abstract class PersistedEventPersister[T <: PersistentEvent: ClassTag, V <: Version: VersionInfo](key: String, entityPersisters: Persisters) extends Persister[T, V](key) with LazyLogging {

    def persist(t: T): Persisted = {
      Persisted(key, currentVersion, toBytes(t))
    }

    def unpersist(p: Persisted): T = {
      if (canUnpersist(p)) {
        fromBytes(p.bytes)
      } else throw new IllegalArgumentException(s"Cannot unpersist:$p")
    }

    def toBytes(t: T): ByteString

    def fromBytes(bytes: ByteString): T = {
      unpersist(toJson(bytes))
    }

    def unpersist(json: JsValue): T

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

  class EntityInsertEventPersister(entityPersisters: Persisters)
      extends PersistedEventPersisterVn[EntityInsertedEvent[BaseId, AnyRef], V2]("entity-inserted", entityPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol
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

    def unpersist(json: JsValue): EntityInsertedEvent[BaseId, AnyRef] = {
      json.asJsObject.getFields("meta", "id", "entity") match {
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

  class EntityUpdatedEventPersister(entityPersisters: Persisters)
      extends PersistedEventPersisterVn[EntityUpdatedEvent[BaseId, AnyRef], V2]("entity-updated", entityPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol
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

    def unpersist(json: JsValue): EntityUpdatedEvent[BaseId, AnyRef] = {
      json.asJsObject.getFields("meta", "id", "entity") match {
        case Seq(metaJson, idJson, entityJson) =>
          val meta = metadataFormat.read(metaJson)
          val id: BaseId = unpersistEntity(idJson)

          val entity = unpersistEntity[AnyRef](entityJson)
          EntityUpdatedEvent(meta, id, entity)
        case x => throw new DeserializationException(s"EntityUpdatedEvent data expected, received:$x")
      }
    }
  }

  class EntityDeletedEventPersister(entityPersisters: Persisters)
      extends PersistedEventPersisterVn[EntityDeletedEvent[BaseId], V2]("entity-deleted", entityPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol {

    def toBytes(t: EntityDeletedEvent[BaseId]): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val id = persistEntity(t.id)

      fromJson(JsObject(
        "meta" -> meta,
        "id" -> id
      ))
    }

    def unpersist(json: JsValue): EntityDeletedEvent[BaseId] = {
      json.asJsObject.getFields("meta", "id") match {
        case Seq(metaJson, idJson) =>
          val meta = metadataFormat.read(metaJson)
          val id: BaseId = unpersistEntity(idJson)

          EntityDeletedEvent(meta, id)
        case x => throw new DeserializationException(s"EntityDeletedEvent data expected, received:$x")
      }
    }
  }

  class SystemEventPersister(eventPersisters: Persisters)
      extends PersistedEventPersisterVn[PersistentSystemEvent, V2]("system-event", eventPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol {

    def toBytes(t: PersistentSystemEvent): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val event = persistEntity(t.event)

      fromJson(JsObject(
        "meta" -> meta,
        "event" -> event
      ))
    }

    def unpersist(json: JsValue): PersistentSystemEvent = {
      json.asJsObject.getFields("meta", "event") match {
        case Seq(metaJson, eventJson) =>
          val meta = metadataFormat.read(metaJson)
          val event: SystemEvent = unpersistEntity(eventJson)

          PersistentSystemEvent(meta, event)
        case x => throw new DeserializationException(s"PersistentSystemEvent data expected, received:$x")
      }
    }
  }

  class SendMailEventPersister(eventPersisters: Persisters)
      extends PersistedEventPersisterVn[SendMailEvent, V2]("send-mail-event", eventPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol with MailJsonProtocol {

    def toBytes(t: SendMailEvent): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val mail = mailFormat.write(t.mail)
      val commandMeta = t.commandMeta map (persistEntity) getOrElse JsNull
      val expires = dateTimeFormat.write(t.expires)

      fromJson(JsObject(
        "meta" -> meta,
        "uid" -> JsString(t.uid),
        "mail" -> mail,
        "expires" -> expires,
        "commandMeta" -> commandMeta
      ))
    }

    def unpersist(json: JsValue): SendMailEvent = {
      def toSendMailEvent(metaJson: JsValue, uid: String, mailJson: JsValue, expiresJson: JsValue, commandMeta: Option[AnyRef]) = {
        val meta = metadataFormat.read(metaJson)
        val mail: Mail = mailFormat.read(mailJson)
        val expires = dateTimeFormat.read(expiresJson)
        SendMailEvent(meta, uid, mail, expires, commandMeta)
      }

      json.asJsObject.getFields("meta", "uid", "mail", "expires", "commandMeta") match {
        case Seq(metaJson, JsString(uid), mailJson, expiresJson, JsNull) =>
          toSendMailEvent(metaJson, uid, mailJson, expiresJson, None)
        case Seq(metaJson, JsString(uid), mailJson, expiresJson, commandMetaJson) =>
          val commandMeta: Option[AnyRef] = Some(unpersistEntity(commandMetaJson))
          toSendMailEvent(metaJson, uid, mailJson, expiresJson, commandMeta)
        case x => throw new DeserializationException(s"SendMailEvent data expected, received:$x")
      }
    }
  }

  class MailSentEventPersister(eventPersisters: Persisters)
      extends PersistedEventPersisterVn[MailSentEvent, V2]("mail-sent-event", eventPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol {

    def toBytes(t: MailSentEvent): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val commandMeta = t.commandMeta map (persistEntity) getOrElse JsNull

      fromJson(JsObject(
        "meta" -> meta,
        "uid" -> JsString(t.uid),
        "commandMeta" -> commandMeta
      ))
    }

    def unpersist(json: JsValue): MailSentEvent = {
      def toMailSentEvent(metaJson: JsValue, uid: String, commandMeta: Option[AnyRef]) = {
        val meta = metadataFormat.read(metaJson)
        MailSentEvent(meta, uid, commandMeta)
      }

      json.asJsObject.getFields("meta", "uid", "commandMeta") match {
        case Seq(metaJson, JsString(uid), JsNull) =>
          toMailSentEvent(metaJson, uid, None)
        case Seq(metaJson, JsString(uid), commandMetaJson) =>
          val commandMeta: Option[AnyRef] = Some(unpersistEntity(commandMetaJson))
          toMailSentEvent(metaJson, uid, commandMeta)
        case x => throw new DeserializationException(s"MailSentEvent data expected, received:$x")
      }
    }
  }

  class SendMailFailedEventPersister(eventPersisters: Persisters)
      extends PersistedEventPersisterVn[SendMailFailedEvent, V2]("send-mail-failed-event", eventPersisters, V1toV2metaDataMigration) with EntityStoreJsonProtocol with BaseJsonProtocol {

    def toBytes(t: SendMailFailedEvent): ByteString = {
      //build custom json
      val meta = metadataFormat.write(t.meta)
      val commandMeta = t.commandMeta map (persistEntity) getOrElse JsNull

      fromJson(JsObject(
        "meta" -> meta,
        "uid" -> JsString(t.uid),
        "numberOfRetries" -> JsNumber(t.numberOfRetries),
        "commandMeta" -> commandMeta
      ))
    }

    def unpersist(json: JsValue): SendMailFailedEvent = {
      def toSendMailFailedEvent(metaJson: JsValue, uid: String, afterNumberOfRetries: Int, commandMeta: Option[AnyRef]) = {
        val meta = metadataFormat.read(metaJson)
        SendMailFailedEvent(meta, uid, afterNumberOfRetries, commandMeta)
      }

      json.asJsObject.getFields("meta", "uid", "numberOfRetries", "commandMeta") match {
        case Seq(metaJson, JsString(uid), JsNumber(afterNumberOfRetries), JsNull) =>
          toSendMailFailedEvent(metaJson, uid, afterNumberOfRetries.toInt, None)
        case Seq(metaJson, JsString(uid), JsNumber(afterNumberOfRetries), commandMetaJson) =>
          val commandMeta: Option[AnyRef] = Some(unpersistEntity(commandMetaJson))
          toSendMailFailedEvent(metaJson, uid, afterNumberOfRetries.toInt, commandMeta)
        case x => throw new DeserializationException(s"SendMailFailedEvent data expected, received:$x")
      }
    }
  }
}
