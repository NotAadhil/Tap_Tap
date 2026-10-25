package com.tapflow

import android.app.Application

/**
 * Application class for TapFlow. Holds the global [AppContainer].
 */
class TapFlowApp : Application() {

    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
