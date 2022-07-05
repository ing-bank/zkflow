package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.zinc.poet.generate.ClassWithAnonymousParty
import com.ing.zkflow.zinc.poet.generate.ClassWithAnonymousParty_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiChar
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiChar_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiString
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiString_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithBoolean
import com.ing.zkflow.zinc.poet.generate.ClassWithBoolean_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithByte
import com.ing.zkflow.zinc.poet.generate.ClassWithByte_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.ClassWithClassWithoutFields_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithDouble
import com.ing.zkflow.zinc.poet.generate.ClassWithDouble_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithEnum
import com.ing.zkflow.zinc.poet.generate.ClassWithEnum_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithFloat
import com.ing.zkflow.zinc.poet.generate.ClassWithFloat_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithHashAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithHashAttachmentConstraint_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithInt
import com.ing.zkflow.zinc.poet.generate.ClassWithInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithListOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithListOfInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithLong
import com.ing.zkflow.zinc.poet.generate.ClassWithLong_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithMapOfStringToInt
import com.ing.zkflow.zinc.poet.generate.ClassWithMapOfStringToInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithNullableInt
import com.ing.zkflow.zinc.poet.generate.ClassWithNullableInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithParty
import com.ing.zkflow.zinc.poet.generate.ClassWithParty_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithPublicKey
import com.ing.zkflow.zinc.poet.generate.ClassWithPublicKey_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSecureHash
import com.ing.zkflow.zinc.poet.generate.ClassWithSecureHash_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSetOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithSetOfInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithShort
import com.ing.zkflow.zinc.poet.generate.ClassWithShort_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSignatureAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithSignatureAttachmentConstraint_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUByte
import com.ing.zkflow.zinc.poet.generate.ClassWithUByte_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUInt
import com.ing.zkflow.zinc.poet.generate.ClassWithUInt_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithULong
import com.ing.zkflow.zinc.poet.generate.ClassWithULong_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUShort
import com.ing.zkflow.zinc.poet.generate.ClassWithUShort_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUnicodeChar
import com.ing.zkflow.zinc.poet.generate.ClassWithUnicodeChar_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf16String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf16String_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf32String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf32String_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf8String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf8String_Serializer
import com.ing.zkflow.zinc.poet.generate.ClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.ClassWithoutFields_Serializer
import com.ing.zkflow.zinc.poet.generate.VersionedState
import com.ing.zkflow.zinc.poet.generate.VersionedState_Serializer
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.descriptors.SerialDescriptor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ZkpStructureGeneratorTest {
    @ParameterizedTest
    @MethodSource("fixturesProvider")
    fun `Generated zinc types should match expected types`(
        descriptor: SerialDescriptor,
        expected: List<ZkpStructureType>
    ) {
        val actual = ZkpStructureGenerator.generate(descriptor).toFlattenedClassStructure().distinct().toList()
        actual shouldContainExactly expected
    }

    companion object {
        private inline fun <reified T> wrappedStructure(
            familyClassName: String? = null,
            field: ZkpStructureField? = null
        ): ZkpStructureClass = ZkpStructureClass(
            serialName = "${T::class.getSerialDescriptor().internalTypeName}",
            familyClassName = familyClassName,
            serializationId = null,
            byteSize = field?.fieldType?.byteSize ?: 0,
            fields = field?.let { listOf(it) } ?: emptyList()
        )

        private val wrappedVersionedStructure = wrappedStructure<VersionedState>(
            field = ZkpStructureField("state", ZkpStructurePrimitive("kotlin.Int", 4))
        )
        private val wrappedEmptyClass = wrappedStructure<ClassWithoutFields>()
        private val wrappedUnitStructure = ZkpStructureClass(
            "${ClassWithClassWithoutFields::class.simpleName}", null, null, 0,
            listOf(
                ZkpStructureField("c", wrappedEmptyClass.ref()),
            )
        )
        private val wrappedBoolStructure = wrappedStructure<ClassWithBoolean>(
            field = ZkpStructureField("boolean", ZkpStructurePrimitive("kotlin.Boolean", Byte.SIZE_BYTES))
        )
        private val wrappedByteStructure = wrappedStructure<ClassWithByte>(
            field = ZkpStructureField("byte", ZkpStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES))
        )
        private val wrappedUByteStructure = wrappedStructure<ClassWithUByte>(
            field = ZkpStructureField("ubyte", ZkpStructurePrimitive("kotlin.UByte", UByte.SIZE_BYTES))
        )
        private val wrappedShortStructure = wrappedStructure<ClassWithShort>(
            field = ZkpStructureField("short", ZkpStructurePrimitive("kotlin.Short", Short.SIZE_BYTES))
        )
        private val wrappedUShortStructure = wrappedStructure<ClassWithUShort>(
            field = ZkpStructureField("ushort", ZkpStructurePrimitive("kotlin.UShort", UShort.SIZE_BYTES))
        )
        private val wrappedIntStructure = wrappedStructure<ClassWithInt>(
            field = ZkpStructureField("int", ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
        )
        private val wrappedUIntStructure = wrappedStructure<ClassWithUInt>(
            field = ZkpStructureField("uint", ZkpStructurePrimitive("kotlin.UInt", UInt.SIZE_BYTES))
        )
        private val wrappedLongStructure = wrappedStructure<ClassWithLong>(
            field = ZkpStructureField("long", ZkpStructurePrimitive("kotlin.Long", Long.SIZE_BYTES))
        )
        private val wrappedULongStructure = wrappedStructure<ClassWithULong>(
            field = ZkpStructureField("ulong", ZkpStructurePrimitive("kotlin.ULong", ULong.SIZE_BYTES))
        )
        private val wrappedFloatStructure = wrappedStructure<ClassWithFloat>(
            field = ZkpStructureField("float", ZkpStructureBigDecimal(95, "Float", 39, 46))
        )
        private val wrappedDoubleStructure = wrappedStructure<ClassWithDouble>(
            field = ZkpStructureField("double", ZkpStructureBigDecimal(644, "Double", 309, 325))
        )
        private val wrappedAsciiCharStructure = wrappedStructure<ClassWithAsciiChar>(
            field = ZkpStructureField("asciiChar", ZkpStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES))
        )
        private val wrappedUnicodeCharStructure = wrappedStructure<ClassWithUnicodeChar>(
            field = ZkpStructureField("unicodeChar", ZkpStructurePrimitive("kotlin.Short", Short.SIZE_BYTES))
        )
        private val wrappedAsciiStringStructure = wrappedStructure<ClassWithAsciiString>(
            field = ZkpStructureField("string", ZkpStructureString(12, 8, "Ascii"))
        )
        private val wrappedUtf8StringStructure = wrappedStructure<ClassWithUtf8String>(
            field = ZkpStructureField("string", ZkpStructureString(12, 8, "Utf8"))
        )
        private val wrappedUtf16StringStructure = wrappedStructure<ClassWithUtf16String>(
            field = ZkpStructureField("string", ZkpStructureString(12, 8, "Utf16"))
        )
        private val wrappedUtf32StringStructure = wrappedStructure<ClassWithUtf32String>(
            field = ZkpStructureField("string", ZkpStructureString(12, 8, "Utf32"))
        )
        private val wrappedNullableIntStructure = wrappedStructure<ClassWithNullableInt>(
            field = ZkpStructureField(
                "nullableInt",
                ZkpStructureNullable(5, ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedListOfIntStructure = wrappedStructure<ClassWithListOfInt>(
            field = ZkpStructureField(
                "list",
                ZkpStructureList(36, 8, ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedSetOfIntStructure = wrappedStructure<ClassWithSetOfInt>(
            field = ZkpStructureField(
                "set",
                ZkpStructureList(36, 8, ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedMapOfStringIntStructure = wrappedStructure<ClassWithMapOfStringToInt>(
            field = ZkpStructureField(
                "map",
                ZkpStructureMap(
                    132,
                    8,
                    ZkpStructureString(12, 8, "Utf8"),
                    ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES),
                )
            )
        )
        private val wrappedEnumStructure = wrappedStructure<ClassWithEnum>(
            field = ZkpStructureField("enum", ZkpStructureEnum("${EnumWithNumbers::class.qualifiedName}"))
        )
        private val wrappedPublicKeyStructure = wrappedStructure<ClassWithPublicKey>(
            field = ZkpStructureField("pk", ZkpStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
        )
        private val wrappedAnonymousPartyStructure = wrappedStructure<ClassWithAnonymousParty>(
            field = ZkpStructureField("party", ZkpStructureClassRef("AnonymousPartyEdDsaEd25519Sha512", 48))
        )
        private val wrappedPartyStructure = wrappedStructure<ClassWithParty>(
            field = ZkpStructureField("party", ZkpStructureClassRef("PartyEdDsaEd25519Sha512", 461))
        )
        private val wrappedSecureHash = wrappedStructure<ClassWithSecureHash>(
            field = ZkpStructureField("hash", ZkpStructureClassRef("SecureHashSha256", 36))
        )
        private val wrappedSignatureAttachmentConstraint = wrappedStructure<ClassWithSignatureAttachmentConstraint>(
            field = ZkpStructureField("constraint", ZkpStructureClassRef("SignatureAttachmentConstraintEdDsaEd25519Sha512", 48))
        )
        private val wrappedHashAttachmentConstraint = wrappedStructure<ClassWithHashAttachmentConstraint>(
            field = ZkpStructureField("constraint", ZkpStructureClassRef("HashAttachmentConstraint", 36))
        )
        private val anonymousPartyEdDsaEd25519Sha512 = ZkpStructureClass(
            "AnonymousPartyEdDsaEd25519Sha512", null, null, 48,
            listOf(
                ZkpStructureField("publicKey", ZkpStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
            )
        )
        private val partyEdDsaEd25519Sha256 = ZkpStructureClass(
            "PartyEdDsaEd25519Sha512", null, null, 461,
            listOf(
                ZkpStructureField(
                    "cordaX500Name",
                    ZkpStructureClassRef(
                        CordaX500NameSerializer.CordaX500NameSurrogate::class.getSerialDescriptor().internalTypeName,
                        413
                    )
                ),
                ZkpStructureField("publicKey", ZkpStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
            )
        )
        private val publicKeyEdDsaEd25519Sha512 = ZkpStructureClass(
            "PublicKeyEdDsaEd25519Sha512", null, null, 48,
            listOf(
                ZkpStructureField(
                    "bytes",
                    ZkpStructureArray(
                        48, 44, ZkpStructurePrimitive("kotlin.Byte", 1)
                    )
                )
            )
        )
        private val secureHashSha256 = ZkpStructureClass(
            "SecureHashSha256", null, null, 36,
            listOf(
                ZkpStructureField(
                    "bytes",
                    ZkpStructureArray(
                        36, 32, ZkpStructurePrimitive("kotlin.Byte", 1)
                    )
                )
            )
        )
        private val signatureAttachmentConstraintEdDsaEd25519Sha512 = ZkpStructureClass(
            "SignatureAttachmentConstraintEdDsaEd25519Sha512", null, null, 48,
            listOf(
                ZkpStructureField(
                    "key", ZkpStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48)
                )
            )
        )
        private val hashAttachmentConstraint = ZkpStructureClass(
            "HashAttachmentConstraint", null, null, 36,
            listOf(
                ZkpStructureField("attachmentId", ZkpStructureClassRef("SecureHashSha256", 36))
            )
        )
        private val cordaX500NameSurrogate = ZkpStructureClass(
            CordaX500NameSerializer.CordaX500NameSurrogate::class.getSerialDescriptor().internalTypeName,
            null, null,
            413,
            listOf(
                ZkpStructureField("commonName", ZkpStructureNullable(69, ZkpStructureString(68, 64, "Utf8"))),
                ZkpStructureField("organisationUnit", ZkpStructureNullable(69, ZkpStructureString(68, 64, "Utf8"))),
                ZkpStructureField("organisation", ZkpStructureString(132, 128, "Utf8")),
                ZkpStructureField("locality", ZkpStructureString(68, 64, "Utf8")),
                ZkpStructureField("state", ZkpStructureNullable(69, ZkpStructureString(68, 64, "Utf8"))),
                ZkpStructureField("country", ZkpStructureString(6, 2, "Utf8")),
            )
        )

        @JvmStatic
        @Suppress("LongMethod")
        fun fixturesProvider(): List<Arguments> {
            return listOf(
                Arguments.of(VersionedState_Serializer.descriptor, listOf(wrappedVersionedStructure)),
                Arguments.of(ClassWithoutFields_Serializer.descriptor, listOf(wrappedEmptyClass)),
                Arguments.of(
                    ClassWithClassWithoutFields_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUnitStructure, wrappedEmptyClass)
                ),
                Arguments.of(ClassWithBoolean_Serializer.descriptor, listOf<ZkpStructureType>(wrappedBoolStructure)),
                Arguments.of(ClassWithByte_Serializer.descriptor, listOf<ZkpStructureType>(wrappedByteStructure)),
                Arguments.of(ClassWithUByte_Serializer.descriptor, listOf<ZkpStructureType>(wrappedUByteStructure)),
                Arguments.of(ClassWithShort_Serializer.descriptor, listOf<ZkpStructureType>(wrappedShortStructure)),
                Arguments.of(ClassWithUShort_Serializer.descriptor, listOf<ZkpStructureType>(wrappedUShortStructure)),
                Arguments.of(ClassWithInt_Serializer.descriptor, listOf<ZkpStructureType>(wrappedIntStructure)),
                Arguments.of(ClassWithUInt_Serializer.descriptor, listOf<ZkpStructureType>(wrappedUIntStructure)),
                Arguments.of(ClassWithLong_Serializer.descriptor, listOf<ZkpStructureType>(wrappedLongStructure)),
                Arguments.of(ClassWithULong_Serializer.descriptor, listOf<ZkpStructureType>(wrappedULongStructure)),
                Arguments.of(ClassWithFloat_Serializer.descriptor, listOf<ZkpStructureType>(wrappedFloatStructure)),
                Arguments.of(ClassWithDouble_Serializer.descriptor, listOf<ZkpStructureType>(wrappedDoubleStructure)),
                Arguments.of(
                    ClassWithAsciiChar_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedAsciiCharStructure)
                ),
                Arguments.of(
                    ClassWithUnicodeChar_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUnicodeCharStructure)
                ),
                Arguments.of(
                    ClassWithAsciiString_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedAsciiStringStructure)
                ),
                Arguments.of(
                    ClassWithUtf8String_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf8StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf16String_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf16StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf32String_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf32StringStructure)
                ),
                Arguments.of(
                    ClassWithNullableInt_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedNullableIntStructure)
                ),
                Arguments.of(
                    ClassWithListOfInt_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedListOfIntStructure)
                ),
                Arguments.of(
                    ClassWithSetOfInt_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedSetOfIntStructure)
                ),
                Arguments.of(
                    ClassWithMapOfStringToInt_Serializer.descriptor,
                    listOf<ZkpStructureType>(wrappedMapOfStringIntStructure)
                ),
                Arguments.of(EnumWithNumbers_Serializer.descriptor, listOf<ZkpStructureType>()),
                Arguments.of(ClassWithEnum_Serializer.descriptor, listOf<ZkpStructureType>(wrappedEnumStructure)),
                Arguments.of(
                    ClassWithPublicKey_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPublicKeyStructure, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithAnonymousParty_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedAnonymousPartyStructure, anonymousPartyEdDsaEd25519Sha512, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithParty_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPartyStructure, partyEdDsaEd25519Sha256, cordaX500NameSurrogate, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithSecureHash_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSecureHash, secureHashSha256
                    )
                ),
                Arguments.of(
                    ClassWithSignatureAttachmentConstraint_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSignatureAttachmentConstraint,
                        signatureAttachmentConstraintEdDsaEd25519Sha512,
                        publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithHashAttachmentConstraint_Serializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedHashAttachmentConstraint,
                        hashAttachmentConstraint,
                        secureHashSha256
                    )
                ),
            )
        }
    }
}
