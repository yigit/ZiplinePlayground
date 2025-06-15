package com.birbit.ziplineplayground

import app.cash.zipline.Zipline
import app.cash.zipline.ZiplineScope
import app.cash.zipline.ZiplineScoped
import kotlin.getValue

class RealMyZiplineService : MyZiplineService, ZiplineScoped {
    override val scope: ZiplineScope = ZiplineScope()
    val hostService by lazy {
        Zipline.get().take<MyHostService>("myHostService")
    }
    override suspend fun echo(input: String): String {
        return "$input $input"
    }

    override suspend fun echoViaHost(input: String): String {
        return hostService.echo(input)
    }
}

