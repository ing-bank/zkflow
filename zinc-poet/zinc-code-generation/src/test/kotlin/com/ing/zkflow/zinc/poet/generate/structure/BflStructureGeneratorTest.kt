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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class BflStructureGeneratorTest {

    @ParameterizedTest
    @MethodSource("fixturesProvider")
    fun `Generated zinc types should match expected types`(
        descriptor: SerialDescriptor,
        expected: List<BflStructureType>
    ) {
        val actual = BflStructureGenerator.generate(descriptor).toFlattenedClassStructure().distinct().toList()
        println("-> ${jsonFormat.encodeToString(ListSerializer(BflStructureType.serializer()), actual)}")
        actual shouldContainExactly expected
    }

    companion object {
        private val jsonFormat = Json { prettyPrint = true }

        inline fun <reified T> wrappedStructure(
            familyClassName: String? = null,
            field: BflStructureField? = null
        ): BflStructureClass = BflStructureClass(
            className = "${T::class.qualifiedName}",
            familyClassName = familyClassName,
            byteSize = field?.fieldType?.byteSize ?: 0,
            fields = field?.let { listOf(it) } ?: emptyList()
        )

        private val wrappedVersionedStructure = wrappedStructure<VersionedState>(
            field = BflStructureField("state", BflStructurePrimitive("kotlin.Int", 4))
        )
        private val wrappedUnitStructure = wrappedStructure<ClassWithClassWithoutFields>(
            field = BflStructureField("c", BflStructureUnit)
        )
        private val wrappedBoolStructure = wrappedStructure<ClassWithBoolean>(
            field = BflStructureField("boolean", BflStructurePrimitive("kotlin.Boolean", Byte.SIZE_BYTES))
        )
        private val wrappedByteStructure = wrappedStructure<ClassWithByte>(
            field = BflStructureField("byte", BflStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES))
        )
        private val wrappedUByteStructure = wrappedStructure<ClassWithUByte>(
            field = BflStructureField("ubyte", BflStructurePrimitive("kotlin.UByte", UByte.SIZE_BYTES))
        )
        private val wrappedShortStructure = wrappedStructure<ClassWithShort>(
            field = BflStructureField("short", BflStructurePrimitive("kotlin.Short", Short.SIZE_BYTES))
        )
        private val wrappedUShortStructure = wrappedStructure<ClassWithUShort>(
            field = BflStructureField("ushort", BflStructurePrimitive("kotlin.UShort", UShort.SIZE_BYTES))
        )
        private val wrappedIntStructure = wrappedStructure<ClassWithInt>(
            field = BflStructureField("int", BflStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
        )
        private val wrappedUIntStructure = wrappedStructure<ClassWithUInt>(
            field = BflStructureField("uint", BflStructurePrimitive("kotlin.UInt", UInt.SIZE_BYTES))
        )
        private val wrappedLongStructure = wrappedStructure<ClassWithLong>(
            field = BflStructureField("long", BflStructurePrimitive("kotlin.Long", Long.SIZE_BYTES))
        )
        private val wrappedULongStructure = wrappedStructure<ClassWithULong>(
            field = BflStructureField("ulong", BflStructurePrimitive("kotlin.ULong", ULong.SIZE_BYTES))
        )
        private val wrappedFloatStructure = wrappedStructure<ClassWithFloat>(
            field = BflStructureField("float", BflStructureBigDecimal(95, "Float", 39, 46))
        )
        private val wrappedDoubleStructure = wrappedStructure<ClassWithDouble>(
            field = BflStructureField("double", BflStructureBigDecimal(644, "Double", 309, 325))
        )
        private val wrappedAsciiCharStructure = wrappedStructure<ClassWithAsciiChar>(
            field = BflStructureField("asciiChar", BflStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES))
        )
        private val wrappedUnicodeCharStructure = wrappedStructure<ClassWithUnicodeChar>(
            field = BflStructureField("unicodeChar", BflStructurePrimitive("kotlin.Short", Short.SIZE_BYTES))
        )
        private val wrappedAsciiStringStructure = wrappedStructure<ClassWithAsciiString>(
            field = BflStructureField("string", BflStructureString(12, 8, "Ascii"))
        )
        private val wrappedUtf8StringStructure = wrappedStructure<ClassWithUtf8String>(
            field = BflStructureField("string", BflStructureString(12, 8, "Utf8"))
        )
        private val wrappedUtf16StringStructure = wrappedStructure<ClassWithUtf16String>(
            field = BflStructureField("string", BflStructureString(12, 8, "Utf16"))
        )
        private val wrappedUtf32StringStructure = wrappedStructure<ClassWithUtf32String>(
            field = BflStructureField("string", BflStructureString(12, 8, "Utf32"))
        )
        private val wrappedNullableIntStructure = wrappedStructure<ClassWithNullableInt>(
            field = BflStructureField(
                "nullableInt",
                BflStructureNullable(5, BflStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedListOfIntStructure = wrappedStructure<ClassWithListOfInt>(
            field = BflStructureField(
                "list",
                BflStructureList(36, 8, BflStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedSetOfIntStructure = wrappedStructure<ClassWithSetOfInt>(
            field = BflStructureField(
                "set",
                BflStructureList(36, 8, BflStructurePrimitive("kotlin.Int", Int.SIZE_BYTES))
            )
        )
        private val wrappedMapOfStringIntStructure = wrappedStructure<ClassWithMapOfStringToInt>(
            field = BflStructureField(
                "map",
                BflStructureMap(
                    132,
                    8,
                    BflStructureString(12, 8, "Utf8"),
                    BflStructurePrimitive("kotlin.Int", Int.SIZE_BYTES),
                )
            )
        )
        private val wrappedEnumStructure = wrappedStructure<ClassWithEnum>(
            field = BflStructureField("enum", BflStructureEnum("EnumWithNumbers"))
        )
        private val wrappedPublicKeyStructure = wrappedStructure<ClassWithPublicKey>(
            field = BflStructureField("pk", BflStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
        )
        private val wrappedAnonymousPartyStructure = wrappedStructure<ClassWithAnonymousParty>(
            field = BflStructureField("party", BflStructureClassRef("AnonymousPartyEdDsaEd25519Sha512", 48))
        )
        private val wrappedPartyStructure = wrappedStructure<ClassWithParty>(
            field = BflStructureField("party", BflStructureClassRef("PartyEdDsaEd25519Sha512", 461))
        )
        private val wrappedSecureHash = wrappedStructure<ClassWithSecureHash>(
            field = BflStructureField("hash", BflStructureClassRef("SecureHashSha256", 36))
        )
        private val wrappedSignatureAttachmentConstraint = wrappedStructure<ClassWithSignatureAttachmentConstraint>(
            field = BflStructureField("constraint", BflStructureClassRef("SignatureAttachmentConstraintEdDsaEd25519Sha512", 48))
        )
        private val wrappedHashAttachmentConstraint = wrappedStructure<ClassWithHashAttachmentConstraint>(
            field = BflStructureField("constraint", BflStructureClassRef("HashAttachmentConstraint", 36))
        )
        private val anonymousPartyEdDsaEd25519Sha512 = BflStructureClass(
            "AnonymousPartyEdDsaEd25519Sha512", null, 48,
            listOf(
                BflStructureField("publicKey", BflStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
            )
        )
        private val partyEdDsaEd25519Sha256 = BflStructureClass(
            "PartyEdDsaEd25519Sha512", null, 461,
            listOf(
                BflStructureField("cordaX500Name", BflStructureClassRef("com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer.CordaX500NameSurrogate", 413)),
                BflStructureField("publicKey", BflStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48))
            )
        )
        private val publicKeyEdDsaEd25519Sha512 = BflStructureClass(
            "PublicKeyEdDsaEd25519Sha512", null, 48,
            listOf(
                BflStructureField(
                    "bytes",
                    BflStructureArray(
                        48, 44, BflStructurePrimitive("kotlin.Byte", 1)
                    )
                )
            )
        )
        private val secureHashSha256 = BflStructureClass(
            "SecureHashSha256", null, 36,
            listOf(
                BflStructureField(
                    "bytes",
                    BflStructureArray(
                        36, 32, BflStructurePrimitive("kotlin.Byte", 1)
                    )
                )
            )
        )
        private val signatureAttachmentConstraintEdDsaEd25519Sha512 = BflStructureClass(
            "SignatureAttachmentConstraintEdDsaEd25519Sha512", null, 48,
            listOf(
                BflStructureField(
                    "key", BflStructureClassRef("PublicKeyEdDsaEd25519Sha512", 48)
                )
            )
        )
        private val hashAttachmentConstraint = BflStructureClass(
            "HashAttachmentConstraint", null, 36,
            listOf(
                BflStructureField("attachmentId", BflStructureClassRef("SecureHashSha256", 36))
            )
        )
        private val cordaX500NameSurrogate = BflStructureClass(
            "com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer.CordaX500NameSurrogate",
            null,
            413,
            listOf(
                BflStructureField("commonName", BflStructureNullable(69, BflStructureString(68, 64, "Utf8"))),
                BflStructureField("organisationUnit", BflStructureNullable(69, BflStructureString(68, 64, "Utf8"))),
                BflStructureField("organisation", BflStructureString(132, 128, "Utf8")),
                BflStructureField("locality", BflStructureString(68, 64, "Utf8")),
                BflStructureField("state", BflStructureNullable(69, BflStructureString(68, 64, "Utf8"))),
                BflStructureField("country", BflStructureString(6, 2, "Utf8")),
            )
        )

        @JvmStatic
        @Suppress("LongMethod")
        fun fixturesProvider(): List<Arguments> {
            return listOf(
                Arguments.of(VersionedState.serializer().descriptor, listOf(wrappedVersionedStructure)),
                Arguments.of(ClassWithoutFields.serializer().descriptor, emptyList<BflStructureType>()),
                Arguments.of(
                    ClassWithClassWithoutFields.serializer().descriptor,
                    listOf<BflStructureType>(wrappedUnitStructure)
                ),
                Arguments.of(ClassWithBoolean.serializer().descriptor, listOf<BflStructureType>(wrappedBoolStructure)),
                Arguments.of(ClassWithByte.serializer().descriptor, listOf<BflStructureType>(wrappedByteStructure)),
                Arguments.of(ClassWithUByte.serializer().descriptor, listOf<BflStructureType>(wrappedUByteStructure)),
                Arguments.of(ClassWithShort.serializer().descriptor, listOf<BflStructureType>(wrappedShortStructure)),
                Arguments.of(ClassWithUShort.serializer().descriptor, listOf<BflStructureType>(wrappedUShortStructure)),
                Arguments.of(ClassWithInt.serializer().descriptor, listOf<BflStructureType>(wrappedIntStructure)),
                Arguments.of(ClassWithUInt.serializer().descriptor, listOf<BflStructureType>(wrappedUIntStructure)),
                Arguments.of(ClassWithLong.serializer().descriptor, listOf<BflStructureType>(wrappedLongStructure)),
                Arguments.of(ClassWithULong.serializer().descriptor, listOf<BflStructureType>(wrappedULongStructure)),
                Arguments.of(ClassWithFloat.serializer().descriptor, listOf<BflStructureType>(wrappedFloatStructure)),
                Arguments.of(ClassWithDouble.serializer().descriptor, listOf<BflStructureType>(wrappedDoubleStructure)),
                Arguments.of(
                    ClassWithAsciiChar.serializer().descriptor,
                    listOf<BflStructureType>(wrappedAsciiCharStructure)
                ),
                Arguments.of(
                    ClassWithUnicodeChar.serializer().descriptor,
                    listOf<BflStructureType>(wrappedUnicodeCharStructure)
                ),
                Arguments.of(
                    ClassWithAsciiString.serializer().descriptor,
                    listOf<BflStructureType>(wrappedAsciiStringStructure)
                ),
                Arguments.of(
                    ClassWithUtf8String.serializer().descriptor,
                    listOf<BflStructureType>(wrappedUtf8StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf16String.serializer().descriptor,
                    listOf<BflStructureType>(wrappedUtf16StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf32String.serializer().descriptor,
                    listOf<BflStructureType>(wrappedUtf32StringStructure)
                ),
                Arguments.of(
                    ClassWithNullableInt.serializer().descriptor,
                    listOf<BflStructureType>(wrappedNullableIntStructure)
                ),
                Arguments.of(
                    ClassWithListOfInt.serializer().descriptor,
                    listOf<BflStructureType>(wrappedListOfIntStructure)
                ),
                Arguments.of(
                    ClassWithSetOfInt.serializer().descriptor,
                    listOf<BflStructureType>(wrappedSetOfIntStructure)
                ),
                Arguments.of(
                    ClassWithMapOfStringToInt.serializer().descriptor,
                    listOf<BflStructureType>(wrappedMapOfStringIntStructure)
                ),
                Arguments.of(EnumWithNumbers.serializer().descriptor, listOf<BflStructureType>()),
                Arguments.of(ClassWithEnum.serializer().descriptor, listOf<BflStructureType>(wrappedEnumStructure)),
                Arguments.of(
                    ClassWithPublicKey.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedPublicKeyStructure, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithAnonymousParty.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedAnonymousPartyStructure, anonymousPartyEdDsaEd25519Sha512, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithParty.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedPartyStructure, partyEdDsaEd25519Sha256, cordaX500NameSurrogate, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithSecureHash.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedSecureHash, secureHashSha256
                    )
                ),
                Arguments.of(
                    ClassWithSignatureAttachmentConstraint.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedSignatureAttachmentConstraint,
                        signatureAttachmentConstraintEdDsaEd25519Sha512,
                        publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithHashAttachmentConstraint.serializer().descriptor,
                    listOf<BflStructureType>(
                        wrappedHashAttachmentConstraint,
                        hashAttachmentConstraint,
                        secureHashSha256
                    )
                ),
            )
        }
    }
}
