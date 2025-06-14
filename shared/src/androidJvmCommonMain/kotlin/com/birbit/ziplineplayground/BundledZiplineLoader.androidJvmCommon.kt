package com.birbit.ziplineplayground

import app.cash.zipline.loader.ZiplineLoader
import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source

internal actual fun ZiplineLoader.configureEmbeddedFileSystem(appName: String): ZiplineLoader {
   return this.withEmbedded(
        embeddedFileSystem = FixedResourceFileSystem(FileSystem.RESOURCES),
        embeddedDir = "/zipline".toPath() / appName
    )
}

/**
 * Okio ResourceFileSystem does not work properly on Android.
 * More specifically, it returns false from an `eixsts` call even though `source` would open the
 * file properly.
 * This wrapper fixes that issue for android. Not needed for JVM.
 */
private class FixedResourceFileSystem(
    val delegate: FileSystem
) : FileSystem() {
    override fun canonicalize(path: Path): Path {
        return delegate.canonicalize(path)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        try {
            delegate.source(path)
        } catch (fileNotFound: FileNotFoundException) {
            return null
        }
        return FileMetadata(
            isRegularFile = false,
            isDirectory = false,
        )
    }

    override fun list(dir: Path): List<Path> {
        return delegate.list(dir)
    }

    override fun listOrNull(dir: Path): List<Path>? {
        return delegate.listOrNull(dir)
    }

    override fun openReadOnly(file: Path): FileHandle {
        return delegate.openReadOnly(file)
    }

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean
    ): FileHandle {
        return delegate.openReadWrite(file, mustCreate, mustExist)
    }

    override fun source(file: Path): Source {
        return delegate.source(file)
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        error("not supported")
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        error("not supported")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        error("not supported")
    }

    override fun atomicMove(source: Path, target: Path) {
        error("not supported")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        error("not supported")
    }

    override fun createSymlink(source: Path, target: Path) {
        error("not supported")
    }
}