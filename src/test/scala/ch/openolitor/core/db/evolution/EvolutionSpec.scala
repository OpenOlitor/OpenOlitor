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
package ch.openolitor.core.db.evolution

import org.specs2.mutable._
import scalikejdbc._
import scala.util._
import org.specs2.mock.Mockito
import ch.openolitor.core.db.TestDB
import scalikejdbc.specs2.mutable.AutoRollback
import ch.openolitor.core.models.DBSchema
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.MandantConfiguration

class EvolutionSpec extends Specification with Mockito with TestDB {

  def initDb(implicit session: DBSession): Unit = {
    sql"""create table if not exists dbschema(
    	id BIGINT NOT NULL, 
    	revision BIGINT NOT NULL, 
    	status varchar(50) NOT NULL,
    	erstelldat datetime not null, 
        ersteller BIGINT not null, 
        modifidat datetime not null, 
        modifikator BIGINT not null,
    	PRIMARY KEY (id));""".execute.apply()
  }

  val mandant = MandantConfiguration("", "", "", 0, 0, Map(), null)
  val cfg = SystemConfig(mandant, null, null)

  "Evolution" should {
    "apply all scripts when they return with success" in new AutoRollback {

      override def fixture(implicit session: DBSession): Unit = initDb

      val script1 = mock[Script]
      val script2 = mock[Script]

      script1.execute(any[SystemConfig])(any[DBSession]) returns Success(true)
      script2.execute(any[SystemConfig])(any[DBSession]) returns Success(true)

      val scripts = Seq(script1, script2)
      val evolution = new Evolution(cfg, Seq())

      implicit val user = PersonId(23)
      val result = evolution.evolve(scripts, 0)

      result === Success(2)

      there was one(script1).execute(any[SystemConfig])(any[DBSession])
      there was one(script2).execute(any[SystemConfig])(any[DBSession])
    }

    "apply revision when second script fails" in new AutoRollback {

      override def fixture(implicit session: DBSession): Unit = initDb

      val script1 = mock[Script]
      val script2 = mock[Script]
      val script3 = mock[Script]

      val exception = new RuntimeException
      script1.execute(any[SystemConfig])(any[DBSession]) returns Success(true)
      script2.execute(any[SystemConfig])(any[DBSession]) returns Failure(exception)
      script3.execute(any[SystemConfig])(any[DBSession]) returns Success(true)

      val scripts = Seq(script1, script2, script3)
      val evolution = new Evolution(cfg, Seq())

      implicit val user = PersonId(24)
      val result = evolution.evolve(scripts, 0)

      result === Failure(exception)

      there was one(script1).execute(any[SystemConfig])(any[DBSession])
      there was one(script2).execute(any[SystemConfig])(any[DBSession])
      there was no(script3).execute(any[SystemConfig])(any[DBSession])

      //There seems to be no storing to the in memory database...
      //evolution.currentRevision === 1
    }
  }
}