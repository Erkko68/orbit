package com.orbit.app.di

import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.auth.IosGoogleAuthClient
import org.koin.dsl.module

actual val platformModule = module {
    single<GoogleAuthClient> { IosGoogleAuthClient() }
}
