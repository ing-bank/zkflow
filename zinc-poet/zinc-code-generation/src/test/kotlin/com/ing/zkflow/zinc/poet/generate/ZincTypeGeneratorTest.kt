package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflUnit
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
            Arguments.of(ClassWithoutFields_Serializer.descriptor, BflUnit),
            Arguments.of(ClassWithClassWithoutFields_Serializer.descriptor, structWithUnit),
            Arguments.of(ClassWithBoolean_Serializer.descriptor, structWithBoolean),
            Arguments.of(ClassWithByte_Serializer.descriptor, structWithByte),
            Arguments.of(ClassWithUByte_Serializer.descriptor, structWithUByte),
            Arguments.of(ClassWithShort_Serializer.descriptor, structWithShort),
            Arguments.of(ClassWithUShort_Serializer.descriptor, structWithUShort),
            Arguments.of(ClassWithInt_Serializer.descriptor, structWithInt),
            Arguments.of(ClassWithUInt_Serializer.descriptor, structWithUInt),
            Arguments.of(ClassWithLong_Serializer.descriptor, structWithLong),
            Arguments.of(ClassWithULong_Serializer.descriptor, structWithULong),
            Arguments.of(ClassWithFloat_Serializer.descriptor, structWithFloat),
            Arguments.of(ClassWithDouble_Serializer.descriptor, structWithDouble),
            Arguments.of(ClassWithAsciiChar_Serializer.descriptor, structWithAsciiChar),
            Arguments.of(ClassWithUnicodeChar_Serializer.descriptor, structWithUnicodeChar),
            Arguments.of(ClassWithAsciiString_Serializer.descriptor, structWithAsciiString),
            Arguments.of(ClassWithUtf8String_Serializer.descriptor, structWithUtf8String),
            Arguments.of(ClassWithUtf16String_Serializer.descriptor, structWithUtf16String),
            Arguments.of(ClassWithUtf32String_Serializer.descriptor, structWithUtf32String),
            Arguments.of(ClassWithNullableInt_Serializer.descriptor, structWithNullableInt),
            Arguments.of(ClassWithListOfInt_Serializer.descriptor, structWithListOfInt),
            Arguments.of(ClassWithSetOfInt_Serializer.descriptor, structWithSetOfInt),
            Arguments.of(ClassWithMapOfStringToInt_Serializer.descriptor, structWithMapOfStringToInt),
            Arguments.of(EnumWithNumbers_Serializer.descriptor, enumWithNumbers),
            Arguments.of(ClassWithPublicKey_Serializer.descriptor, structWithPublicKey),
            Arguments.of(ClassWithAnonymousParty_Serializer.descriptor, structWithAnonymousParty),
            Arguments.of(ClassWithParty_Serializer.descriptor, structWithParty),
            Arguments.of(ClassWithSecureHash_Serializer.descriptor, structWithSecureHash),
            Arguments.of(ClassWithSignatureAttachmentConstraint_Serializer.descriptor, structWithSignatureAttachmentConstraint),
            Arguments.of(ClassWithHashAttachmentConstraint_Serializer.descriptor, structWithHashAttachmentConstraint),
        )

        @JvmStatic
        fun unsupportedFixturesProvider() = listOf(
            Arguments.of(String.serializer().descriptor),
            Arguments.of(Char.serializer().descriptor),
        )
    }
}
