package ch.openolitor.util

import com.typesafe.config.Config

object ConfigUtil {
  /**
   * Enhanced typesafe config adding support to read config keys as option
   */
  implicit class MyConfig(self: Config) {
    
    private def getOption[T](path:String)(get: String => T):Option[T] = {
      if (self.hasPath(path)) {
        Some(get(path))
      } 
      else {
        None
      }
    }
    
    def getStringOption(path:String):Option[String] = getOption(path)(path => self.getString(path))
    def getIntOption(path:String):Option[Int] = getOption(path)(path => self.getInt(path))
    def getBooleanOption(path:String):Option[Boolean] = getOption(path)(path => self.getBoolean(path))
  }

}