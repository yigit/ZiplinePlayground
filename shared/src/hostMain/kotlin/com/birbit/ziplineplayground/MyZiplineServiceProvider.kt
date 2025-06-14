package com.birbit.ziplineplayground

import app.cash.zipline.ZiplineManifest
import app.cash.zipline.ZiplineScope
import app.cash.zipline.loader.FreshnessChecker
import app.cash.zipline.loader.LoadResult
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// from https://github.com/cashapp/zipline/blob/df7239bc37f2304620e8ed4e20e28744bd7a13b2/zipline-testing/src/hostMain/kotlin/app/cash/zipline/testing/CoroutineDispatchers.kt#L29
@OptIn(ExperimentalCoroutinesApi::class)
expect fun singleThreadCoroutineDispatcher(
    name: String,
    stackSize: Int,
): CloseableCoroutineDispatcher

object MyZiplineServiceProvider {
    private var cached: MyZiplineService? = null
    private val loadedMutex = Mutex()

    suspend fun obtain(): MyZiplineService {
        cached?.let { return it }
        loadedMutex.withLock {
            cached?.let { return it }
            return doLoadService().also {
                cached = it
            }

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun doLoadService(): MyZiplineService {
        val dispatcher = singleThreadCoroutineDispatcher("ziplineDispatcher", 8 * 1024 * 1024)
        val loader = BundledZiplineLoader.createBundledLoader(
            dispatcher = dispatcher,
            appName = "shared"
        )
        val loadResult = withContext(dispatcher) {
            // not using the qjs dispatcher crashes the app.
            loader.loadOnce("shared", object : FreshnessChecker {
                override fun isFresh(
                    manifest: ZiplineManifest,
                    freshAtEpochMs: Long
                ): Boolean = true
            }, manifestUrl = "http://localhost/shared.zipline.json")
        }
        check(loadResult is LoadResult.Success) {
            "Failed to load zipline: $loadResult"
        }
        val scope = ZiplineScope()
        val zipline = loadResult.zipline
        val service = zipline.take<MyZiplineService>("myZiplineService", scope)
        return MyServiceWrapper(dispatcher, service)
    }
}

class MyServiceWrapper(val dispatcher: CoroutineDispatcher, val delegate: MyZiplineService) :
    MyZiplineService {
    override suspend fun echo(input: String): String = withContext(dispatcher) {
        delegate.echo(input)
    }

    override fun blockingEcho(input: String): String {
        return "cannot call this "
    }

}