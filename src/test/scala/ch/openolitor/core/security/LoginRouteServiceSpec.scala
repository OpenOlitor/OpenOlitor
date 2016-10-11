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
import org.specs2.time.NoTimeConversions
import org.mockito.Matchers.{ eq => isEq, _ }
import scala.concurrent.duration._
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
import ch.openolitor.core.mailservice.DefaultMailService
import akka.actor.Actor
import akka.actor.ActorSystem
import spray.caching.Cache
import spray.caching.LruCache
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.mailservice.MailServiceMock
import java.util.Locale

class LoginRouteServiceSpec extends Specification with Mockito with NoTimeConversions {
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
  val projekt = Projekt(ProjektId(1), "Test", None, None, None, None, None, true, true, true, CHF, 1, 1, Map(AdministratorZugang -> true, KundenZugang -> false), Locale.GERMAN, DateTime.now, PersonId(1), DateTime.now, PersonId(1))

  implicit val ctx = MultipleAsyncConnectionPoolContext()
  val timeout = 5 seconds
  val retries = 3

  "Direct login" should {

    "Succeed" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(isEq(email))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { x =>
        x.toEither.right.map(_.status) must beRight(LoginOk)
        val token = x.toEither.right.map(_.token).right.get
        service.loginTokenCache.get(token) must beSome
      }.await(3, timeout)
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await(3, timeout)
    }

    "Fail on password mismatch" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, "wrongPwd")).run

      result.map { _.toEither must beLeft(RequestFailed("Benutzername oder Passwort stimmen nicht überein")) }.await(3, timeout)
    }

    "Fail when no person was found" in {
      val service = new MockLoginRouteService(false)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateLogin(LoginForm("anyEmail", pwd)).run

      result.map { _.toEither must beLeft(RequestFailed("Benutzername oder Passwort stimmen nicht überein")) }.await(3, timeout)
    }
  }

  "Require second factor" should {
    "be disabled when disabled in project settings" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personKundeActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await(3, timeout)
    }

    "be enabled by project settings" in {
      val service = new MockLoginRouteService(true)

      service.stammdatenReadRepository.getProjekt(any[ExecutionContext], any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(projekt))
      service.stammdatenReadRepository.getPersonByEmail(any[String])(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateLogin(LoginForm(email, pwd)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginSecondFactorRequired) }.await(3, timeout)
    }
  }

  "Second factor login" should {
    "Succeed" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await(3, timeout)
    }

    "Fail when login not active" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminInactive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Login wurde deaktiviert")) }.await(3, timeout)
    }

    "Fail when code does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, "anyCode")).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when token does not match" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm("anyToken", code)).run

      result.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }

    "Fail when person not found" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(None)

      val result = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run

      result.map { _.toEither must beLeft(RequestFailed("Person konnte nicht gefunden werden")) }.await(3, timeout)
    }

    "Ensure token gets deleted after successful login" in {
      val service = new MockLoginRouteService(true)
      val token = "asdasad"
      val code = "sadfasd"
      val secondFactor = SecondFactor(token, code, personId)

      //store token in cache
      service.secondFactorTokenCache(token)(secondFactor)

      service.stammdatenReadRepository.getPerson(isEq(personId))(any[MultipleAsyncConnectionPoolContext]) returns Future.successful(Some(personAdminActive))
      val result1 = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run
      result1.map { _.toEither.right.map(_.status) must beRight(LoginOk) }.await(3, timeout)

      service.secondFactorTokenCache.get(token) must beNone

      //second try
      val result2 = service.validateSecondFactorLogin(SecondFactorLoginForm(token, code)).run
      result2.map { _.toEither must beLeft(RequestFailed("Code stimmt nicht überein")) }.await(3, timeout)
    }
  }
}

class MockLoginRouteService(
  requireSecondFactorAuthenticationP: Boolean
)
    extends LoginRouteService
    with MockStammdatenReadRepositoryComponent {
  override val entityStore: ActorRef = null
  override val reportSystem: ActorRef = null
  implicit val system = ActorSystem("test")
  override val sysConfig: SystemConfig = SystemConfig(null, null, MultipleAsyncConnectionPoolContext())
  override val eventStore: ActorRef = TestActorRef(new DefaultSystemEventStore(null))
  override val mailService: ActorRef = TestActorRef(new MailServiceMock)
  override val fileStore: FileStore = null
  override val actorRefFactory: ActorRefFactory = null
  override val loginTokenCache: Cache[Subject] = LruCache()
  override val airbrakeNotifier: ActorRef = null

  override lazy val requireSecondFactorAuthentication = requireSecondFactorAuthenticationP
}
