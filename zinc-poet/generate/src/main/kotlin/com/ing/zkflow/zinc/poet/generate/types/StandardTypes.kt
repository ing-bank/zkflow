package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflTypeDef
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enumOf
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zkflow.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.identity.Party
import java.security.PublicKey

class StandardTypes(
    private val zincTypeResolver: ZincTypeResolver,
) {
    val notaryModule: BflModule by lazy {
        zincTypeResolver.zincTypeOf(Party::class) // TODO("PartyEdDSA")
    }

    val signerModule: BflModule by lazy {
        zincTypeResolver.zincTypeOf(PublicKey::class)
    }

    internal fun getSignerListModule(
        numberOfSigners: Int
    ) = list {
        capacity = numberOfSigners
        elementType = signerModule
    }

    internal fun transactionState(stateType: BflType) = struct {
        name = "${stateType.id}TransactionState"
        field {
            name = "data"
            type = stateType
        }
        field {
            name = "contract"
            type = byteArray(CLASS_NAME_SIZE)
        }
        field {
            name = "notary"
            type = notaryModule
        }
        field {
            name = "encumbrance"
            type = option {
                innerType = BflPrimitive.I32
            }
        }
        field {
            name = "constraint"
            type = zincTypeResolver.zincTypeOf(SignatureAttachmentConstraint::class) // TODO("SignatureAttachmentConstraint")
        }
    }

    internal fun stateAndRef(stateType: BflType) = struct {
        name = "${stateType.id}StateAndRef"
        field {
            name = "state"
            type = transactionState(stateType)
        }
        field {
            name = "reference"
            type = stateRef
        }
    }

    companion object {
        private val instant = struct {
            name = "Instant"
            field {
                name = "seconds"
                type = BflPrimitive.U64
            }
            field {
                name = "nanos"
                type = BflPrimitive.U32
            }
        }
        internal val timeWindow = struct {
            name = "TimeWindow"
            field {
                name = "from_time"
                type = option {
                    innerType = instant
                }
            }
            field {
                name = "until_time"
                type = option {
                    innerType = instant
                }
            }
        }
        internal val nonceDigest = BflTypeDef(
            "NonceDigest",
            array {
                capacity = 32 * Byte.SIZE_BITS // TODO size depends on the used hashing algorithm
                elementType = BflPrimitive.Bool
            }
        )
        internal val privacySalt = BflTypeDef(
            "PrivacySalt",
            array {
                capacity = 32 * Byte.SIZE_BITS // TODO size depends on the used hashing algorithm
                elementType = BflPrimitive.Bool
            }
        )
        internal val secureHash = struct {
            name = "SecureHash"
            field {
                name = "algorithm"
                type = BflPrimitive.U8
            }
            field {
                name = "bytes"
                type = byteArray(SecureHashSurrogate.BYTES_SIZE)
            }
        }
        internal val stateRef = struct {
            name = "StateRef"
            field {
                name = "hash"
                type = secureHash
            }
            field {
                name = "index"
                type = BflPrimitive.U32
            }
        }
        internal val componentGroupEnum = enumOf(ComponentGroupEnum::class)
    }
}