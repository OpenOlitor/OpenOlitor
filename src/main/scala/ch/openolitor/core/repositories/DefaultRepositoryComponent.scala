package ch.openolitor.core.repositories

/**
 * Dieses WriteRepository verlinked die jeweiligen Repositories auf das konkrete default Datenbank Backend
 */
trait DefaultWriteRepositoryComponent extends WriteRepositoryComponent {
  //TODO: map to concrete implementation 
  override val stammdatenRepository = null
}