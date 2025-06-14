package com.birbit.ziplineplayground

import app.cash.zipline.ZiplineService

interface MyZiplineService : ZiplineService {
    suspend fun echo(input: String): String
    @Throws(RuntimeException::class)
    fun blockingEcho(input: String): String
}