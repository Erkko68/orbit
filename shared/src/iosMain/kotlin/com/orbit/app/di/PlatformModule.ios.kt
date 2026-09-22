package com.orbit.app.di

import androidx.room.Room
import com.orbit.app.data.local.DATABASE_NAME
import com.orbit.app.data.local.OrbitDatabase
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val platformModule = module {
    single { Room.databaseBuilder<OrbitDatabase>(name = "${NSHomeDirectory()}/Documents/$DATABASE_NAME") }
}
