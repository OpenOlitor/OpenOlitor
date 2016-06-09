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
package ch.openolitor.core.mailservice

import org.specs2.mutable.Specification
import ch.openolitor.core.db.TestDB
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.specs2.specification.Scope
import akka.testkit.TestProbe
import ch.openolitor.core.ConfigLoader
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.MandantConfiguration
import ch.openolitor.core.domain.AggregateRoot._
import org.joda.time.DateTime

class MailRetryHandlerSpec extends Specification {

  "MailRetryHandler" should {

    val handler = new MailRetryHandlerMock

    "return None when nextTry is not yet reached" in {
      val enqueued = MailEnqueued(null, "uid", null, None, DateTime.now().plusSeconds(3), DateTime.now().plusSeconds(50), 1)

      handler.calculateRetryEnqueued(enqueued) must beRight(None)
    }

    "return new MailEnqueued when nextTry is in the past" in {
      val enqueued = MailEnqueued(null, "uid", null, None, DateTime.now().minusSeconds(10), DateTime.now().plusSeconds(50), 1)

      handler.calculateRetryEnqueued(enqueued) must beRight((x: Option[MailEnqueued]) => x must beSome)
    }

    "return RetryFailed when expired" in {
      val enqueued = MailEnqueued(null, "uid", null, None, DateTime.now().plusSeconds(3), DateTime.now().minusSeconds(10), 1)

      handler.calculateRetryEnqueued(enqueued) must beLeft
    }

    "return RetryFailed when max retries reached" in {
      val enqueued = MailEnqueued(null, "uid", null, None, DateTime.now().plusSeconds(3), DateTime.now().plusSeconds(3600), 5)

      handler.calculateRetryEnqueued(enqueued) must beLeft
    }
  }
}

class MailRetryHandlerMock extends DefaultMailRetryHandler {
  val maxNumberOfRetries = 5
}