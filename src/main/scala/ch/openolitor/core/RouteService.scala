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
package ch.openolitor.core

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import ch.openolitor.helloworld.HelloWorldRoutes
import ch.openolitor.stammdaten.StammdatenRoutes
import spray.routing.HttpService
import ch.openolitor.stammdaten.DefaultStammdatenRepositoryComponent
import ch.openolitor.core._
import akka.actor.ActorSystem
import ch.openolitor.core.models._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import java.util.UUID
import ch.openolitor.core.domain._
import ch.openolitor.core.domain.EntityStore._
import akka.pattern.ask
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http._
import spray.http.StatusCodes
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpHeaders.Location
import spray.json._
import ch.openolitor.core.BaseJsonProtocol.IdResponse
import stamina.Persister
import stamina.json.JsonPersister

object RouteServiceActor {
  def props(entityStore: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[RouteServiceActor], entityStore, sysConfig, system)
}

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class RouteServiceActor(override val entityStore: ActorRef, override val sysConfig: SystemConfig, override val system: ActorSystem)
  extends Actor with ActorReferences
  with DefaultRouteService
  with HelloWorldRoutes
  with StammdatenRoutes
  with DefaultStammdatenRepositoryComponent
  with CORSSupport
  with BaseJsonProtocol{

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  val receive = runRoute(cors(helloWorldRoute ~ stammdatenRoute))
}

// this trait defines our service behavior independently from the service actor
trait DefaultRouteService extends HttpService with ActorReferences with BaseJsonProtocol {

  val userId: UserId
  implicit val timeout = Timeout(5.seconds)

  def create[E, I <: BaseId](idFactory: UUID => I)(implicit um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], persister:Persister[EntityInsertedEvent[E], _]) = {
    requestInstance { request =>
      entity(as[E]) { entity =>
        created(request)(entity)
      }
    }
  }

  def created[E, I <: BaseId](request: HttpRequest)(entity: E)(implicit persister:Persister[EntityInsertedEvent[E], _]) = {
    //create entity
    onSuccess(entityStore ? EntityStore.InsertEntityCommand(userId, entity)) {
      case event: EntityInsertedEvent[_] =>
        respondWithHeaders(Location(request.uri.withPath(request.uri.path / event.id.toString))) {
          respondWithStatus(StatusCodes.Created) {
            complete(IdResponse(event.id.toString).toJson.compactPrint)
          }
        }
      case x =>
        complete(StatusCodes.BadRequest, s"No id generated:$x")
    }
  }

  def update[E, I <: BaseId](id: I)(implicit um: FromRequestUnmarshaller[E],
    tr: ToResponseMarshaller[I], persister:Persister[EntityUpdatedEvent[I,E], _]) = {
    entity(as[E]) { entity => updated(id, entity) }
  }

  def updated[E, I <: BaseId](id: I, entity: E)(implicit persister:Persister[EntityUpdatedEvent[I, E], _]) = {
    //update entity
    onSuccess(entityStore ? EntityStore.UpdateEntityCommand(userId, id, entity)) { result =>
      complete(StatusCodes.Accepted, "")
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

  /**
   * @persister declare format to ensure that format exists for persising purposes
   */
  def remove[I <: BaseId](id: I)(implicit persister:Persister[EntityDeletedEvent[I], _]) = {
    onSuccess(entityStore ? EntityStore.DeleteEntityCommand(userId, id)) { result =>
      complete("")
    }
  }
}