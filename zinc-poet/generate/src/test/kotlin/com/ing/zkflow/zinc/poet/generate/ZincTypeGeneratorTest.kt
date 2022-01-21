package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ZincTypeGeneratorTest {
    @ParameterizedTest
    @MethodSource("fixturesProvider")
    fun `Generated zinc types should match expected types`(descriptor: SerialDescriptor, expectedType: BflType) {
        val actual = ZincTypeGenerator.generate(descriptor)
        actual shouldBe expectedType
    }

    @ParameterizedTest
    @MethodSource("unsupportedFixturesProvider")
    fun `Generation fails for normal strings and collections`(descriptor: SerialDescriptor) {
        val ex = shouldThrow<IllegalArgumentException> {
            println(ZincTypeGenerator.generate(descriptor))
        }
        ex.message shouldBe "No handler found for ${descriptor.kind}: ${descriptor.serialName}."
    }

    companion object {
        @JvmStatic
        fun fixturesProvider(): List<Arguments> = listOf(
            Arguments.of(ClassWithBoolean.serializer().descriptor, structWithBoolean),
            Arguments.of(ClassWithByte.serializer().descriptor, structWithByte),
            Arguments.of(ClassWithUByte.serializer().descriptor, structWithUByte),
            Arguments.of(ClassWithShort.serializer().descriptor, structWithShort),
            Arguments.of(ClassWithUShort.serializer().descriptor, structWithUShort),
            Arguments.of(ClassWithInt.serializer().descriptor, structWithInt),
            Arguments.of(ClassWithUInt.serializer().descriptor, structWithUInt),
            Arguments.of(ClassWithLong.serializer().descriptor, structWithLong),
            Arguments.of(ClassWithULong.serializer().descriptor, structWithULong),
            Arguments.of(ClassWithAsciiChar.serializer().descriptor, structWithAsciiChar),
            Arguments.of(ClassWithUtf8Char.serializer().descriptor, structWithUtf8Char),
            Arguments.of(ClassWithAsciiString.serializer().descriptor, structWithAsciiString),
            Arguments.of(ClassWithUtf8String.serializer().descriptor, structWithUtf8String),
            Arguments.of(ClassWithNullableInt.serializer().descriptor, structWithNullableInt),
            Arguments.of(ClassWithListOfInt.serializer().descriptor, structWithListOfInt),
            Arguments.of(ClassWithSetOfInt.serializer().descriptor, structWithSetOfInt),
            Arguments.of(ClassWithMapOfStringToInt.serializer().descriptor, structWithMapOfStringToInt),
            Arguments.of(EnumWithNumbers.serializer().descriptor, enumWithNumbers),
            Arguments.of(ClassWithPublicKey.serializer().descriptor, structWithPublicKey),
            Arguments.of(ClassWithAnonymousParty.serializer().descriptor, structWithAnonymousParty),
            Arguments.of(ClassWithParty.serializer().descriptor, structWithParty),
        )

        @JvmStatic
        fun unsupportedFixturesProvider() = listOf(
            Arguments.of(String.serializer().descriptor),
            Arguments.of(Char.serializer().descriptor),
        )
    }
}
