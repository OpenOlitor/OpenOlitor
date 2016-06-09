package ch.openolitor.core.db.evolution.scripts

object Scripts {
  val current =
    V1Scripts.scripts ++
      OO205_DBScripts.scripts
}