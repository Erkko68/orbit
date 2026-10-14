package com.orbit.app.di

import com.orbit.app.data.repository.FirebaseAuthRepository
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.FirestoreSpaceRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.account.AccountViewModel
import com.orbit.app.feature.auth.CreateAccountViewModel
import com.orbit.app.feature.auth.SessionViewModel
import com.orbit.app.feature.auth.SignInViewModel
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.feature.space.card.SpaceCardViewModel
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsViewModel
import com.orbit.app.feature.spacehome.members.SpaceMembersViewModel
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

/** Normal DI entry point. Android passes `androidContext`; iOS calls it with no config. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startOrbitKoin(dataModule, config)
}

/** Local development entry point: all repositories stay in memory, with no Firebase account needed. */
fun initDemoKoin(config: KoinAppDeclaration? = null): KoinApplication = startOrbitKoin(demoDataModule, config)

private fun startOrbitKoin(repositories: Module, config: KoinAppDeclaration?): KoinApplication = startKoin {
    config?.invoke(this)
    modules(platformModule, repositories, domainModule, featureModule)
}

/** Platform-only implementations (camera, notifications, text recognition, file paths). */
expect val platformModule: Module

private val sharedDataModule = module {
    // In-memory stand-in. The Firebase implementation replaces it in #30.
    singleOf(::InMemoryItemRepository) bind ItemRepository::class
    single { Settings() }
}

val dataModule = module {
    includes(sharedDataModule)
    single { Firebase.auth }
    single { Firebase.firestore }
    singleOf(::FirebaseAuthRepository) bind AuthRepository::class
    singleOf(::FirestoreSpaceRepository) bind SpaceRepository::class
}

private val demoDataModule = module {
    includes(sharedDataModule)
    singleOf(::InMemoryAuthRepository) bind AuthRepository::class
    singleOf(::InMemorySpaceRepository) bind SpaceRepository::class
}

val domainModule = module {
    // Use cases go here.
}

val featureModule = module {
    viewModelOf(::AccountViewModel)
    viewModelOf(::SessionViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::CreateAccountViewModel)
    viewModelOf(::SpaceViewModel)
    // The id of these comes from parametersOf in the composable they belong to.
    viewModelOf(::SpaceCardViewModel)
    viewModelOf(::SpaceHomeViewModel)
    viewModelOf(::SpaceItemsViewModel)
    viewModelOf(::SpaceMembersViewModel)
}
