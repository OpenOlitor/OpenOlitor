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
package ch.openolitor.buchhaltung

import org.specs2.mutable._
import ch.openolitor.stammdaten.models._
import ch.openolitor.buchhaltung.models._
import org.joda.time.DateTime
import ch.openolitor.core.MandantConfiguration
import ch.openolitor.core.SystemConfig
import akka.actor.ActorSystem
import ch.openolitor.core.models.PersonId

class BuchhaltungInsertServiceSpec extends Specification {
  "BuchhaltungInsertService" should {
    val config = SystemConfig(MandantConfiguration(
      "", "", "", 0, 0, Map(), null
    ), null, null)

    val kontoDaten = KontoDaten(KontoDatenId(123), Some("CH1223"), Some("777777777"), Some("123456"), new DateTime, PersonId(123), new DateTime, PersonId(123))

    val service = new MockBuchhaltungInsertService(config, null, 6, 5)

    "calculate correct checksum according to definition matrix" in {
      service.calculateChecksum("00000001290381204712347234".toList map (_.asDigit)) === 0
    }

    "calculate correct checksum according to definition matrix" in {
      service.calculateChecksum("00000001290381204712347230".toList map (_.asDigit)) === 3
    }

    "calculate correct checksum according to definition matrix" in {
      service.calculateChecksum("00000001290381204712347233".toList map (_.asDigit)) === 5
    }

    "calculate correct checksum according to definition matrix" in {
      service.calculateChecksum("00000001290381204712347232".toList map (_.asDigit)) === 7
    }

    "calculate correct checksum according to definition matrix" in {
      service.calculateChecksum("00000001290381204712347231".toList map (_.asDigit)) === 9
    }

    "calculate correct referenzNummer" in {
      service.generateReferenzNummer(kontoDaten, KundeId(123), RechnungId(777)) === "123456000000000001230007772"
    }

    "calculate correct referenzNummer with kundeId length same as allowed size" in {
      service.generateReferenzNummer(kontoDaten, KundeId(12345), RechnungId(777)) === "123456000000000123450007773"
    }

    "calculate correct esrNummer" in {
      val referenzNummer = service.generateReferenzNummer(kontoDaten, KundeId(321), RechnungId(555))
      service.generateEsrNummer(kontoDaten, 20.57, CHF, referenzNummer) === "0100000020573>123456000000000003210005556+ 777777777>"
    }
  }

  "BuchhaltungInsertService" should {
    val config = SystemConfig(MandantConfiguration(
      "", "", "", 0, 0, Map(), null
    ), null, null)

    val kontoDaten = KontoDaten(KontoDatenId(123), Some("CH1223"), Some("132"), Some(""), new DateTime, PersonId(123), new DateTime, PersonId(123))

    val service = new MockBuchhaltungInsertService(config, null, 6, 5)

    "fill teilnehmernummer from right" in {
      val referenzNummer = service.generateReferenzNummer(kontoDaten, KundeId(321), RechnungId(555))
      service.generateEsrNummer(kontoDaten, 20.57, CHF, referenzNummer) === "0100000020573>000000000000000003210005550+ 000000132>"
    }
  }
}

class MockBuchhaltungInsertService(sysConfig: SystemConfig, override val system: ActorSystem, RechnungIdLengthP: Int,
    KundeIdLengthP: Int) extends DefaultBuchhaltungInsertService(sysConfig, system) {
  override lazy val RechnungIdLength = RechnungIdLengthP
  override lazy val KundeIdLength = KundeIdLengthP
}
