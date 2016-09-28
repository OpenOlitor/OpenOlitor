package ch.openolitor.stammdaten.calculations

import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorLogging
import ch.openolitor.core.calculations.BaseCalculation
import scala.concurrent.duration._
import ch.openolitor.core.calculations.Calculations.StartCalculation
import scala.concurrent.ExecutionContext.Implicits.global
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc._
import ch.openolitor.stammdaten.repositories.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.core.db.AsyncConnectionPoolContextAware
import scala.util.Failure
import ch.openolitor.core.calculations.Calculations.CalculationResult
import org.joda.time.DateTime

object KorbStatusCalculation {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[KorbStatusCalculation], sysConfig, system)
}

class KorbStatusCalculation(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BaseCalculation
    with AsyncConnectionPoolContextAware
    with DefaultStammdatenWriteRepositoryComponent
    with StammdatenDBMappings {

  override def calculate(): Unit = {
    DB autoCommit { implicit session =>
      sql"""update ${korbMapping.table} k inner join ${abwesenheitMapping.table} a on k.abo_id=a.abo_id and k.lieferung_id=a.lieferung_id 
      	set Status='FaelltAusAbwesend'""".execute.apply()
    }
  }
}