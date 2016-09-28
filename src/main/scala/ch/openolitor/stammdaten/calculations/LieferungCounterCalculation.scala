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

object LieferungCounterCalculation {
  def props(implicit sysConfig: SystemConfig, system: ActorSystem): Props = Props(classOf[LieferungCounterCalculation], sysConfig, system)
}

class LieferungCounterCalculation(override val sysConfig: SystemConfig, override val system: ActorSystem) extends BaseCalculation
    with AsyncConnectionPoolContextAware
    with DefaultStammdatenWriteRepositoryComponent
    with StammdatenDBMappings {

  override def calculate(): Unit = {
    DB autoCommit { implicit session =>
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='WirdGeliefert' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_koerbe_zu_liefern=k.counter""".execute.apply()
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='FaelltAusAbwesend' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_abwesenheiten=k.counter""".execute.apply()
      sql"""update ${lieferungMapping.table} l 
		INNER JOIN (SELECT k.lieferung_id, count(k.id) counter from ${korbMapping.table} k WHERE Status='FaelltAusSaldoZuTief' group by k.lieferung_id) k ON l.id=k.lieferung_id
		set l.anzahl_saldo_zu_tief=k.counter""".execute.apply()
    }
  }
}