package com.jenarvaezg.coindex

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.allowHardware

class CoindexApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Hardware bitmaps are disabled because exporting a plate replays it onto a software
     * canvas, which cannot draw them. The catalog pictures are small, so the cost is noise.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).allowHardware(false).build()
}
