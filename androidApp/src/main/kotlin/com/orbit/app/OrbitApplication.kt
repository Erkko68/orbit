package com.orbit.app

import android.app.Application
import com.orbit.app.di.initDemoKoin
import com.orbit.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class OrbitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEMO_AUTH) {
            initDemoKoin { androidContext(this@OrbitApplication) }
        } else {
            initKoin { androidContext(this@OrbitApplication) }
        }
    }
}
