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
package ch.openolitor.util

import org.joda.time.DateTime
import org.joda.time.LocalDate
import com.github.nscala_time.time.DurationBuilder
import scala.concurrent.duration.{ Duration, FiniteDuration }

object DateTimeUtil {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  implicit def localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  /**
   * Convert a `java.time.Duration` to Scala's `Duration`
   */
  implicit def javaDurationToDuration(d: java.time.Duration): Duration =
    Duration.fromNanos(d.toNanos)

  /**
   * Convert a `java.time.Duration` to `org.joda.time.Duration`
   */
  implicit def javaDurationToJodaDuration(d: java.time.Duration): org.joda.time.Duration =
    org.joda.time.Duration.millis(d.toMillis)

  implicit def nscalaDurationBuilderToFiniteDuration(x: DurationBuilder): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(x.standardDuration.getMillis * 1000000)
}
