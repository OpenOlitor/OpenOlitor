package ch.openolitor.core.repositories

trait BaseParameter {
  def parameter[V](value: V)(implicit binder: SqlBinder[V]): Any = binder.apply(value)
}