package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.zinc.poet.generate.ClassWithAnonymousParty
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiChar
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiString
import com.ing.zkflow.zinc.poet.generate.ClassWithBoolean
import com.ing.zkflow.zinc.poet.generate.ClassWithByte
import com.ing.zkflow.zinc.poet.generate.ClassWithClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.ClassWithDouble
import com.ing.zkflow.zinc.poet.generate.ClassWithEnum
import com.ing.zkflow.zinc.poet.generate.ClassWithFloat
import com.ing.zkflow.zinc.poet.generate.ClassWithHashAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithInt
import com.ing.zkflow.zinc.poet.generate.ClassWithListOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithLong
import com.ing.zkflow.zinc.poet.generate.ClassWithMapOfStringToInt
import com.ing.zkflow.zinc.poet.generate.ClassWithNullableInt
import com.ing.zkflow.zinc.poet.generate.ClassWithParty
import com.ing.zkflow.zinc.poet.generate.ClassWithPublicKey
import com.ing.zkflow.zinc.poet.generate.ClassWithSecureHash
import com.ing.zkflow.zinc.poet.generate.ClassWithSetOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithShort
import com.ing.zkflow.zinc.poet.generate.ClassWithSignatureAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithUByte
import com.ing.zkflow.zinc.poet.generate.ClassWithUInt
import com.ing.zkflow.zinc.poet.generate.ClassWithULong
import com.ing.zkflow.zinc.poet.generate.ClassWithUShort
import com.ing.zkflow.zinc.poet.generate.ClassWithUnicodeChar
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf16String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf32String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf8String
import com.ing.zkflow.zinc.poet.generate.ClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.EnumWithNumbers
import com.ing.zkflow.zinc.poet.generate.VersionedState
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.builtins.serializer
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
            serialName = "${T::class.qualifiedName}",
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
            "${ClassWithClassWithoutFields::class.qualifiedName}", null, null, 0,
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
            field = ZkpStructureField(
                "constraint",
                ZkpStructureClassRef("SignatureAttachmentConstraintEdDsaEd25519Sha512", 48)
            )
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
                        "com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer.CordaX500NameSurrogate",
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
            "com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer.CordaX500NameSurrogate",
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
                Arguments.of(VersionedState.serializer().descriptor, listOf(wrappedVersionedStructure)),
                Arguments.of(ClassWithoutFields.serializer().descriptor, listOf(wrappedEmptyClass)),
                Arguments.of(
                    ClassWithClassWithoutFields.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedUnitStructure, wrappedEmptyClass)
                ),
                Arguments.of(ClassWithBoolean.serializer().descriptor, listOf<ZkpStructureType>(wrappedBoolStructure)),
                Arguments.of(ClassWithByte.serializer().descriptor, listOf<ZkpStructureType>(wrappedByteStructure)),
                Arguments.of(ClassWithUByte.serializer().descriptor, listOf<ZkpStructureType>(wrappedUByteStructure)),
                Arguments.of(ClassWithShort.serializer().descriptor, listOf<ZkpStructureType>(wrappedShortStructure)),
                Arguments.of(ClassWithUShort.serializer().descriptor, listOf<ZkpStructureType>(wrappedUShortStructure)),
                Arguments.of(ClassWithInt.serializer().descriptor, listOf<ZkpStructureType>(wrappedIntStructure)),
                Arguments.of(ClassWithUInt.serializer().descriptor, listOf<ZkpStructureType>(wrappedUIntStructure)),
                Arguments.of(ClassWithLong.serializer().descriptor, listOf<ZkpStructureType>(wrappedLongStructure)),
                Arguments.of(ClassWithULong.serializer().descriptor, listOf<ZkpStructureType>(wrappedULongStructure)),
                Arguments.of(ClassWithFloat.serializer().descriptor, listOf<ZkpStructureType>(wrappedFloatStructure)),
                Arguments.of(ClassWithDouble.serializer().descriptor, listOf<ZkpStructureType>(wrappedDoubleStructure)),
                Arguments.of(
                    ClassWithAsciiChar.serializer().descriptor, listOf<ZkpStructureType>(wrappedAsciiCharStructure)
                ),
                Arguments.of(
                    ClassWithUnicodeChar.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedUnicodeCharStructure)
                ),
                Arguments.of(
                    ClassWithAsciiString.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedAsciiStringStructure)
                ),
                Arguments.of(
                    ClassWithUtf8String.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedUtf8StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf16String.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedUtf16StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf32String.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedUtf32StringStructure)
                ),
                Arguments.of(
                    ClassWithNullableInt.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedNullableIntStructure)
                ),
                Arguments.of(
                    ClassWithListOfInt.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedListOfIntStructure)
                ),
                Arguments.of(
                    ClassWithSetOfInt.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedSetOfIntStructure)
                ),
                Arguments.of(
                    ClassWithMapOfStringToInt.serializer().descriptor,
                    listOf<ZkpStructureType>(wrappedMapOfStringIntStructure)
                ),
                Arguments.of(EnumWithNumbers.serializer().descriptor, listOf<ZkpStructureType>()),
                Arguments.of(ClassWithEnum.serializer().descriptor, listOf<ZkpStructureType>(wrappedEnumStructure)),
                Arguments.of(
                    ClassWithPublicKey.serializer().descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPublicKeyStructure, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithAnonymousParty.serializer().descriptor,
                    listOf<ZkpStructureType>(
                        wrappedAnonymousPartyStructure, anonymousPartyEdDsaEd25519Sha512, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithParty.serializer().descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPartyStructure,
                        partyEdDsaEd25519Sha256,
                        cordaX500NameSurrogate,
                        publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithSecureHash.serializer().descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSecureHash, secureHashSha256
                    )
                ),
                Arguments.of(
                    ClassWithSignatureAttachmentConstraint.serializer().descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSignatureAttachmentConstraint,
                        signatureAttachmentConstraintEdDsaEd25519Sha512,
                        publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithHashAttachmentConstraint.serializer().descriptor,
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
