package com.orbit.app.di

import androidx.room.Room
import com.orbit.app.data.local.DATABASE_NAME
import com.orbit.app.data.local.OrbitDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single {
        val context = androidContext()
        Room.databaseBuilder<OrbitDatabase>(context, context.getDatabasePath(DATABASE_NAME).absolutePath)
    }
}
