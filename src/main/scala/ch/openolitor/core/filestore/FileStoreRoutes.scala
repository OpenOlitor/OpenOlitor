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
package ch.openolitor.stammdaten

import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ch.openolitor.core._
import ch.openolitor.core.domain._
import ch.openolitor.core.db._
import spray.httpx.unmarshalling.Unmarshaller
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import java.util.UUID
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import scala.concurrent.Future
import ch.openolitor.core.Macros._
import ch.openolitor.stammdaten.models._
import ch.openolitor.core.filestore.FileType
import ch.openolitor.core.filestore.FileStoreFileMetadata
import ch.openolitor.core.filestore.FileStoreComponent

trait FileStoreRoutes extends HttpService with ActorReferences with SprayDeserializers with DefaultRouteService {
  self: FileStoreComponent =>

  //TODO: get real userid from login
  override val userId: UserId = Boot.systemUserId

  lazy val fileStoreRoute =
    pathPrefix("filestore") {
      fileStoreFileTypeRoute
    }

  lazy val fileStoreFileTypeRoute: Route =
    path(Segment) { fileTypeName =>
      val fileType = FileType(fileTypeName)
      get {
        onSuccess(fileStore.getFileIds(fileType.bucket)) {
          case Left(e)     => complete(StatusCodes.InternalServerError, s"Could not list objects for the given fileType: ${fileType}")
          case Right(list) => complete(s"Result list: $list")
        }
      } ~
        path(Segment) { id =>
          get {
            onSuccess(fileStore.getFile(fileType.bucket, id)) {
              case Left(e)     => complete(StatusCodes.NotFound, s"File of file type ${fileType} with id ${id} was not found.")
              case Right(file) => complete(s"Result list: $file")
            }
          } ~
            put {
              parameters('name.as[Option[String]]) { name =>
                onSuccess(fileStore.putFile(fileType.bucket, Some(id), FileStoreFileMetadata(name.getOrElse(""), fileType), null)) {
                  case Left(e)         => complete(StatusCodes.BadRequest, s"File of file type ${fileType} with id ${id} could not be stored.")
                  case Right(metadata) => complete(s"Resulting metadata: $metadata")
                }
              }
            }
        }
    }
}