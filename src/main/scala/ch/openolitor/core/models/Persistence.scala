package ch.openolitor.core.models

import spray.json.JsValue
import ch.openolitor.core.JSONSerializable

case class PersistedMessage(persistenceId: String, sequenceNr: Long, payload: JsValue) extends JSONSerializable

case class PersistenceQueryParams(key: Option[String], from: Option[Long], to: Option[Long], content: Option[String]) extends JSONSerializable

case class PersistenceJournal(
  persistenceKey: Long,
  sequenceNr: Long,
  message: Option[PersistedMessage]
) extends JSONSerializable

case class PersistenceMetadata(
  persistenceId: String,
  persistenceKey: Long,
  sequenceNr: Long
) extends JSONSerializable