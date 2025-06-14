package com.birbit.ziplineplayground

import app.cash.zipline.Zipline

@JsExport
fun launchZipline() {
    val zipline = Zipline.get()
    zipline.bind<MyZiplineService>("myZiplineService", RealMyZiplineService())
}