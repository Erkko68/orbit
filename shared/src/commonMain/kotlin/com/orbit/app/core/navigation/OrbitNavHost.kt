package com.orbit.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.designsystem.component.OrbitStartup
import com.orbit.app.feature.account.ui.AccountScreen
import com.orbit.app.feature.assistant.ui.InterviewScreen
import com.orbit.app.feature.auth.SessionStart
import com.orbit.app.feature.auth.SessionViewModel
import com.orbit.app.feature.auth.ui.CreateAccountScreen
import com.orbit.app.feature.auth.ui.SignInScreen
import com.orbit.app.feature.expense.ui.ExpenseScreen
import com.orbit.app.feature.item.ui.ItemScreen
import com.orbit.app.feature.member.ui.MemberScreen
import com.orbit.app.feature.newspace.ui.NewSpaceScreen
import com.orbit.app.feature.space.ui.SpaceScreen
import com.orbit.app.feature.spacehome.ui.SpaceHomeScreen
import com.orbit.app.feature.spacesettings.ui.SpaceSettingsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The auth gate and the app's routes: signed out, only sign-in; signed in, the spaces, their homes, settings,
 * members and items, each a screen on the stack. Each side gets its own back stack, so signing in or out never leaves a way back across.
 * A signed-in user starts in the space they opened last, or in the list if there is none. An invitation
 * link brings them to the list, which opens the new space screen, ready to join.
 */
@Composable
fun OrbitNavHost(
    sessionViewModel: SessionViewModel = koinViewModel(),
    pendingInvitation: PendingInvitation = koinInject(),
) {
    val start = sessionViewModel.start.collectAsStateWithLifecycle().value
    if (start == null) {
        OrbitStartup()
        return
    }
    key(start) {
        val navController = rememberNavController()
        val invitation by pendingInvitation.code.collectAsStateWithLifecycle()
        LaunchedEffect(invitation, start) {
            if (invitation != null && start != SessionStart.SignedOut) navController.showSpaces()
        }
        val startDestination = when (start) {
            SessionStart.SignedOut -> SignInRoute
            SessionStart.Spaces -> SpacesRoute
            is SessionStart.Space -> SpaceHomeRoute(start.spaceId)
        }
        NavHost(navController = navController, startDestination = startDestination) {
            composable<SignInRoute> { SignInScreen(onCreateAccount = { navController.navigate(CreateAccountRoute) }) }
            composable<CreateAccountRoute> { CreateAccountScreen(onSignIn = { navController.popBackStack() }) }
            composable<SpacesRoute> {
                SpaceScreen(
                    onOpenSpace = { spaceId -> navController.navigate(SpaceHomeRoute(spaceId)) },
                    onOpenAccount = { navController.navigate(AccountRoute) },
                    onNewSpace = { navController.navigate(NewSpaceRoute) { launchSingleTop = true } },
                )
            }
            composable<NewSpaceRoute> {
                NewSpaceScreen(
                    onClose = { navController.popBackStack() },
                    onOpenSpace = { spaceId ->
                        navController.navigate(SpaceHomeRoute(spaceId)) {
                            popUpTo<NewSpaceRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<AccountRoute> {
                AccountScreen(onBack = { navController.popBackStack() })
            }
            composable<SpaceHomeRoute> { entry ->
                val spaceId = entry.toRoute<SpaceHomeRoute>().spaceId
                SpaceHomeScreen(
                    spaceId = spaceId,
                    onBack = { navController.showSpaces() },
                    onOpenAccount = { navController.navigate(AccountRoute) },
                    onOpenItem = { itemId -> navController.navigate(ItemRoute(spaceId = spaceId, itemId = itemId)) },
                    onNewItem = { navController.navigate(ItemRoute(spaceId = spaceId)) },
                    onOpenAssistant = { navController.navigate(InterviewRoute) },
                    onOpenSettings = { navController.navigate(SpaceSettingsRoute(spaceId)) },
                    onOpenExpense = { id -> navController.navigate(ExpenseRoute(spaceId, id)) },
                    onNewExpense = { navController.navigate(ExpenseRoute(spaceId)) },
                )
            }
            composable<SpaceSettingsRoute> { entry ->
                val spaceId = entry.toRoute<SpaceSettingsRoute>().spaceId
                SpaceSettingsScreen(
                    spaceId = spaceId,
                    onBack = { navController.popBackStack() },
                    onOpenMember = { userId -> navController.navigate(MemberRoute(spaceId, userId)) },
                    onGone = { navController.showSpaces() },
                )
            }
            composable<MemberRoute> { entry ->
                val route = entry.toRoute<MemberRoute>()
                MemberScreen(
                    spaceId = route.spaceId,
                    userId = route.userId,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { itemId -> navController.navigate(ItemRoute(route.spaceId, itemId)) },
                )
            }
            composable<ItemRoute> { entry ->
                val route = entry.toRoute<ItemRoute>()
                // A new item's cost comes back here from the expense screen (#300).
                val plannedCost by entry.savedStateHandle.getStateFlow(PLANNED_COST, "").collectAsStateWithLifecycle()
                ItemScreen(
                    spaceId = route.spaceId,
                    itemId = route.itemId,
                    onBack = { navController.popBackStack() },
                    onOpenCost = { draft ->
                        navController.navigate(ExpenseRoute(route.spaceId, itemId = route.itemId, draftCost = draft))
                    },
                    plannedCost = plannedCost,
                    onPlannedCostTaken = { entry.savedStateHandle[PLANNED_COST] = "" },
                    onAddExpense = { navController.navigate(ExpenseRoute(route.spaceId, sourceItemId = route.itemId)) },
                )
            }
            composable<ExpenseRoute> { entry ->
                val route = entry.toRoute<ExpenseRoute>()
                ExpenseScreen(
                    spaceId = route.spaceId,
                    expenseId = route.expenseId,
                    onBack = { navController.popBackStack() },
                    itemId = route.itemId,
                    draftCost = route.draftCost,
                    sourceItemId = route.sourceItemId,
                    onOpenItem = { itemId -> navController.navigate(ItemRoute(route.spaceId, itemId)) },
                    onCostPlanned = { navController.previousBackStackEntry?.savedStateHandle?.set(PLANNED_COST, it) },
                )
            }
            composable<InterviewRoute> {
                // Closing goes back to where the assistant was opened from, which has the manual way; the review board
                // (#75) follows a plan.
                InterviewScreen(
                    onClose = { navController.popBackStack() },
                    onPlanReady = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Shows the list of spaces: the one under the current screen, or a new one if the app opened straight into a space. */
private fun NavController.showSpaces() {
    if (!popBackStack<SpacesRoute>(inclusive = false)) {
        navigate(SpacesRoute) {
            launchSingleTop = true
            popUpTo<SpaceHomeRoute> { inclusive = true }
        }
    }
}
