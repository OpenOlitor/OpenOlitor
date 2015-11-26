package ch.openolitor.core

import spray.httpx.unmarshalling._
import ch.openolitor.core.models.BaseId
import java.util.UUID
import spray.routing._
import shapeless.HNil
import spray.http.Uri.Path

trait SprayDeserializers {
  implicit val string2BooleanConverter = new Deserializer[String, Boolean] {
    def apply(value: String) = value.toLowerCase match {
      case "true" | "yes" | "on" => Right(true)
      case "false" | "no" | "off" => Right(false)
      case x => Left(MalformedContent("'" + x + "' is not a valid Boolean value"))
    }
  }

  def string2BaseIdPathMatcher[T <: BaseId](implicit f: UUID => T): spray.routing.PathMatcher1[T] = {
    PathMatchers.JavaUUID.flatMap(uuid => Some(f(uuid)))
  }

  def string2BaseIdConverter[T <: BaseId](implicit f: UUID => T) = new Deserializer[String, T] {
    def apply(value: String) = {
      try {
        val uuid = UUID.fromString(value)
        if (uuid == null) {
          Left(MalformedContent(s"'$value' is not a valid UUID:null"))
        } else {
          Right(f(uuid))
        }
      } catch {
        case e: Exception =>
          Left(MalformedContent(s"'$value' is not a valid UUID:$e"))
      }
    }

  }
}