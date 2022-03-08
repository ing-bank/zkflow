package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflTypeDef
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enumOf
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.attachmentConstraintSerializer
import com.ing.zkflow.common.network.notarySerializer
import com.ing.zkflow.common.network.signerSerializer
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.serializer.corda.SHA256SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.TimeWindowSerializer
import com.ing.zkflow.serialization.serializer.corda.TransactionStateSurrogate
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import net.corda.core.contracts.ComponentGroupEnum

class StandardTypes(
    private val zkNetworkParameters: ZKNetworkParameters
) {
    val notaryModule by lazy {
        ZincTypeGenerator.generate(
            zkNetworkParameters.notarySerializer.descriptor
        )
    }

    val signerModule by lazy {
        ZincTypeGenerator.generate(
            zkNetworkParameters.signerSerializer.descriptor
        )
    }

    fun signerList(metadata: ResolvedZKCommandMetadata) = list {
        capacity = metadata.numberOfSigners
        elementType = signerModule
    }

    private val attachmentConstraintModule: BflType by lazy {
        ZincTypeGenerator.generate(zkNetworkParameters.attachmentConstraintSerializer.descriptor)
    }

    internal fun toWitnessGroupOptions(groupName: String, states: List<IndexedState>): List<WitnessGroupOptions> = states
        .map { it.state }
        .distinctBy { it.id }
        .map {
            WitnessGroupOptions.cordaWrapped(
                "${groupName}_${it.id.camelToSnakeCase()}",
                transactionState(it)
            )
        }

    internal fun toTransactionStates(states: List<IndexedState>): List<IndexedState> =
        states.map {
            IndexedState(
                it.index,
                transactionState(it.state)
            )
        }

    internal fun transactionState(stateType: BflType) = struct {
        name = "${stateType.id}TransactionState"
        field {
            name = "data"
            type = stateType
        }
        @Suppress("MagicNumber")
        field {
            name = "contract"
            type = contractClassName
        }
        field {
            name = "notary"
            type = notaryModule
        }
        field {
            name = "encumbrance"
            type = encumbrance
        }
        field {
            name = "constraint"
            type = attachmentConstraintModule
        }
    }

    companion object {
        internal val timeWindow by lazy {
            ZincTypeGenerator.generate(
                TimeWindowSerializer.descriptor
            )
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
        // NetworkParameters hash. Hardcoded to be SHA-256 in Corda.
        val parametersSecureHash by lazy {
            ZincTypeGenerator.generate(
                SHA256SecureHashSerializer.descriptor
            )
        }
        private val contractClassName by lazy {
            ZincTypeGenerator.generate(
                TransactionStateSurrogate.Companion.ContractClassName.descriptor
            )
        }
        private val encumbrance by lazy {
            ZincTypeGenerator.generate(
                TransactionStateSurrogate.Companion.Encumbrance.descriptor
            )
        }
        internal val componentGroupEnum = enumOf(ComponentGroupEnum::class)
    }
}
