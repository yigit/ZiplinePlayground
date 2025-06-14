package com.birbit.ziplineplayground

import app.cash.zipline.ZiplineScope
import app.cash.zipline.ZiplineScoped

class RealMyZiplineService : MyZiplineService, ZiplineScoped {
    override val scope: ZiplineScope = ZiplineScope()
    override suspend fun echo(input: String): String {
        return "$input $input"
    }

    override fun blockingEcho(input: String): String {
        return "$input $input"
    }
}

