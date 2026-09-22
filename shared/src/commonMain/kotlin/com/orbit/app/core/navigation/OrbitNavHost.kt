package com.orbit.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orbit.app.feature.space.ui.SpaceScreen

@Composable
fun OrbitNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = SpacesRoute) {
        composable<SpacesRoute> { SpaceScreen() }
    }
}
