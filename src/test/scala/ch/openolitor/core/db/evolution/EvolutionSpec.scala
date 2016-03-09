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

class EvolutionSpec extends Specification with Mockito with TestDB with Before {

  val script1 = mock[Script]
  val script2 = mock[Script]
  
  def before: Any = {
    //initialize database
    DB autoCommit { implicit session =>
      sql"create table dbschema(id varchar(36) NOT NULL, revision BIGINT NOT NULL, status varchar(50) NOT NULL, execution_date TIMESTAMP NOT NULL, PRIMARY KEY (id));".execute.apply()
    }
  }
   
   "Evolution" should {
     "apply all scripts when they return with success" in new AutoRollback { 
       script1.execute(any[DBSession]) returns Success(true)
       script2.execute(any[DBSession]) returns Success(true)
       
       val scripts = Seq(script1, script2)
       val evolution = new Evolution(Seq())
           
       val result = evolution.evolve(scripts, 0)
       
       result === Success(2)
       
       there was one (script1).execute(any[DBSession])
       there was one (script2).execute(any[DBSession])
     }  
  }
 }