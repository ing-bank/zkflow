package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflStruct
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
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zkflow.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

@ZKP
private data class WrapsEdDsaParty(val party: @EdDSA AnonymousParty)

@ZKP
private data class WrapsEdDsaPublicKey(val publicKey: @EdDSA PublicKey)

@ZKP
private data class WrapsSignatureAttachmentConstraint(val constraint: @EdDSA SignatureAttachmentConstraint)

private fun BflModule.getSingleFieldType(): BflModule = (this as BflStruct).fields.single().type as BflModule

class StandardTypes(
    private val zincTypeResolver: ZincTypeResolver,
) {
    private val edDsaPartyNotaryModule by lazy { zincTypeResolver.zincTypeOf(WrapsEdDsaParty::class).getSingleFieldType() }
    fun notaryModule(metadata: ResolvedZKCommandMetadata): BflModule {
        require(metadata.network.notary.signatureScheme == Crypto.EDDSA_ED25519_SHA512) {
            "Currently only EDDSA_ED25519_SHA512 is supported as signature scheme for notary."
        }
        return edDsaPartyNotaryModule
    }

    private val edDsaPublicKeySignerModule by lazy { zincTypeResolver.zincTypeOf(WrapsEdDsaPublicKey::class).getSingleFieldType() }
    fun signerModule(metadata: ResolvedZKCommandMetadata): BflModule {
        require(metadata.network.participantSignatureScheme == Crypto.EDDSA_ED25519_SHA512) {
            "Currently only EDDSA_ED25519_SHA512 is supported as signature scheme for signers."
        }
        return edDsaPublicKeySignerModule
    }

    private val signatureAttachmentConstraint by lazy {
        zincTypeResolver.zincTypeOf(WrapsSignatureAttachmentConstraint::class).getSingleFieldType()
    }
    private fun attachmentConstraintModule(metadata: ResolvedZKCommandMetadata): BflModule {
        require(metadata.network.attachmentConstraintType == SignatureAttachmentConstraint::class) {
            "Currently only ${SignatureAttachmentConstraint::class.simpleName} is supported as attachment constraint."
        }
        return signatureAttachmentConstraint
    }

    internal fun getSignerListModule(
        numberOfSigners: Int,
        commandMetadata: ResolvedZKCommandMetadata
    ) = list {
        capacity = numberOfSigners
        elementType = signerModule(commandMetadata)
    }

    internal fun toWitnessGroupOptions(groupName: String, states: Map<BflModule, Int>, commandMetadata: ResolvedZKCommandMetadata): List<WitnessGroupOptions> = states.keys.map {
        WitnessGroupOptions.cordaWrapped(
            "${groupName}_${it.id.camelToSnakeCase()}",
            transactionState(it, commandMetadata)
        )
    }

    internal fun toTransactionStates(states: Map<BflModule, Int>, commandMetadata: ResolvedZKCommandMetadata): Map<BflModule, Int> =
        states.map { (stateType, count) ->
            transactionState(stateType, commandMetadata) to count
        }.toMap()

    internal fun transactionState(stateType: BflType, commandMetadata: ResolvedZKCommandMetadata) = struct {
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
            type = notaryModule(commandMetadata)
        }
        field {
            name = "encumbrance"
            type = option {
                innerType = BflPrimitive.I32
            }
        }
        field {
            name = "constraint"
            type = attachmentConstraintModule(commandMetadata)
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
        // TODO Should be [SecureHash]?
        internal val nonceDigest = BflTypeDef(
            "NonceDigest",
            array {
                capacity = 32 * Byte.SIZE_BITS // TODO size depends on the used hashing algorithm
                elementType = BflPrimitive.Bool
            }
        )
        // TODO Should be [SecureHash]?
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
        internal val componentGroupEnum = enumOf(ComponentGroupEnum::class)
    }
}
