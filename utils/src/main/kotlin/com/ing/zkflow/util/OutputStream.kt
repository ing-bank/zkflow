package com.ing.zkflow.util

import java.io.OutputStream
import java.nio.charset.StandardCharsets

fun OutputStream.appendText(text: String) = use {
    write(text.toByteArray(StandardCharsets.UTF_8))
}
