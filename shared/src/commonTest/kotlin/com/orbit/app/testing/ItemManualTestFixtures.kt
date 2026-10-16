package com.orbit.app.testing

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Test-only repositories for an interactive session; none are registered in the production app. */
internal data class ItemManualTestFixtures(
    val auth: AuthRepository,
    val spaces: SpaceRepository,
    val items: ItemRepository,
)

internal suspend fun createItemManualTestFixtures(): ItemManualTestFixtures {
    val auth = InMemoryAuthRepository()
    val spaces = InMemorySpaceRepository(auth)
    val items = InMemoryItemRepository(auth)
    auth.register("ana@example.test", "test-only-password", "Ana")
    val spaceId = spaces.createSpace("Flat 4B", accent = 0)
    items.create(spaceId, NewItem("Book plumber", description = "Kitchen leak", preset = Preset.TASK))
    items.create(
        spaceId,
        NewItem(
            "Groceries",
            preset = Preset.SHOPPING_LIST,
            modules = ItemModules(
                checklist = Checklist(
                    listOf("Milk", "Eggs", "Pasta", "Tomatoes", "Coffee", "Dish soap").mapIndexed { index, text ->
                        ChecklistEntry("entry-$index", text, checked = index < 2)
                    },
                )
            ),
        ),
    )
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    items.create(
        spaceId,
        NewItem(
            "Dinner at 20:00",
            description = "Dinner together at home",
            preset = Preset.EVENT,
            modules = ItemModules(schedule = Schedule(today, time = LocalTime(20, 0))),
        ),
    )
    return ItemManualTestFixtures(auth, spaces, items)
}
