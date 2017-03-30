package ch.openolitor.core.repositories

import ch.openolitor.core.ActorSystemReference

trait CoreReadRepositoryComponent {
  val coreReadRepository: CoreReadRepository
}

trait DefaultCoreReadRepositoryComponent extends CoreReadRepositoryComponent with ActorSystemReference {
  override val coreReadRepository: CoreReadRepository = new CoreReadRepositoryImpl(system)
}