package ch.openolitor.core.models

import java.util.UUID
import org.joda.time.DateTime

sealed trait EvolutionStatus
case object Applying extends EvolutionStatus
case object Done extends EvolutionStatus

object EvolutionStatus {
  val AllStatus = Vector(Applying, Done)
  
  def apply(value: String): EvolutionStatus = AllStatus.find(_.toString == value).getOrElse(Applying) 
}

case class DBSchemaId(id: UUID = UUID.randomUUID) extends BaseId
case class DBSchema(id: DBSchemaId, revision:Int, status: EvolutionStatus, executionDate: DateTime = DateTime.now) extends BaseEntity[DBSchemaId]