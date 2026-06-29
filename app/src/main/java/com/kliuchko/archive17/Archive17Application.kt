package com.kliuchko.archive17

import android.app.Application
import com.kliuchko.archive17.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Archive17Application : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Archive17Application)
            modules(appModule)
        }
    }
}
