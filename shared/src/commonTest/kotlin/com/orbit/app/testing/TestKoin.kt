package com.orbit.app.testing

import com.orbit.app.core.network.Connectivity
import com.orbit.app.core.notification.NotificationAccess
import com.orbit.app.core.notification.NotificationScheduler
import com.orbit.app.core.notification.ReminderSync
import com.orbit.app.core.share.TextSharer
import com.orbit.app.core.speech.Dictation
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.di.featureModule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.PlanRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Starts Koin with these repositories and the app's view models, for screens whose components get
 * their own view model. Call `stopKoin()` after the test.
 */
internal fun startTestKoin(
    auth: AuthRepository,
    spaces: SpaceRepository,
    items: ItemRepository = InMemoryItemRepository(auth),
    lastSpace: LastSpaceRepository = InMemoryLastSpaceRepository(),
    invitations: InvitationRepository = InMemoryInvitationRepository(auth),
    sharer: TextSharer = TextSharer {},
    assistant: AssistantRepository = ScriptedAssistantRepository(emptyList()),
    dictation: Dictation = FakeDictation(),
    expenses: ExpenseRepository = InMemoryExpenseRepository(auth),
    plans: PlanRepository = InMemoryPlanRepository(spaces, items),
    connectivity: Connectivity = FakeConnectivity(),
    notifications: NotificationAccess = FakeNotificationAccess(),
    scheduler: NotificationScheduler = RecordingNotificationScheduler(),
    settings: Settings = MapSettings(),
) = startKoin {
    modules(
        module {
            single { auth }
            single { spaces }
            single { items }
            single { lastSpace }
            single { invitations }
            single { sharer }
            single { assistant }
            single { dictation }
            single { expenses }
            single { plans }
            single { connectivity }
            single { notifications }
            single { scheduler }
            single { settings }
            singleOf(::ReminderSync)
        },
        featureModule,
    )
}
