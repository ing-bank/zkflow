package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UnicodeChar
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsBasics(
    val char: @ASCIIChar Char = 'z',
    val unicodeChar: @UnicodeChar Char = '的',
    val int: Int = 0,
    val string: @UTF8(6) String = "䶖万"
)
