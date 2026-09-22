package com.orbit.app

import android.app.Application
import com.orbit.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class OrbitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@OrbitApplication) }
    }
}
