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

import scalikejdbc._
import scalikejdbc.TypeBinder._
import ch.openolitor.core.models._
import stamina.DefaultPersistedCodec
import scala.util.{ Try, Success, Failure }
import stamina.Persisters
import spray.json.JsValue
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.repositories.DBMappings
import akka.util.ByteString
import akka.util.ByteStringBuilder
import java.io.InputStream
import scala.annotation.tailrec
import akka.serialization.SerializationExtension
import akka.serialization.Serialization
import akka.persistence.PersistentRepr
import ch.openolitor.core.ActorSystemReference
import spray.json._
import ch.openolitor.core.domain.PersistentEvent

trait PersistenceDBMappings extends DBMappings with ActorSystemReference {

  val codec = DefaultPersistedCodec
  val persisters = Persisters(EventStoreSerializer.eventStorePersisters)
  implicit val byteOrder = java.nio.ByteOrder.LITTLE_ENDIAN

  private def inputStreamToString(is: InputStream) = {
    val builder = ByteString.newBuilder
    readStream(is, builder)
  }

  @tailrec
  private def readStream(is: InputStream, builder: ByteStringBuilder): ByteString = {
    val buffer = new Array[Byte](1024)
    is.read(buffer) match {
      case l if l <= 0 => builder.result
      case l =>
        for (i <- 0 to l) {
          print(buffer(i) + " ")
          builder.putByte(buffer(i))
        }
        readStream(is, builder)
    }
  }

  val serialization: Serialization = SerializationExtension(system)

  implicit val persistedMessageBinder: TypeBinder[Option[PersistedMessage]] = bytes map { message =>
    Try {
      serialization.deserialize(message, classOf[PersistentRepr]) match {
        case Success(m: PersistentRepr) =>
          m.payload match {
            case payload: JsValue =>
              Some(PersistedMessage(m.persistenceId, m.sequenceNr, payload))
            case x: AnyRef if persisters.canPersist(x) =>
              // decapitalize
              val chars = x.getClass.getSimpleName.toCharArray()
              if (chars.length > 1) {
                chars(0) = chars(0).toLower
              }
              // add name to object
              val name = new String(chars)
              val bytes = persisters.persist(x).bytes
              val json = JsonParser(ParserInput(bytes.toArray))
              val result = JsObject(name -> json)
              Some(PersistedMessage(m.persistenceId, m.sequenceNr, result))
          }
        case _ => None
      }
    } match {
      case Success(msg) => msg
      case Failure(e) =>
        println(s">>>>>>Errro:$e")
        e.printStackTrace()
        None
    }
  }

  implicit val persistentEventBinder: TypeBinder[Option[PersistentEvent]] = bytes map { message =>
    Try {
      serialization.deserialize(message, classOf[PersistentRepr]) match {
        case Success(m: PersistentRepr) =>
          m.payload match {
            case x: PersistentEvent => Some(x)
          }
        case _ => None
      }
    } match {
      case Success(msg) => msg
      case Failure(e) =>
        None
    }
  }

  implicit val persistenceJournalMapping = new SQLSyntaxSupport[PersistenceJournal] with LazyLogging with DBMappings {
    override val tableName = "persistence_journal"

    override lazy val columns = autoColumns[PersistenceJournal]()

    //override def columnNames 
    def apply(p: SyntaxProvider[PersistenceJournal])(rs: WrappedResultSet): PersistenceJournal = apply(p.resultName)(rs)

    def opt(e: SyntaxProvider[PersistenceJournal])(rs: WrappedResultSet): Option[PersistenceJournal] = try {
      Option(apply(e)(rs))
    } catch {
      case e: IllegalArgumentException => None
    }

    def apply(rn: ResultName[PersistenceJournal])(rs: WrappedResultSet): PersistenceJournal = autoConstruct(rs, rn)
  }

  implicit val persistenceMetadataMapping = new SQLSyntaxSupport[PersistenceMetadata] with LazyLogging with DBMappings {
    override val tableName = "persistence_metadata"

    override lazy val columns = autoColumns[PersistenceMetadata]()

    //override def columnNames 
    def apply(p: SyntaxProvider[PersistenceMetadata])(rs: WrappedResultSet): PersistenceMetadata = apply(p.resultName)(rs)

    def opt(e: SyntaxProvider[PersistenceMetadata])(rs: WrappedResultSet): Option[PersistenceMetadata] = try {
      Option(apply(e)(rs))
    } catch {
      case e: IllegalArgumentException => None
    }

    def apply(rn: ResultName[PersistenceMetadata])(rs: WrappedResultSet): PersistenceMetadata = autoConstruct(rs, rn)
  }
}