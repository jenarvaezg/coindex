package com.jenarvaezg.coindex

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.jenarvaezg.coindex.data.photos.coinPhotoImageLoader
import com.jenarvaezg.coindex.data.photos.coinPhotoUserAgent

class CoindexApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Every picture in the app is a Numista catalog photograph; see [coinPhotoImageLoader]. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        coinPhotoImageLoader(
            context,
            coinPhotoUserAgent(container.installedVersionName()),
            container.gonePhotographs,
        )
}
