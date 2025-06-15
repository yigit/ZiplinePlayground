package com.birbit.ziplineplayground

import app.cash.zipline.ZiplineService

interface MyHostService: ZiplineService {
    suspend fun echo(msg: String): String
}