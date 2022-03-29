package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UTF8Char
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsBasics(
    val char: @ASCIIChar Char = 'z',
    val utf8Char: @UTF8Char Char = '的',
    val int: Int = 0,
    val string: @UTF8(6) String = "䶖万"
)
