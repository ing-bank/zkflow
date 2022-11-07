package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflUnit
import com.ing.zinc.bfl.dsl.BigDecimalBuilder.Companion.bigDecimal
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enum
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.string
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF16
import com.ing.zkflow.annotations.UTF32
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UnicodeChar
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.security.PublicKey

interface MyFamilyMarker : VersionedContractStateGroup, ContractState
@ZKP
class VersionedState(val state: Int) : MyFamilyMarker {
    override val participants: List<AbstractParty> = emptyList()
}
@ZKP
class ClassWithoutFields
@ZKP
data class ClassWithClassWithoutFields(val c: ClassWithoutFields)
@ZKP
data class ClassWithBoolean(val boolean: Boolean)
@ZKP
data class ClassWithByte(val byte: Byte)
@ZKP
data class ClassWithShort(val short: Short)
@ZKP
data class ClassWithInt(val int: Int)
@ZKP
data class ClassWithLong(val long: Long)
@ZKP
data class ClassWithFloat(val float: Float)
@ZKP
data class ClassWithDouble(val double: Double)
@ZKP
data class ClassWithAsciiChar(val asciiChar: @ASCIIChar Char)
@ZKP
data class ClassWithUnicodeChar(val unicodeChar: @UnicodeChar Char)
@ZKP
data class ClassWithAsciiString(val string: @ASCII(8) String)
@ZKP
data class ClassWithUtf8String(val string: @UTF8(8) String)
@ZKP
data class ClassWithUtf16String(val string: @UTF16(8) String)
@ZKP
data class ClassWithUtf32String(val string: @UTF32(8) String)
@ZKP
data class ClassWithNullableInt(val nullableInt: Int?)
@ZKP
data class ClassWithListOfInt(val list: @Size(8) List<Int>)
@ZKP
data class ClassWithSetOfInt(val set: @Size(8) Set<Int>)
@ZKP
data class ClassWithMapOfStringToInt(val map: @Size(8) Map<@UTF8(8) String, Int>)
@ZKP
enum class EnumWithNumbers { ONE, TWO, THREE }
@ZKP
data class ClassWithEnum(val enum: EnumWithNumbers)
@ZKP
data class ClassWithPublicKey(val pk: @EdDSA PublicKey)
@ZKP
data class ClassWithAnonymousParty(val party: @EdDSA AnonymousParty)
@ZKP
data class ClassWithParty(val party: @EdDSA Party)
@ZKP
data class ClassWithSecureHash(val hash: @SHA256 SecureHash)
@ZKP
data class ClassWithSignatureAttachmentConstraint(val constraint: @EdDSA SignatureAttachmentConstraint)

@ZKP
data class ClassWithHashAttachmentConstraint(val constraint: @SHA256 HashAttachmentConstraint)

val structWithUnit = struct {
    name = "ClassWithClassWithoutFields"
    field { name = "c"; type = BflUnit }
}
val structWithBoolean = struct {
    name = "ClassWithBoolean"
    field { name = "boolean"; type = BflPrimitive.Bool }
}
val structWithByte = struct {
    name = "ClassWithByte"
    field { name = "byte"; type = BflPrimitive.I8 }
}
val structWithUByte = struct {
    name = "ClassWithUByte"
    field { name = "ubyte"; type = BflPrimitive.U8 }
}
val structWithShort = struct {
    name = "ClassWithShort"
    field { name = "short"; type = BflPrimitive.I16 }
}
val structWithUShort = struct {
    name = "ClassWithUShort"
    field { name = "ushort"; type = BflPrimitive.U16 }
}
val structWithInt = struct {
    name = "ClassWithInt"
    field { name = "int"; type = BflPrimitive.I32 }
}
val structWithUInt = struct {
    name = "ClassWithUInt"
    field { name = "uint"; type = BflPrimitive.U32 }
}
val structWithLong = struct {
    name = "ClassWithLong"
    field { name = "long"; type = BflPrimitive.I64 }
}
val structWithULong = struct {
    name = "ClassWithULong"
    field { name = "ulong"; type = BflPrimitive.U64 }
}
val structWithFloat = struct {
    name = "ClassWithFloat"
    field {
        name = "float"
        type = bigDecimal {
            integerSize = FixedLengthFloatingPointSerializer.FLOAT_INTEGER_PART_MAX_PRECISION
            fractionSize = FixedLengthFloatingPointSerializer.FLOAT_FRACTION_PART_MAX_PRECISION
            name = FixedLengthFloatingPointSerializer.FLOAT
        }
    }
}
val structWithDouble = struct {
    name = "ClassWithDouble"
    field {
        name = "double"
        type = bigDecimal {
            integerSize = FixedLengthFloatingPointSerializer.DOUBLE_INTEGER_PART_MAX_PRECISION
            fractionSize = FixedLengthFloatingPointSerializer.DOUBLE_FRACTION_PART_MAX_PRECISION
            name = FixedLengthFloatingPointSerializer.DOUBLE
        }
    }
}
val structWithAsciiChar = struct {
    name = "ClassWithAsciiChar"
    field { name = "ascii_char"; type = BflPrimitive.I8 }
}
val structWithUnicodeChar = struct {
    name = "ClassWithUnicodeChar"
    field { name = "unicode_char"; type = BflPrimitive.I16 }
}
val structWithAsciiString = struct {
    name = "ClassWithAsciiString"
    field { name = "string"; type = string(8) }
}
val structWithUtf8String = struct {
    name = "ClassWithUtf8String"
    field { name = "string"; type = string(8) }
}
val structWithUtf16String = struct {
    name = "ClassWithUtf16String"
    field { name = "string"; type = string(8) }
}
val structWithUtf32String = struct {
    name = "ClassWithUtf32String"
    field { name = "string"; type = string(8) }
}
val structWithNullableInt = struct {
    name = "ClassWithNullableInt"
    field {
        name = "nullable_int"
        type = option { innerType = BflPrimitive.I32 }
    }
}
val structWithListOfInt = struct {
    name = "ClassWithListOfInt"
    field {
        name = "list"
        type = list { capacity = 8; elementType = BflPrimitive.I32 }
    }
}
val structWithSetOfInt = struct {
    name = "ClassWithSetOfInt"
    field {
        name = "set"
        type = list { capacity = 8; elementType = BflPrimitive.I32 }
    }
}
val structWithMapOfStringToInt = struct {
    name = "ClassWithMapOfStringToInt"
    field {
        name = "map"
        type = map {
            capacity = 8
            keyType = string(8)
            valueType = BflPrimitive.I32
        }
    }
}
val enumWithNumbers = enum {
    name = "EnumWithNumbers"
    variant("ONE")
    variant("TWO")
    variant("THREE")
}
val publicKeyEdDsa = struct {
    name = "PublicKeyEdDsaEd25519Sha512"
    field {
        name = "bytes"
        type = byteArray(44)
    }
}
val structWithPublicKey = struct {
    name = "ClassWithPublicKey"
    field {
        name = "pk"
        type = publicKeyEdDsa
    }
}
val structWithAnonymousParty = struct {
    name = "ClassWithAnonymousParty"
    field {
        name = "party"
        type = struct {
            name = "AnonymousPartyEdDsaEd25519Sha512"
            field {
                name = "public_key"
                type = publicKeyEdDsa
            }
        }
    }
}
val structWithParty = struct {
    name = "ClassWithParty"
    field {
        name = "party"
        type = struct {
            name = "PartyEdDsaEd25519Sha512"
            field {
                name = "corda_x_500_name"
                type = struct {
                    name = "CordaX500NameSerializer_CordaX500NameSurrogate"
                    field { name = "common_name"; type = option { innerType = string(64) } }
                    field { name = "organisation_unit"; type = option { innerType = string(64) } }
                    field { name = "organisation"; type = string(128) }
                    field { name = "locality"; type = string(64) }
                    field { name = "state"; type = option { innerType = string(64) } }
                    field { name = "country"; type = string(2) }
                }
            }
            field {
                name = "public_key"
                type = publicKeyEdDsa
            }
        }
    }
}
val secureHashSha256 = struct {
    name = "SecureHashSha256"
    field {
        name = "bytes"
        type = byteArray(32)
    }
}
val structWithSecureHash = struct {
    name = "ClassWithSecureHash"
    field {
        name = "hash"
        type = secureHashSha256
    }
}
val structWithSignatureAttachmentConstraint = struct {
    name = "ClassWithSignatureAttachmentConstraint"
    field {
        name = "constraint"
        type = struct {
            name = "SignatureAttachmentConstraintEdDsaEd25519Sha512"
            field {
                name = "key"
                type = publicKeyEdDsa
            }
        }
    }
}
val structWithHashAttachmentConstraint = struct {
    name = "ClassWithHashAttachmentConstraint"
    field {
        name = "constraint"
        type = struct {
            name = "HashAttachmentConstraint"
            field {
                name = "attachment_id"
                type = secureHashSha256
            }
        }
    }
}
