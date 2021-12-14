package com.ing.zkflow

import com.ing.zkflow.annotated.pilot.ivno.IvnoTokenType
import com.ing.zkflow.annotated.pilot.r3.types.TokenType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Transient
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

class PropertyAnnotatorTest {
    @ParameterizedTest
    @MethodSource("properties")
    fun `properties (not constructor parameters) must be transient`(case: Case) {
        case.property?.hasAnnotation<Transient>() shouldBe case.mustBeAnnotated
    }

    companion object {
        @JvmStatic
        fun properties() = listOf(
            Case(ClassWithProperties::class, "thisPropertyHasBackingField", true),
            Case(ClassWithProperties::class, "noBackingField1", false),
            Case(ClassWithProperties::class, "noBackingField2", false),
            //
            // Properties from actual classes.
            Case(IvnoTokenType::class, "participants", true),
            Case(TokenType::class, "displayTokenSize", false)
        )

        class Case(
            clazz: KClass<*>,
            propertyName: String,
            val mustBeAnnotated: Boolean
        ) {
            val property: KCallable<*>? = clazz.members.singleOrNull { it.name == propertyName }
        }
    }
}

@ZKP
data class ClassWithProperties(val int: Int) {
    val thisPropertyHasBackingField: Int = 0

    val noBackingField1 get() = 1 + 1 + 1
    val noBackingField2: Int get() {
        // This comment MUST NOT be removed.
        // Here is a comment featuring the word `field`
        // field = 2 // <- test
        // which is usually used to refer to the backing field.
        return 0
    }
}
