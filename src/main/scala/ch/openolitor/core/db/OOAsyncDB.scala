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
package ch.openolitor.core.db

import scalikejdbc.async._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
 * This asyncdb case class supports dealing with multiple connectionpools within the same vm
 */
case class OOAsyncDB(context: MultipleAsyncConnectionPoolContext, name: Any = AsyncConnectionPool.DEFAULT_NAME) {
  /**
   * Provides a code block which have a connection from ConnectionPool and passes it to the operation.
   *
   * @param op operation
   * @tparam A return type
   * @return a future value
   */
  def withPool[A](op: (SharedAsyncDBSession) => Future[A]): Future[A] = {
    op.apply(sharedSession)
  }

  /**
   * Provides a shared session.
   *
   * @return shared session
   */
  def sharedSession: SharedAsyncDBSession = {
    SharedAsyncDBSession(context.get(name).borrow())
  }

  /**
   * Provides a future world within a transaction.
   *
   * @param op operation
   * @param cxt execution context
   * @tparam A return type
   * @return a future value
   */
  def localTx[A](op: (TxAsyncDBSession) => Future[A])(implicit cxt: ExecutionContext): Future[A] = {
    context.get(name).borrow().toNonSharedConnection()
      .map { nonSharedConnection => TxAsyncDBSession(nonSharedConnection) }
      .flatMap { tx => AsyncTx.inTransaction[A](tx, op) }
  }
}

object OOAsyncDB {

  implicit def contextToSession(implicit context: MultipleAsyncConnectionPoolContext): AsyncDBSession = OOAsyncDB(context).sharedSession

}