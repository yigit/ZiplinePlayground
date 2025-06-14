package com.birbit.ziplineplayground

import androidx.kruth.assertThat
import app.cash.zipline.ZiplineManifest
import app.cash.zipline.ZiplineScope
import app.cash.zipline.loader.DefaultFreshnessCheckerNotFresh
import app.cash.zipline.loader.FreshnessChecker
import app.cash.zipline.loader.LoadResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class PlaygroundTest {
    @Test
    fun bundledLoader() = runBlocking {
        val service = MyZiplineServiceProvider.obtain()
        val result = service.echo("hello")
        assertThat(result).isEqualTo("hello hello")
    }
}