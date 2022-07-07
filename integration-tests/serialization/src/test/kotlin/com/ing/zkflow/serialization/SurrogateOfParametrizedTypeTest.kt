package com.ing.zkflow.serialization

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SurrogateOfParametrizedTypeTest : SerializerTest {
    // Setup
    @ZKP
    data class ParametrizedTypeContainer(
        val b: @Via<B> @UselessAnnotation A<@UselessAnnotation Int?> = A(1)
    )

    @ZKPSurrogate(A2B::class)
    class B(
        val t: Int?
    ) : Surrogate<A<Int?>> {
        override fun toOriginal(): A<Int?> = A(t)
    }

    object A2B : ConversionProvider<A<Int?>, B> {
        override fun from(original: A<Int?>): B {
            return B(original.t)
        }
    }

    class A<T>(val t: T)

    @Target(AnnotationTarget.TYPE)
    annotation class UselessAnnotation

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ParametrizedTypeContainer makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            SurrogateOfParametrizedTypeTestParametrizedTypeContainerSerializer,
            ParametrizedTypeContainer(),
            false
        ) { value, de -> value.b.t shouldBe de.b.t }
    }
}
