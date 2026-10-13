package com.orbit.app.testing

import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.di.featureModule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Starts Koin with these repositories and the app's view models, for screens whose components get
 * their own view model. Call `stopKoin()` after the test.
 */
internal fun startTestKoin(
    auth: AuthRepository,
    spaces: SpaceRepository,
    items: ItemRepository = InMemoryItemRepository(auth),
) = startKoin {
    modules(
        module {
            single { auth }
            single { spaces }
            single { items }
        },
        featureModule,
    )
}
