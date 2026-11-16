package com.orbit.app.di

import com.orbit.app.data.repository.ServerAssistantRepository
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertIs

class AssistantModuleTest {
    @Test
    fun theAppTalksToTheAssistantServer() {
        // The overrides keep Firebase out of the test; the assistant comes from the app's own data module.
        val auth = InMemoryAuthRepository()
        val app = koinApplication {
            modules(
                dataModule,
                module {
                    single<AuthRepository> { auth }
                    single<SpaceRepository> { InMemorySpaceRepository(auth) }
                    single<ItemRepository> { InMemoryItemRepository(auth) }
                },
            )
        }

        assertIs<ServerAssistantRepository>(app.koin.get<AssistantRepository>())
        app.close()
    }
}
