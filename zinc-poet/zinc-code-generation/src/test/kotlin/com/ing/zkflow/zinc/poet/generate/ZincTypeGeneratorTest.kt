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
            Arguments.of(ClassWithoutFieldsSerializer.descriptor, BflUnit),
            Arguments.of(ClassWithClassWithoutFieldsSerializer.descriptor, structWithUnit),
            Arguments.of(ClassWithBooleanSerializer.descriptor, structWithBoolean),
            Arguments.of(ClassWithByteSerializer.descriptor, structWithByte),
            Arguments.of(ClassWithShortSerializer.descriptor, structWithShort),
            Arguments.of(ClassWithIntSerializer.descriptor, structWithInt),
            Arguments.of(ClassWithLongSerializer.descriptor, structWithLong),
            Arguments.of(ClassWithFloatSerializer.descriptor, structWithFloat),
            Arguments.of(ClassWithDoubleSerializer.descriptor, structWithDouble),
            Arguments.of(ClassWithAsciiCharSerializer.descriptor, structWithAsciiChar),
            Arguments.of(ClassWithUnicodeCharSerializer.descriptor, structWithUnicodeChar),
            Arguments.of(ClassWithAsciiStringSerializer.descriptor, structWithAsciiString),
            Arguments.of(ClassWithUtf8StringSerializer.descriptor, structWithUtf8String),
            Arguments.of(ClassWithUtf16StringSerializer.descriptor, structWithUtf16String),
            Arguments.of(ClassWithUtf32StringSerializer.descriptor, structWithUtf32String),
            Arguments.of(ClassWithNullableIntSerializer.descriptor, structWithNullableInt),
            Arguments.of(ClassWithListOfIntSerializer.descriptor, structWithListOfInt),
            Arguments.of(ClassWithSetOfIntSerializer.descriptor, structWithSetOfInt),
            Arguments.of(ClassWithMapOfStringToIntSerializer.descriptor, structWithMapOfStringToInt),
            // Arguments.of(EnumWithNumbers_Serializer.descriptor, enumWithNumbers),
            Arguments.of(ClassWithPublicKeySerializer.descriptor, structWithPublicKey),
            Arguments.of(ClassWithAnonymousPartySerializer.descriptor, structWithAnonymousParty),
            Arguments.of(ClassWithPartySerializer.descriptor, structWithParty),
            Arguments.of(ClassWithSecureHashSerializer.descriptor, structWithSecureHash),
            Arguments.of(ClassWithSignatureAttachmentConstraintSerializer.descriptor, structWithSignatureAttachmentConstraint),
            Arguments.of(ClassWithHashAttachmentConstraintSerializer.descriptor, structWithHashAttachmentConstraint),
        )

        @JvmStatic
        fun unsupportedFixturesProvider() = listOf(
            Arguments.of(String.serializer().descriptor),
            Arguments.of(Char.serializer().descriptor),
        )
    }
}
