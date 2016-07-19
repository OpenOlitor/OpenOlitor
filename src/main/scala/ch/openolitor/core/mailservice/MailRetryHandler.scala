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

import org.joda.time.DateTime
import scala.math._
import scala.concurrent.duration.Duration

case class RetryFailed(expired: Boolean)

trait MailRetryHandler {

  val maxNumberOfRetries: Int
  def calculateRetryEnqueued(enqueued: MailEnqueued): Either[RetryFailed, Option[MailEnqueued]]
  def calculateExpires(duration: Option[Duration]): DateTime
}

trait DefaultMailRetryHandler extends MailRetryHandler {
  val RetryTime = List(10, 60, 300, 900, 3600)

  override def calculateRetryEnqueued(enqueued: MailEnqueued): Either[RetryFailed, Option[MailEnqueued]] = {
    val now = DateTime.now()
    val expired = enqueued.expires.isBefore(now)
    if (enqueued.retries < maxNumberOfRetries && !expired) {
      if (enqueued.nextTry.isBefore(now)) {
        Right(Some(enqueued.copy(
          retries = enqueued.retries + 1,
          nextTry = enqueued.nextTry.plusSeconds(
            if (enqueued.retries < RetryTime.size) RetryTime(enqueued.retries) else RetryTime.last
          )
        )))
      } else {
        Right(None)
      }
    } else {
      Left(RetryFailed(expired))
    }
  }

  def calculateExpires(maybeDuration: Option[Duration]): DateTime = {
    maybeDuration map { duration =>
      DateTime.now().plusSeconds(duration.toSeconds.toInt)
    } getOrElse {
      DateTime.now().plusSeconds(RetryTime.sum)
    }
  }
}