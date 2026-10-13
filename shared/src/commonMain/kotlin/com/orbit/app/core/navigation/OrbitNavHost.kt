package com.orbit.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.orbit.app.feature.auth.SessionViewModel
import com.orbit.app.feature.auth.ui.SignInScreen
import com.orbit.app.feature.space.ui.SpaceScreen
import com.orbit.app.feature.spacehome.ui.SpaceHomeScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * The auth gate and the app's routes: signed out, only sign-in; signed in, the spaces and their
 * homes. Each side gets its own back stack, so signing in or out never leaves a way back across.
 */
@Composable
fun OrbitNavHost(sessionViewModel: SessionViewModel = koinViewModel()) {
    val signedIn = sessionViewModel.signedIn.collectAsStateWithLifecycle().value ?: return
    key(signedIn) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = if (signedIn) SpacesRoute else SignInRoute) {
            composable<SignInRoute> { SignInScreen() }
            composable<SpacesRoute> {
                SpaceScreen(onOpenSpace = { spaceId -> navController.navigate(SpaceHomeRoute(spaceId)) })
            }
            composable<SpaceHomeRoute> { entry ->
                SpaceHomeScreen(
                    spaceId = entry.toRoute<SpaceHomeRoute>().spaceId,
                    onSwitchSpace = { navController.popBackStack() },
                )
            }
        }
    }
}
