package com.birbit.ziplineplayground

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
actual fun singleThreadCoroutineDispatcher(
    name: String,
    stackSize: Int,
): CloseableCoroutineDispatcher {
    val executorService = ThreadPoolExecutor(
        0,
        1,
        100,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
    ) { runnable ->
        Thread(null, runnable, name, stackSize.toLong())
    }

    return executorService.asCoroutineDispatcher()
}
