package com.orbit.app.di

import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.notification.ReminderSync
import com.orbit.app.core.notification.permission.NotificationPermissionViewModel
import com.orbit.app.data.repository.FirebaseAuthRepository
import com.orbit.app.data.repository.FirestoreExpenseRepository
import com.orbit.app.data.repository.FirestoreInvitationRepository
import com.orbit.app.data.repository.FirestoreItemRepository
import com.orbit.app.data.repository.FirestoreSpaceRepository
import com.orbit.app.data.repository.ServerAssistantRepository
import com.orbit.app.data.repository.SettingsLastSpaceRepository
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.account.AccountViewModel
import com.orbit.app.feature.expense.ExpenseViewModel
import com.orbit.app.feature.spacehome.expenses.SpaceExpensesViewModel
import com.orbit.app.feature.assistant.InterviewViewModel
import com.orbit.app.feature.assistant.dictation.DictationViewModel
import com.orbit.app.feature.assistant.consent.AiConsentViewModel
import com.orbit.app.feature.auth.CreateAccountViewModel
import com.orbit.app.feature.auth.SessionViewModel
import com.orbit.app.feature.auth.SignInViewModel
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.history.ItemHistoryViewModel
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.feature.member.activity.MemberActivityViewModel
import com.orbit.app.feature.newspace.NewSpaceViewModel
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.feature.space.card.SpaceCardViewModel
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsViewModel
import com.orbit.app.feature.spacesettings.SpaceSettingsViewModel
import com.orbit.app.feature.spacesettings.invite.InviteViewModel
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
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
        modules(platformModule, dataModule, featureModule)
    }
}

/** Platform-only implementations (camera, notifications, text recognition, file paths, Google sign-in). */
expect val platformModule: Module

val dataModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    singleOf(::FirebaseAuthRepository) bind AuthRepository::class
    singleOf(::FirestoreSpaceRepository) bind SpaceRepository::class
    singleOf(::FirestoreItemRepository) bind ItemRepository::class
    singleOf(::FirestoreExpenseRepository) bind ExpenseRepository::class
    singleOf(::FirestoreInvitationRepository) bind InvitationRepository::class
    single<AssistantRepository> {
        // A small model on a CPU can take a while to answer a question.
        val client = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 180_000 } }
        ServerAssistantRepository(client, get())
    }
    single { Settings() }
    singleOf(::SettingsLastSpaceRepository) bind LastSpaceRepository::class
    singleOf(::ReminderSync)
}

val featureModule = module {
    viewModelOf(::ExpenseViewModel)
    viewModelOf(::SpaceExpensesViewModel)
    single { PendingInvitation() }
    viewModelOf(::AccountViewModel)
    viewModelOf(::InterviewViewModel)
    viewModelOf(::DictationViewModel)
    viewModelOf(::AiConsentViewModel)
    viewModelOf(::SessionViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::CreateAccountViewModel)
    viewModelOf(::ItemViewModel)
    viewModelOf(::ItemHistoryViewModel)
    viewModelOf(::NewSpaceViewModel)
    viewModelOf(::NotificationPermissionViewModel)
    viewModelOf(::SpaceViewModel)
    // The id of these comes from parametersOf in the composable they belong to.
    viewModelOf(::SpaceCardViewModel)
    viewModelOf(::SpaceHomeViewModel)
    viewModelOf(::SpaceItemsViewModel)
    viewModelOf(::SpaceMembersViewModel)
    viewModelOf(::InviteViewModel)
    viewModelOf(::MemberActivityViewModel)
    viewModelOf(::SpaceSettingsViewModel)
}
