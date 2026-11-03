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
import com.orbit.app.feature.item.ui.ItemScreen
import com.orbit.app.feature.newspace.ui.NewSpaceScreen
import com.orbit.app.feature.space.ui.SpaceScreen
import com.orbit.app.feature.spacehome.ui.SpaceHomeScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The auth gate and the app's routes: signed out, only sign-in; signed in, the spaces and their
 * homes. Each side gets its own back stack, so signing in or out never leaves a way back across.
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
                )
            }
            composable<ItemRoute> { entry ->
                val route = entry.toRoute<ItemRoute>()
                ItemScreen(
                    spaceId = route.spaceId,
                    itemId = route.itemId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<InterviewRoute> {
                // The manual path is wherever the assistant was opened from; the review board (#75) follows a plan.
                InterviewScreen(
                    onClose = { navController.popBackStack() },
                    onManual = { navController.popBackStack() },
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
