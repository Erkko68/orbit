package com.orbit.app.di

import com.orbit.app.data.repository.ServerAssistantRepository
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertIs

class AssistantModuleTest {
    @Test
    fun theAppTalksToTheAssistantServer() {
        // The auth override keeps Firebase out of the test; the assistant comes from the app's own data module.
        val app = koinApplication {
            modules(dataModule, module { single<AuthRepository> { InMemoryAuthRepository() } })
        }

        assertIs<ServerAssistantRepository>(app.koin.get<AssistantRepository>())
        app.close()
    }
}
