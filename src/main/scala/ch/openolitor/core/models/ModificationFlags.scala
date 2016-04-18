package ch.openolitor.core.models

import org.joda.time.DateTime

case class ModificationFlags(erstelldat: DateTime,
  ersteller: UserId,
  modifidat: DateTime,
  modifikator: UserId)