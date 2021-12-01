package com.ing.zkflow

import com.ing.zkflow.annotated.ivno.IvnoTokenType
import kotlinx.serialization.Transient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KCallable
import kotlin.reflect.full.hasAnnotation

class PropertyAnnotatorTest {
    @ParameterizedTest
    @MethodSource("properties")
    fun `properties (not constructor parameters) must be transient`(property: KCallable<*>) {
        assertTrue(property.hasAnnotation<Transient>())
    }

    companion object {
        @JvmStatic
        fun properties() = listOf(
            ClassWithProperties::class
                .members
                .single { it.name == "thisPropertyMustBeAnnotated" },
            IvnoTokenType::class
                .members
                .single { it.name == "participants" }
        )
    }
}

@ZKP
data class ClassWithProperties(val int: Int) {
    val thisPropertyMustBeAnnotated: Int = 0
}
