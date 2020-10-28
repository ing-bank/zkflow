package com.ing.zknotary.common.util

import java.io.File
import kotlin.math.max

/**
 * Computes the latest modification date across the files contained in a folder.
 * Accepts folders only.
 */
val File.includedLastModified: Long?
    get() = if (isFile) {
        error("Only folders are accepted")
    } else {
        walkTopDown()
            .filter { it.isFile }
            .fold(null as Long?) { lastModified, path ->
                max(path.lastModified(), lastModified ?: 0L)
            }
    }
