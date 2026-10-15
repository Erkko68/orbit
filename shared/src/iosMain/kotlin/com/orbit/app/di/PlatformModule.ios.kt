package com.orbit.app.di

import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.auth.GoogleSignInBridge
import com.orbit.app.data.auth.IosGoogleAuthClient
import org.koin.dsl.module

actual val platformModule = module {
    single<GoogleAuthClient> { IosGoogleAuthClient(get()) }
}

/** Starts Koin from Swift with the parts only Swift can provide. */
fun initKoin(googleSignIn: GoogleSignInBridge) = initKoin {
    modules(module { single { googleSignIn } })
}
