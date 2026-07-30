package com.jenarvaezg.coindex

import android.app.Application

class CoindexApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
