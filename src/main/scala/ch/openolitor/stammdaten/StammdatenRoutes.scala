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

trait StammdatenRoutes extends HttpService with ActorReferences with AsyncConnectionPoolContextAware with SprayDeserializers {
  self: StammdatenRepositoryComponent =>

  implicit val abotypIdParamConverter = string2BaseIdConverter[AbotypId](AbotypId.apply)
  implicit val abotypIdPath = string2BaseIdPathMatcher[AbotypId](AbotypId.apply)
  implicit val personIdPath = string2BaseIdPathMatcher[PersonId](PersonId.apply)

  import StammdatenJsonProtocol._
  import EntityStore._

  implicit val timeout = Timeout(5.seconds)

  //TODO: get real userid from login
  def userId: UserId = Boot.systemUserId

  lazy val stammdatenRoute = aboTypenRoute ~ personenRoute

  def create[E, I <: BaseId](idFactory: UUID => I)(implicit um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I]) = {
    entity(as[E]) { entity =>
      //create abotyp
      onSuccess(entityStore ? EntityStore.InsertEntityCommand(userId, entity)) {
        case event: EntityInsertedEvent =>
          //load entity          
          complete(idFactory(event.id))
        case x =>
          complete(StatusCodes.BadRequest, s"No id generated:$x")
      }
    }
  }

  def list[R](f: => Future[R])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch list of something
    onSuccess(f) { result =>
      complete(result)
    }
  }

  def detail[R](f: => Future[Option[R]])(implicit tr: ToResponseMarshaller[R]) = {
    //fetch detail of something
    onSuccess(f) { result =>
      result.map(complete(_)).getOrElse(complete(StatusCodes.NotFound))
    }
  }

  lazy val personenRoute =
    path("personen") {
      get(list(readRepository.getPersonen)) ~
        post(create[PersonCreate, PersonId](PersonId.apply _))
    } ~
      path("personen" / personIdPath) { id =>
        get(detail(readRepository.getPersonDetail(id))) ~
          (put | post) {
            complete("")
          } ~
          delete {
            complete("")
          }
      }

  lazy val aboTypenRoute =
    path("abotypen") {
      get(list(readRepository.getAbotypen)) ~
        post(create[AbotypCreate, AbotypId](AbotypId.apply _))
    } ~
      path("abotypen" / abotypIdPath) { id =>
        get(detail(readRepository.getAbotypDetail(id))) ~
          (put | post) {
            entity(as[AbotypUpdate]) { abotyp =>
              //update abotyp
              onSuccess(entityStore ? EntityStore.UpdateEntityCommand(userId, id, abotyp)) { result =>
                //refetch entity
                onSuccess(readRepository.getAbotypDetail(id)) { abotyp =>
                  complete(abotyp)
                }
              }
            }
          } ~
          delete {
            onSuccess(entityStore ? EntityStore.DeleteEntityCommand(userId, id)) { result =>
              complete("")
            }
          }
      }
}