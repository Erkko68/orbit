package com.orbit.app.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.orbit.app.data.ai.NoopSetupAssistant
import com.orbit.app.data.ai.SetupAssistant
import com.orbit.app.data.local.OrbitDatabase
import com.orbit.app.data.repository.LocalSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.space.SpaceViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

/** Single DI entry point. Android passes `androidContext`; iOS calls it with no config. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, dataModule, domainModule, featureModule)
    }
}

/** Provides a `RoomDatabase.Builder<OrbitDatabase>` plus any platform-only implementations. */
expect val platformModule: Module

val dataModule = module {
    single {
        get<RoomDatabase.Builder<OrbitDatabase>>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<OrbitDatabase>().spaceDao() }
    singleOf(::LocalSpaceRepository) bind SpaceRepository::class
    single { HttpClient { install(ContentNegotiation) { json() } } }
    single { Settings() }
    single<SetupAssistant> { NoopSetupAssistant() }
}

val domainModule = module {
    // Use cases go here.
}

val featureModule = module {
    viewModelOf(::SpaceViewModel)
}
