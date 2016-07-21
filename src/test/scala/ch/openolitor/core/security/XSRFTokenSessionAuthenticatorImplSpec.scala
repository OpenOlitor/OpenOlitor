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

import scala.concurrent.duration._
import org.specs2.mutable._
import spray.caching._
import spray.routing._
import spray.http.HttpHeaders._
import ch.openolitor.core.models.PersonId
import spray.http.{ DateTime => SprayDateTime, _ }
import org.specs2.time.NoTimeConversions
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime

class XSRFTokenSessionAuthenticatorImplSpec extends Specification with NoTimeConversions {
  import AuthCookies._

  val timeout = 5 seconds
  val retries = 3

  "Authenticate" should {
    val token = "asdasd"
    val personId = PersonId(123)
    val subject = Subject(token, personId, None)

    "Succeed without time limitation" in {
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(None)

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, token)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.right.e must beRight(subject)).await(retries, timeout)
    }

    "Succeed with time limitation" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))

      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.right.e must beRight(subject)).await(retries, timeout)
    }

    "Fail when missing cookie param" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val headers = List()
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft(AuthenticatorRejection("Kein XSRF-Token im Cookie gefunden"))).await(retries, timeout)
    }.pendingUntilFixed("cookie is ignored for now")

    "Fail when missing header param" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val headers = List(cookie)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft(AuthenticatorRejection("Kein XSRF-Token im Header gefunden"))).await(retries, timeout)
    }

    "Fail when header param does not contain correct time" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::asdasdsd"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft(AuthenticatorRejection("Ungültiges Datumsformat im Header:asdasdsd"))).await(retries, timeout)
    }

    "Fail when header token and cookie token mismatch" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, "asasdfas")))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft(AuthenticatorRejection(s"Cookie und Header Token weichen voneinander ab 'asasdfas' != '$token'"))).await(retries, timeout)
    }.pendingUntilFixed("cookie is ignored for now")

    "Fail when delay exceeded" in {
      val delay = 1 milli
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::asdasdsd"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      // prepare cache
      provider.loginTokenCache(token)(subject)

      Thread.sleep(100)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft).await
    }

    "Fail when no person match token" in {
      val delay = 10 seconds
      val provider = new MockXSRFTokenSessionAuthenticatorProvider(Some(delay))
      val time = DateTime.now.toString
      val headerValue = s"$token::$time"

      // prepare requestcontext
      val cookie = Cookie(Seq(HttpCookie(CsrfTokenCookieName, token)))
      val tokenHeader = RawHeader(CsrfTokenHeaderName, headerValue)
      val headers = List(cookie, tokenHeader)
      val request = HttpRequest(headers = headers)
      val ctx = RequestContext(request, null, null)

      val result = provider.openOlitorAuthenticator.apply(ctx)

      result.map(_.left.e must beLeft(AuthenticatorRejection(s"Keine Person gefunden für token: $token"))).await(retries, timeout)
    }
  }
}

class MockXSRFTokenSessionAuthenticatorProvider(override val maxRequestDelay: Option[Duration]) extends XSRFTokenSessionAuthenticatorProvider {
  override val loginTokenCache: Cache[Subject] = LruCache()
}