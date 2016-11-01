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

import spray.routing._
import spray.http._
import spray.http.StatusCodes._
import spray.http.ContentTypes._
import spray.json.DefaultJsonProtocol._
import Directives._
import spray.httpx.SprayJsonSupport._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.security.AuthenticatorRejection

case class RejectionMessage(message: String, cause: String)

/** Custom RejectionHandler for dealing with AuthenticatorRejections. */
object OpenOlitorRejectionHandler extends LazyLogging with BaseJsonProtocol {
  import spray.httpx.marshalling

  def apply(corsSupport: CORSSupport): RejectionHandler = RejectionHandler {
    case rejections if rejections.find(_.isInstanceOf[AuthenticatorRejection]).isDefined =>
      val reason = rejections.find(_.isInstanceOf[AuthenticatorRejection]).get.asInstanceOf[AuthenticatorRejection].reason

      logger.debug(s"AuthenticatorRejection: $reason")

      complete(HttpResponse(Unauthorized).withHeaders(
        corsSupport.allowCredentialsHeader :: corsSupport.allowOriginHeader :: corsSupport.exposeHeaders :: corsSupport.optionsCorsHeaders
      ).withEntity(marshalling.marshalUnsafe(RejectionMessage("Unauthorized", ""))))

    case others if RejectionHandler.Default.isDefinedAt(others) =>
      ctx => RejectionHandler.Default(others) {
        ctx.withHttpResponseMapped {
          case resp @ HttpResponse(_, HttpEntity.NonEmpty(_, msg), _, _) =>
            resp.withHeaders(
              corsSupport.allowCredentialsHeader :: corsSupport.allowOriginHeader :: corsSupport.exposeHeaders :: corsSupport.optionsCorsHeaders
            ).withEntity(marshalling.marshalUnsafe(RejectionMessage(msg.asString, "")))
        }
      }
  }
}
