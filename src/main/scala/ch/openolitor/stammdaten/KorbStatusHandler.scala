package ch.openolitor.stammdaten

import ch.openolitor.stammdaten.models._

trait KorbStatusHandler {
  def calculateKorbStatus(abwCount: Option[Int], guthaben: Int, minGuthaben: Int): KorbStatus = {
    (abwCount, guthaben) match {
      case (Some(abw), _) if abw > 0 => FaelltAusAbwesend
      case (_, guthaben) if guthaben > minGuthaben => WirdGeliefert
      case (_, guthaben) if guthaben <= minGuthaben => FaelltAusSaldoZuTief
    }
  }
}