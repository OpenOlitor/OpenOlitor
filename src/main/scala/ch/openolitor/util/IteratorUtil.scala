package ch.openolitor.util

object IteratorUtil {
  implicit class Extension[A](i: Iterator[A]) {
    def takeWhileInclusive(p: A => Boolean) = {
      val (a, b) = i.span(p)
      a ++ (if (b.hasNext) Some(b.next) else None)
    }
  }
}