package com.ing.zkflow.util

import java.time.Duration

fun Duration.toSecondsWithNanosString(): String = "$seconds.$nano"
