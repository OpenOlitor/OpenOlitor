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
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.caching._
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.stammdaten.StammdatenReadRepositoryComponent
import ch.openolitor.stammdaten.DefaultStammdatenReadRepositoryComponent
import akka.actor.ActorRef
import ch.openolitor.core.filestore.FileStore
import akka.actor.ActorRefFactory
import ch.openolitor.stammdaten.models.Person
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import ch.openolitor.core.models.PersonId
import java.util.UUID
import ch.openolitor.stammdaten.models.PersonDetail
import ch.openolitor.core.Macros._
import scala.util.Random
import scalaz._
import Scalaz._
import ch.openolitor.util.ConfigUtil._
import ch.openolitor.stammdaten.models.PersonSummary
import ch.openolitor.core.domain.SystemEvents
import spray.routing.authentication.UserPass

trait LoginRouteService extends HttpService with ActorReferences
    with AsyncConnectionPoolContextAware
    with SprayDeserializers
    with DefaultRouteService with LazyLogging with LoginJsonProtocol
    with XSRFTokenSessionAuthenticatorProvider {
  self: StammdatenReadRepositoryComponent =>
  import SystemEvents._

  //TODO: get real userid from login  
  override val personId: PersonId = Boot.systemPersonId

  type EitherFuture[A] = EitherT[Future, LoginFailed, A]

  lazy val loginRoutes = loginRoute

  val loginTokenCache = LruCache[Subject](
    maxCapacity = 10000,
    timeToLive = 1 day,
    timeToIdle = 4 hours
  )
  val secondFactorTokenCache = LruCache[SecondFactor](
    maxCapacity = 1000,
    timeToLive = 20 minutes,
    timeToIdle = 10 minutes
  )

  lazy val config = sysConfig.mandantConfiguration.config
  lazy val requireSecondFactorAuthentication = config.getBooleanOption(s"security.second-factor-auth.require").getOrElse(true)
  lazy val sendSecondFactorEmail = config.getBooleanOption(s"security.second-factor-auth.send-email").getOrElse(true)
  override lazy val maxRequestDelay: Option[Duration] = config.getLongOption(s"security.max-request-delay").map(_ millis)

  val errorUsernameOrPasswordMismatch = LoginFailed("Benutzername oder Passwort stimmen nicht überein")
  val errorTokenOrCodeMismatch = LoginFailed("Code stimmt nicht überein")
  val errorPersonNotFound = LoginFailed("Person konnte nicht gefunden werden")
  val errorPersonLoginNotActive = LoginFailed("Login wurde deaktiviert")

  def loginRoute = pathPrefix("auth") {
    path("login") {
      post {
        requestInstance { request =>
          entity(as[LoginForm]) { form =>
            onSuccess(validateLogin(form).run) {
              case -\/(error) =>
                logger.debug(s"Login failed ${error.msg}")
                complete(StatusCodes.BadRequest, error.msg)
              case \/-(result) =>
                complete(result)
            }
          }
        }
      }
    } ~
      path("secondFactor") {
        post {
          requestInstance { request =>
            entity(as[SecondFactorLoginForm]) { form =>
              onSuccess(validateSecondFactorLogin(form).run) {
                case -\/(error) =>
                  complete(StatusCodes.BadRequest, error.msg)
                case \/-(result) =>
                  complete(result)
              }
            }
          }
        }
      }
  }

  def validateLogin(form: LoginForm): EitherFuture[LoginResult] = {
    for {
      person <- personByEmail(form.email)
      pwdValid <- validatePassword(form.passwort, person)
      personValid <- validatePerson(person)
      result <- handleLoggedIn(person)
    } yield result
  }

  private def transform[A](o: Option[Future[A]]): Future[Option[A]] = {
    o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
  }

  def validateSecondFactorLogin(form: SecondFactorLoginForm): EitherFuture[LoginResult] = {
    for {
      secondFactor <- readTokenFromCache(form)
      person <- personById(secondFactor.personId)
      personValid <- validatePerson(person)
      result <- doLogin(person)
    } yield {
      //cleanup code from cache
      secondFactorTokenCache.remove(form.token)

      result
    }
  }

  private def readTokenFromCache(form: SecondFactorLoginForm): EitherFuture[SecondFactor] = {
    EitherT {
      transform(secondFactorTokenCache.get(form.token)) map {
        case Some(factor @ SecondFactor(form.token, form.code, _)) => factor.right
        case _ => errorTokenOrCodeMismatch.left
      }
    }
  }

  private def personByEmail(email: String): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPersonByEmail(email) map (_ map (_.right) getOrElse {
        logger.debug(s"No person found for email")
        errorUsernameOrPasswordMismatch.left
      })
    }
  }

  private def personById(personId: PersonId): EitherFuture[Person] = {
    EitherT {
      stammdatenReadRepository.getPerson(personId) map (_ map (_.right) getOrElse (errorPersonNotFound.left))
    }
  }

  private def handleLoggedIn(person: Person): EitherFuture[LoginResult] = {
    requireSecondFactorAuthentifcation(person) flatMap {
      case false => doLogin(person)
      case true => sendSecondFactorAuthentication(person)
    }
  }

  private def doLogin(person: Person): EitherFuture[LoginResult] = {
    //generate token
    val token = generateToken
    EitherT {
      loginTokenCache(token)(Subject(person.id, person.rolle)) map { _ =>
        val personSummary = copyTo[Person, PersonSummary](person)

        eventStore ! PersonLoggedIn(person.id, org.joda.time.DateTime.now)

        LoginResult(LoginOk, token, personSummary).right
      }
    }
  }

  private def sendSecondFactorAuthentication(person: Person): EitherFuture[LoginResult] = {
    for {
      secondFactor <- generateSecondFactor(person)
      emailSent <- sendEmail(secondFactor, person)
    } yield {
      val personSummary = copyTo[Person, PersonSummary](person)
      LoginResult(LoginSecondFactorRequired, secondFactor.token, personSummary)
    }
  }

  private def generateSecondFactor(person: Person): EitherFuture[SecondFactor] = {
    EitherT {
      val token = generateToken
      val code = generateCode
      secondFactorTokenCache(token)(SecondFactor(token, code, person.id)) map (_.right)
    }
  }

  private def sendEmail(secondFactor: SecondFactor, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      logger.debug(s"=====================================================================")
      logger.debug(s"| Send Email to: ${person.email}")
      logger.debug(s"---------------------------------------------------------------------")
      logger.debug(s"| Token: ${secondFactor.token}")
      logger.debug(s"| Code: ${secondFactor.code}")
      logger.debug(s"=====================================================================")

      //TODO: bind to email service

      true.right
    }
  }

  private def requireSecondFactorAuthentifcation(person: Person): EitherFuture[Boolean] = EitherT {
    requireSecondFactorAuthentication match {
      case false => Future.successful(false.right)
      case true if (person.rolle.isEmpty) => Future.successful(true.right)
      case true => stammdatenReadRepository.getProjekt map {
        case None => true.right
        case Some(projekt) => projekt.twoFactorAuthentication.get(person.rolle.get).map(_.right).getOrElse(true.right)
      }
    }
  }

  private def validatePassword(password: String, person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.passwort map { pwd =>
        BCrypt.checkpw(password, new String(pwd)) match {
          case true => true.right
          case false =>
            logger.debug(s"Password mismatch")
            errorUsernameOrPasswordMismatch.left
        }
      } getOrElse {
        logger.debug(s"No password for user")
        errorUsernameOrPasswordMismatch.left
      }
    }
  }

  private def validatePerson(person: Person): EitherFuture[Boolean] = EitherT {
    Future {
      person.loginAktiv match {
        case true => true.right
        case false => errorPersonLoginNotActive.left
      }
    }
  }

  private def generateToken = UUID.randomUUID.toString
  private def generateCode = (Random.alphanumeric take 6).mkString.toLowerCase

  /**
   * Validate user password used by basic authentication. Using basic auth we never to a two factor
   * authentication
   */
  def basicAuthValidation(userPass: Option[UserPass]): Future[Option[Subject]] = {
    logger.debug(s"Perform basic authentication")
    (for {
      up <- validateUserPass(userPass)
      person <- personByEmail(up.user)
      pwdValid <- validatePassword(up.pass, person)
      personValid <- validatePerson(person)
      result <- doLogin(person)
    } yield Subject(person.id, person.rolle)).run.map(_.toOption)
  }

  private def validateUserPass(userPass: Option[UserPass]): EitherFuture[UserPass] = EitherT {
    Future {
      userPass map (_.right) getOrElse (errorUsernameOrPasswordMismatch.left)
    }
  }
}

class DefaultLoginRouteService(
  override val entityStore: ActorRef,
  override val eventStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val fileStore: FileStore,
  override val actorRefFactory: ActorRefFactory
)
    extends LoginRouteService
    with DefaultStammdatenReadRepositoryComponent