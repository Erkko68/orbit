package com.orbit.app.di

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.space.SpaceViewModel
import com.russhwolf.settings.Settings
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

/** Platform-only implementations (camera, notifications, text recognition, file paths). */
expect val platformModule: Module

val dataModule = module {
    // In-memory stand-ins. The Firebase implementations replace them in #15, #21 and #30.
    singleOf(::InMemoryAuthRepository) bind AuthRepository::class
    singleOf(::InMemorySpaceRepository) bind SpaceRepository::class
    singleOf(::InMemoryItemRepository) bind ItemRepository::class
    single { Settings() }
}

val domainModule = module {
    // Use cases go here.
}

val featureModule = module {
    viewModelOf(::SpaceViewModel)
}
