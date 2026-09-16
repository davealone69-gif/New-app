package com.threedoubled.app

import android.app.Application
import com.google.android.filament.utils.Utils

class ThreeDoubleDApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Loads the Filament / gltfio / filament-utils native libraries.
        Utils.init()
    }
}
