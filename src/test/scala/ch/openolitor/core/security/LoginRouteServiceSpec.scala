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

import org.specs2.mutable._
import org.specs2.mock.Mockito
import org.mockito.Matchers.{ eq => isEq, _ }
import ch.openolitor.stammdaten.MockStammdatenReadRepositoryComponent
import akka.actor.ActorRef
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.filestore.FileStore
import akka.actor.ActorRefFactory
import ch.openolitor.core.models.PersonId
import org.joda.time.DateTime
import ch.openolitor.core.db.MultipleAsyncConnectionPoolContext
import scala.concurrent.Future
import org.mindrot.jbcrypt.BCrypt
import ch.openolitor.stammdaten.models._
import scala.concurrent.ExecutionContext
import ch.openolitor.core.domain.SystemEventStore
import akka.testkit.TestActorRef
import ch.openolitor.core.domain.DefaultSystemEventStore
import akka.actor.Actor
import akka.actor.ActorSystem
import spray.caching.Cache
import spray.caching.LruCache

class LoginRouteServiceSpec extends Specification with Mockito {
  val email = "info@test.com"
  val pwd = "pwd"
  val pwdHashed = BCrypt.hashpw(pwd, BCrypt.gensalt())
  val personId = PersonId(1);
  val personKundeActive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, true, Some(pwdHashed.toCharArray), None,
    false, Some(KundenZugang), DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val personAdminActive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, true, Some(pwdHashed.toCharArray), None,
    false, Some(AdministratorZugang), DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val personAdminInactive = Person(personId, KundeId(1), None, "Test", "Test", Some(email), None, None, None, None, 1, false, Some(pwdHashed.toCharArray), None,
    false, Some(AdministratorZugang), DateTime.now, PersonId(1), DateTime.now, PersonId(1))
  val projekt = Projekt(ProjektId(1), "Test", None, None, None, None, None, true, true, true, CHF, 1, 1, Map(AdministratorZugang -> true, KundenZugang -> false), DateTime.now, PersonId(1), DateTime.now, PersonId(1))

  implicit val ctx = MultipleAsyncConnectionPoolContext()

  "Direct login" should {
    "Succeed" in {
      val service = new MockLoginRouteService(false, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(isEq(email))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { x =>
        x.toEither.right.map(_.status) must beRight(LoginOk)
        val token = x.toEither.right.map(_.token).right.get
        service.loginTokenCache.get(token) must beSome
      }.await
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(false, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await
    }

    "Fail on password mismatch" in {
      val service = new MockLoginRouteService(false, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, "wrongPwd")).run

      result.map { _.toEither must beLeft(RequestFailed("Benutzername oder Passwort stimmen nicht überein")) }.await
    }

    "Fail when no person was found" in {
      val service = new MockLoginRouteService(false, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateLogin(LoginForm("anyEmail", pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Benutzername oder Passwort stimmen nicht überein")) }.await
    }
  }

  "Require second factor" should {
    "be disabled when disabled in project settings" in {
      val service = new MockLoginRouteService(true, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personKundeActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await
    }

    "be enabled by project settings" in {
      val service = new MockLoginRouteService(true, false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginSecondFactorRequired) }.await
    }
  }

  "Second factor login" should {
    "Succeed" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await
    }

    "Fail when code does not match" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, "anyCode")).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await
    }

    "Fail when token does not match" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm("anyToken", code)).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await
    }

    "Fail when person not found" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Person konnte nicht gefunden werden")) }.await
    }

    "Ensure token gets deleted after successful login" in {
      val service = new MockLoginRouteService(true, false)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))
      val result1 = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run
      result1.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await

      service.secondFactorTokenCache.get(token) must beNone

      //second try
      val result2 = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run
      result2.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await
    }
  }
}

class MockLoginRouteService(
  requireSecondFactorAuthenticationP: Boolean,
  sendSecondFactorEmailP: Boolean
)
    extends LoginRouteService
    with MockStammdatenReadRepositoryComponent {
  override val entityStore: ActorRef = null
  implicit val system = ActorSystem("test")
  override val eventStore: ActorRef = TestActorRef(new DefaultSystemEventStore(null))
  override val sysConfig: SystemConfig = SystemConfig(null, null, MultipleAsyncConnectionPoolContext())
  override val fileStore: FileStore = null
  override val actorRefFactory: ActorRefFactory = null
  override val loginTokenCache: Cache[Subject] = LruCache()

  override lazy val requireSecondFactorAuthentication = requireSecondFactorAuthenticationP
  override lazy val sendSecondFactorEmail = sendSecondFactorEmailP

}