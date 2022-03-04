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
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zkflow.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import net.corda.core.contracts.ComponentGroupEnum

class StandardTypes(
    private val zkNetworkParameters: ZKNetworkParameters
) {
    val notaryModule by lazy {
        ZincTypeGenerator.generate(
            PartySerializer(
                zkNetworkParameters.notaryInfo.signatureScheme.schemeNumberID,
                CordaX500NameSerializer
            ).descriptor
        )
    }

    val signerModule by lazy {
        ZincTypeGenerator.generate(
            PublicKeySerializer(
                zkNetworkParameters.participantSignatureScheme.schemeNumberID
            ).descriptor
        )
    }

    fun signerList(metadata: ResolvedZKCommandMetadata) = list {
        capacity = metadata.numberOfSigners
        elementType = signerModule
    }

    private val attachmentConstraintModule: BflType by lazy {
        ZincTypeGenerator.generate(
            when (val attachmentConstraintType = zkNetworkParameters.attachmentConstraintType) {
                ZKAttachmentConstraintType.AlwaysAcceptAttachmentConstraintType -> AlwaysAcceptAttachmentConstraintSerializer.descriptor
                is ZKAttachmentConstraintType.HashAttachmentConstraintType -> HashAttachmentConstraintSerializer(
                    attachmentConstraintType.algorithm.simpleName!!, // TODO this should be a string with algorithmName
                    attachmentConstraintType.digestLength
                ).descriptor
                is ZKAttachmentConstraintType.SignatureAttachmentConstraintType -> SignatureAttachmentConstraintSerializer(
                    attachmentConstraintType.signatureScheme.schemeNumberID
                ).descriptor
            }
        )
    }

    internal fun toWitnessGroupOptions(groupName: String, states: Map<BflModule, Int>): List<WitnessGroupOptions> = states.keys.map {
        WitnessGroupOptions.cordaWrapped(
            "${groupName}_${it.id.camelToSnakeCase()}",
            transactionState(it)
        )
    }

    internal fun toTransactionStates(states: Map<BflModule, Int>): Map<BflModule, Int> =
        states.map { (stateType, count) ->
            transactionState(stateType) to count
        }.toMap()

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
            type = attachmentConstraintModule
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
        internal val digest = BflTypeDef(
            "Digest",
            array {
                capacity = 32 // TODO size depends on the used hashing algorithm
                elementType = BflPrimitive.I8
            }
        )
        internal val privacySalt = BflTypeDef(
            "PrivacySalt",
            array {
                capacity = 32 // Size is [PrivacySalt.MINIMUM_SIZE]
                elementType = BflPrimitive.I8
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
        internal val componentGroupEnum = enumOf(ComponentGroupEnum::class)
    }
}
