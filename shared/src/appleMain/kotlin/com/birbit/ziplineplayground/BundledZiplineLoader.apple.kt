package com.birbit.ziplineplayground

import app.cash.zipline.loader.ZiplineLoader
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle

internal actual fun ZiplineLoader.configureEmbeddedFileSystem(appName: String): ZiplineLoader {
    val bundle = NSBundle.mainBundle
    val resourcePath = bundle.resourcePath ?: error("no resource path configured")
    return this.withEmbedded(
        embeddedFileSystem = FileSystem.SYSTEM,
        embeddedDir = resourcePath.toPath(normalize = true) / "zipline" / appName
    )
}