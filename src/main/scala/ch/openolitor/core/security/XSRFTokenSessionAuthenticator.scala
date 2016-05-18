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
package ch.openolitor.core.security

import spray.routing._
import spray.routing.authentication._
import spray.http._
import StatusCodes._
import Directives._
import scala.concurrent.Future
import spray.caching.Cache
import ch.openolitor.core.models.PersonId
import com.typesafe.scalalogging.LazyLogging
import spray.routing.Rejection
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import scala.util.{ Try, Success => TrySuccess, Failure => TryFailure }

object AuthCookies {
  val CsrfTokenCookieName = "XSRF-TOKEN"
  val CsrfTokenHeaderName = "OO-XSRF-TOKEN"
}

/** If anything goes wrong during authentication, this is the rejection to use. */
case class AuthenticatorRejection(reason: String) extends Rejection

/** Custom RejectionHandler for dealing with AuthenticatorRejections. */
object AuthenticatorRejectionHandler extends LazyLogging {
  import spray.http.StatusCodes._

  def apply(): RejectionHandler = RejectionHandler {
    case rejections if rejections.find(_.isInstanceOf[AuthenticatorRejection]).isDefined =>
      val reason = rejections.find(_.isInstanceOf[AuthenticatorRejection]).get.asInstanceOf[AuthenticatorRejection].reason
      logger.debug(s"Request unauthorized ${reason}")
      complete(Unauthorized)
  }
}

/**
 * Dieser Authenticator authentisiert den Benutzer anhand folgender Kriteren:
 * 1. Im Cookie header sowie im HttpHeader muss das Token mitgeliefert werden
 * 2. Im HttpHeader wird das Token mit der Request Zeit im ISO Format und dem Separator :: mitgeliefert
 * 3. Token im Header und Cookie müssen übereinstimmen
 * 4. Request Zeit darf eine maximale Duration nicht überschreiten (Request kann nicht zu einem späteren Zeitpunkt erneut ausgeführt werden)
 * 5. Im lokalen Cache muss eine PersonId für das entsprechende token gespeichert sein
 */
trait XSRFTokenSessionAuthenticatorProvider extends LazyLogging with TokenCache {
  import AuthCookies._

  /*
   * Configure max time since
   */
  val maxRequestDelay: Option[Duration]
  lazy val openOlitorAuthenticator: ContextAuthenticator[Subject] = new XSRFTokenSessionAuthenticatorImpl(loginTokenCache)

  private class XSRFTokenSessionAuthenticatorImpl(tokenCache: Cache[Subject]) extends ContextAuthenticator[Subject] {

    val noDateTimeValue = new DateTime(0, 1, 1, 0, 0, 0, DateTimeZone.UTC);

    type RequestValidation[V] = EitherT[Future, AuthenticatorRejection, V]

    def apply(ctx: RequestContext): Future[Authentication[Subject]] = {
      logger.debug(s"${ctx.request.uri}:${ctx.request.method}")
      (for {
        cookieToken <- findCookieToken(ctx)
        headerToken <- findHeaderToken(ctx)
        pair <- extractTimeFromHeaderToken(headerToken)
        (token, requestTime) = pair
        valid <- validateCookieAndHeaderToken(cookieToken, token)
        time <- compareRequestTime(requestTime)
        subject <- findSubjectInCache(token)
      } yield subject).run.map(_.toEither)
    }

    private def findCookieToken(ctx: RequestContext): RequestValidation[String] = EitherT {
      Future {
        logger.debug(s"Check cookies:${ctx.request.cookies}")
        ctx.request.cookies.find(_.name == CsrfTokenCookieName).map(_.content.right).getOrElse(AuthenticatorRejection("Kein XSRF-Token im Cookie gefunden").left)
      }
    }

    private def findHeaderToken(ctx: RequestContext): RequestValidation[String] = EitherT {
      Future {
        ctx.request.headers.find(_.name == CsrfTokenHeaderName).map(_.value.right).getOrElse(AuthenticatorRejection("Kein XSRF-Token im Header gefunden").left)
      }
    }

    private def extractTimeFromHeaderToken(headerToken: String): RequestValidation[(String, DateTime)] = EitherT {
      Future {
        headerToken.split("::").toList match {
          case token :: timeString :: Nil =>
            Try(DateTime.parse(timeString)) match {
              case TrySuccess(dateTime) => (token, dateTime).right
              case TryFailure(e) => AuthenticatorRejection(s"Ungültiges Datumsformat im Header:$timeString").left
            }
          case token :: Nil => (token, noDateTimeValue).right
          case x => AuthenticatorRejection(s"Ungüliges Token im Header: $x").left
        }
      }
    }

    private def validateCookieAndHeaderToken(cookieToken: String, headerToken: String): RequestValidation[Boolean] = EitherT {
      Future {
        if (cookieToken == headerToken) true.right
        else AuthenticatorRejection(s"Cookie und Header Token weichen voneinander ab '$cookieToken' != '$headerToken'").left
      }
    }

    private def findSubjectInCache(token: String): RequestValidation[Subject] = EitherT {
      loginTokenCache.get(token) map (_ map (_.right)) getOrElse Future.successful(AuthenticatorRejection(s"Keine Person gefunden für token: $token").left)
    }

    private def compareRequestTime(requestTime: DateTime): RequestValidation[Boolean] = EitherT {
      Future {
        maxRequestDelay map { maxTime =>
          val now = DateTime.now
          val min = now.minus(maxTime.toMillis)
          if (min.isAfter(requestTime)) {
            //request took too long
            AuthenticatorRejection(s"Zeitstempel stimmt nicht überein. Aktuell: $now, Min: $min, Zeitstempel: $requestTime").left
          } else {
            true.right
          }
        } getOrElse (true.right)
      }
    }
  }
}