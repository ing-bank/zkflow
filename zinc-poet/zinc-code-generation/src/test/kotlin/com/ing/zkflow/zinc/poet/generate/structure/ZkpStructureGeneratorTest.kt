package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.Surrogate
import com.ing.zkflow.common.serialization.zinc.generation.getSerialDescriptor
import com.ing.zkflow.zinc.poet.generate.ClassWithAnonymousParty
import com.ing.zkflow.zinc.poet.generate.ClassWithAnonymousPartySerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiChar
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiCharSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiString
import com.ing.zkflow.zinc.poet.generate.ClassWithAsciiStringSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithBoolean
import com.ing.zkflow.zinc.poet.generate.ClassWithBooleanSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithByte
import com.ing.zkflow.zinc.poet.generate.ClassWithByteSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.ClassWithClassWithoutFieldsSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithDouble
import com.ing.zkflow.zinc.poet.generate.ClassWithDoubleSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithFloat
import com.ing.zkflow.zinc.poet.generate.ClassWithFloatSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithHashAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithHashAttachmentConstraintSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithInt
import com.ing.zkflow.zinc.poet.generate.ClassWithIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithListOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithListOfIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithLong
import com.ing.zkflow.zinc.poet.generate.ClassWithLongSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithMapOfStringToInt
import com.ing.zkflow.zinc.poet.generate.ClassWithMapOfStringToIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithNullableInt
import com.ing.zkflow.zinc.poet.generate.ClassWithNullableIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithParty
import com.ing.zkflow.zinc.poet.generate.ClassWithPartySerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithPublicKey
import com.ing.zkflow.zinc.poet.generate.ClassWithPublicKeySerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSecureHash
import com.ing.zkflow.zinc.poet.generate.ClassWithSecureHashSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSetOfInt
import com.ing.zkflow.zinc.poet.generate.ClassWithSetOfIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithShort
import com.ing.zkflow.zinc.poet.generate.ClassWithShortSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithSignatureAttachmentConstraint
import com.ing.zkflow.zinc.poet.generate.ClassWithSignatureAttachmentConstraintSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUByte
import com.ing.zkflow.zinc.poet.generate.ClassWithUByteSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUInt
import com.ing.zkflow.zinc.poet.generate.ClassWithUIntSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithULong
import com.ing.zkflow.zinc.poet.generate.ClassWithULongSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUShort
import com.ing.zkflow.zinc.poet.generate.ClassWithUShortSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUnicodeChar
import com.ing.zkflow.zinc.poet.generate.ClassWithUnicodeCharSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf16String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf16StringSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf32String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf32StringSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf8String
import com.ing.zkflow.zinc.poet.generate.ClassWithUtf8StringSerializer
import com.ing.zkflow.zinc.poet.generate.ClassWithoutFields
import com.ing.zkflow.zinc.poet.generate.ClassWithoutFieldsSerializer
import com.ing.zkflow.zinc.poet.generate.MyFamilyMarker
import com.ing.zkflow.zinc.poet.generate.VersionedState
import com.ing.zkflow.zinc.poet.generate.VersionedStateSerializer
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.descriptors.SerialDescriptor
import net.corda.core.identity.CordaX500Name
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
            field: ZkpStructureField? = null,
            serializationId: Int? = null,
        ): ZkpStructureClass = ZkpStructureClass(
            serialName = T::class.getSerialDescriptor().serialName.removeSuffix(Surrogate.GENERATED_SURROGATE_POSTFIX),
            familyClassName = familyClassName,
            serializationId = serializationId,
            byteSize = field?.fieldType?.byteSize ?: 0,
            fields = field?.let { listOf(it) } ?: emptyList()
        )

        private val wrappedVersionedStructure = wrappedStructure<VersionedState>(
            familyClassName = "${MyFamilyMarker::class.qualifiedName}",
            serializationId = 1860209081,
            field = ZkpStructureField("state", ZkpStructurePrimitive("kotlin.Int", 4)),
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
        // private val wrappedEnumStructure = wrappedStructure<ClassWithEnum>(
        //     field = ZkpStructureField("enum", ZkpStructureEnum("${EnumWithNumbers::class.simpleName}"))
        // )
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
                        "${CordaX500Name::class.qualifiedName}",
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
            "${CordaX500Name::class.qualifiedName}",
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
                Arguments.of(VersionedStateSerializer.descriptor, listOf(wrappedVersionedStructure)),
                Arguments.of(ClassWithoutFieldsSerializer.descriptor, listOf(wrappedEmptyClass)),
                Arguments.of(
                    ClassWithClassWithoutFieldsSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUnitStructure, wrappedEmptyClass)
                ),
                Arguments.of(ClassWithBooleanSerializer.descriptor, listOf<ZkpStructureType>(wrappedBoolStructure)),
                Arguments.of(ClassWithByteSerializer.descriptor, listOf<ZkpStructureType>(wrappedByteStructure)),
                Arguments.of(ClassWithUByteSerializer.descriptor, listOf<ZkpStructureType>(wrappedUByteStructure)),
                Arguments.of(ClassWithShortSerializer.descriptor, listOf<ZkpStructureType>(wrappedShortStructure)),
                Arguments.of(ClassWithUShortSerializer.descriptor, listOf<ZkpStructureType>(wrappedUShortStructure)),
                Arguments.of(ClassWithIntSerializer.descriptor, listOf<ZkpStructureType>(wrappedIntStructure)),
                Arguments.of(ClassWithUIntSerializer.descriptor, listOf<ZkpStructureType>(wrappedUIntStructure)),
                Arguments.of(ClassWithLongSerializer.descriptor, listOf<ZkpStructureType>(wrappedLongStructure)),
                Arguments.of(ClassWithULongSerializer.descriptor, listOf<ZkpStructureType>(wrappedULongStructure)),
                Arguments.of(ClassWithFloatSerializer.descriptor, listOf<ZkpStructureType>(wrappedFloatStructure)),
                Arguments.of(ClassWithDoubleSerializer.descriptor, listOf<ZkpStructureType>(wrappedDoubleStructure)),
                Arguments.of(
                    ClassWithAsciiCharSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedAsciiCharStructure)
                ),
                Arguments.of(
                    ClassWithUnicodeCharSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUnicodeCharStructure)
                ),
                Arguments.of(
                    ClassWithAsciiStringSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedAsciiStringStructure)
                ),
                Arguments.of(
                    ClassWithUtf8StringSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf8StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf16StringSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf16StringStructure)
                ),
                Arguments.of(
                    ClassWithUtf32StringSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedUtf32StringStructure)
                ),
                Arguments.of(
                    ClassWithNullableIntSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedNullableIntStructure)
                ),
                Arguments.of(
                    ClassWithListOfIntSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedListOfIntStructure)
                ),
                Arguments.of(
                    ClassWithSetOfIntSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedSetOfIntStructure)
                ),
                Arguments.of(
                    ClassWithMapOfStringToIntSerializer.descriptor,
                    listOf<ZkpStructureType>(wrappedMapOfStringIntStructure)
                ),
                // Arguments.of(EnumWithNumbersSerializer.descriptor, listOf<ZkpStructureType>()),
                // Arguments.of(ClassWithEnumSerializer.descriptor, listOf<ZkpStructureType>(wrappedEnumStructure)),
                Arguments.of(
                    ClassWithPublicKeySerializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPublicKeyStructure, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithAnonymousPartySerializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedAnonymousPartyStructure, anonymousPartyEdDsaEd25519Sha512, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithPartySerializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedPartyStructure, partyEdDsaEd25519Sha256, cordaX500NameSurrogate, publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithSecureHashSerializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSecureHash, secureHashSha256
                    )
                ),
                Arguments.of(
                    ClassWithSignatureAttachmentConstraintSerializer.descriptor,
                    listOf<ZkpStructureType>(
                        wrappedSignatureAttachmentConstraint,
                        signatureAttachmentConstraintEdDsaEd25519Sha512,
                        publicKeyEdDsaEd25519Sha512
                    )
                ),
                Arguments.of(
                    ClassWithHashAttachmentConstraintSerializer.descriptor,
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
