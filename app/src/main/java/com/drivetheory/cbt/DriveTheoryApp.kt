package com.drivetheory.cbt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DriveTheoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.drivetheory.cbt.presentation.viewmodel.ServiceLocatorRef.app = this
    }
}
