package com.ing.zknotary.common.zkp

import java.io.File

data class CircuitMetaData(val folder: File) {
    init {
        require(folder.exists())
    }
}
