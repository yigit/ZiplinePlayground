package com.birbit.ziplineplayground

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSThread
import kotlin.coroutines.CoroutineContext

// taken from Zipline host tests.
@OptIn(ExperimentalForeignApi::class)
actual fun singleThreadCoroutineDispatcher(
    name: String,
    stackSize: Int
): CloseableCoroutineDispatcher {
    val channel = Channel<Runnable?>(capacity = Channel.UNLIMITED)

    val thread = NSThread {
        runBlocking {
            while (true) {
                val runnable = channel.receive() ?: break
                runnable.run()
            }
        }
    }.apply {
        this.name = name
        this.stackSize = stackSize.convert()
    }

    thread.start()

    return object : CloseableCoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            channel.trySend(block)
        }

        override fun close() {
            channel.trySend(null)
        }
    }
}