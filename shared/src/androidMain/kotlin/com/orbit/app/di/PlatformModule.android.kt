package com.orbit.app.di

import com.orbit.app.data.auth.AndroidGoogleAuthClient
import com.orbit.app.data.auth.GoogleAuthClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<GoogleAuthClient> { AndroidGoogleAuthClient(androidContext()) }
}
