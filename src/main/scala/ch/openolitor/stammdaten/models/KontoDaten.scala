package ch.openolitor.stammdaten.models

import ch.openolitor.core.models._
import org.joda.time.DateTime
import ch.openolitor.core.JSONSerializable

case class KontoDatenId(id: Long) extends BaseId

case class KontoDaten(
  id: KontoDatenId,
  iban: Option[String],
  teilnehmerNummer: Option[String],
  referenzNummerPrefix: Option[String],
  //modification flags
  erstelldat: DateTime,
  ersteller: PersonId,
  modifidat: DateTime,
  modifikator: PersonId
) extends BaseEntity[KontoDatenId]

case class KontoDatenModify(
  iban: Option[String],
  teilnehmerNummer: Option[String],
  referenzNummerPrefix: Option[String]
) extends JSONSerializable
