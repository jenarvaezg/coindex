package com.jenarvaezg.coindex.data.update

import java.io.File

/**
 * An installer that installs nothing and can be made to fail in each of the four ways a real one
 * does (#220): no permission, no screen to grant it on, a download that dies, no package installer.
 */
class FakeUpdateInstaller(
    var permitted: Boolean = true,
    var permissionScreenOpens: Boolean = true,
    var installs: Boolean = true,
    var downloadError: Throwable? = null,
) : UpdateInstaller {
    var permissionRequests = 0
    val downloads = mutableListOf<Pair<String, Int>>()
    var handed: File? = null

    override fun canInstall(): Boolean = permitted

    override fun requestInstallPermission(): Boolean {
        permissionRequests += 1
        return permissionScreenOpens
    }

    override suspend fun download(url: String, versionCode: Int): File {
        downloads += url to versionCode
        downloadError?.let { throw it }
        return File("coindex-$versionCode.apk")
    }

    override fun install(apk: File): Boolean {
        handed = apk
        return installs
    }
}
