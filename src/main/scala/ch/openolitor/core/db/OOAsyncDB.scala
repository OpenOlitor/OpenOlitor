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
    println(s"!!!!!!$name:$context")
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