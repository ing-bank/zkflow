package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zkflow.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.identity.Party

class StandardTypes(private val typeResolver: ZincTypeResolver) {
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
            type = typeResolver.zincTypeOf(Party::class) // TODO("PartyEdDSA")
        }
        field {
            name = "encumbrance"
            type = option {
                innerType = BflPrimitive.I32
            }
        }
        field {
            name = "constraint"
            type = typeResolver.zincTypeOf(SignatureAttachmentConstraint::class) // TODO("SignatureAttachmentConstraint")
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
        internal val nonceDigest = struct {
            name = "NonceDigest"
            field {
                name = "bytes"
                type = array {
                    capacity = 32
                    elementType = BflPrimitive.U8
                }
            }
        }
        internal val privacySalt = struct {
            name = "PrivacySalt"
            field {
                name = "bytes"
                type = array {
                    capacity = 32
                    elementType = BflPrimitive.U8 // TODO shouldn't we keep this as bitarray?
                }
            }
        }
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
    }
}
