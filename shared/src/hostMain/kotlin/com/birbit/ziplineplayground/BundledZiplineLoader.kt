package com.birbit.ziplineplayground

import app.cash.zipline.loader.ManifestVerifier
import app.cash.zipline.loader.ZiplineHttpClient
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import okio.ByteString

private val NO_OP_HTTP_CLIENT = object : ZiplineHttpClient() {
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        throw UnsupportedOperationException("Didn't expect to download anything, looks like we failed to load from embedded resources")
    }
}

internal expect fun ZiplineLoader.configureEmbeddedFileSystem(
    appName: String
): ZiplineLoader
/**
 * A ZiplineLoader abstraction that initializes with a local copy of latest js target, bundled with
 * the app via the gradle setup.
 */
object BundledZiplineLoader {
    fun createBundledLoader(
        dispatcher: CoroutineDispatcher,
        httpClient: ZiplineHttpClient = NO_OP_HTTP_CLIENT,
        appName: String,
    ): ZiplineLoader {
        return ZiplineLoader(
            dispatcher = dispatcher,
            manifestVerifier = ManifestVerifier.NO_SIGNATURE_CHECKS,
            httpClient = httpClient,
        ).configureEmbeddedFileSystem(appName)
    }
}