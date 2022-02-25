package com.ing.zkflow.common.serialization

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.serialization.infra.AttachmentConstraintMetadata
import com.ing.zkflow.serialization.infra.AttachmentConstraintSerializerRegistry
import com.ing.zkflow.serialization.infra.CommandDataSerializationMetadata
import com.ing.zkflow.serialization.infra.HashAttachmentConstraintSpec
import com.ing.zkflow.serialization.infra.NotarySerializationMetadata
import com.ing.zkflow.serialization.infra.SecureHashSerializationMetadata
import com.ing.zkflow.serialization.infra.SerializerRegistry
import com.ing.zkflow.serialization.infra.SignersSerializationMetadata
import com.ing.zkflow.serialization.infra.TransactionStateSerializationMetadata
import com.ing.zkflow.serialization.infra.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.infra.ZkCommandDataSerializerMapProvider
import com.ing.zkflow.serialization.infra.unwrapSerialization
import com.ing.zkflow.serialization.infra.wrapSerialization
import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SHA256SecureHashSerializationMetadata
import com.ing.zkflow.serialization.serializer.corda.SHA256SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.StateRefSerializer
import com.ing.zkflow.serialization.serializer.corda.TimeWindowSerializer
import com.ing.zkflow.serialization.serializer.corda.TransactionStateSerializer
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.SHA2_256
import net.corda.core.crypto.algorithm
import net.corda.core.identity.Party
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils.Companion.getSchemeIdIfCustomSerializationMagic
import net.corda.core.utilities.ByteSequence
import net.corda.core.utilities.loggerFor
import net.corda.serialization.internal.CordaSerializationMagic
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.ServiceLoader

open class BFLSerializationSchemeCandidate : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 713325187

        object ZkContractStateSerializerRegistry : SerializerRegistry<ContractState>()
        object ZkCommandDataSerializerRegistry : SerializerRegistry<CommandData>()

        init {
            val log = LoggerFactory.getLogger(this::class.java)
            log.debug("Populating `${ZkContractStateSerializerRegistry::class.simpleName}`")
            ServiceLoader.load(ZKContractStateSerializerMapProvider::class.java).flatMap { it.list() }
                .also { if (it.isEmpty()) log.debug("No ZK states founds") }
                .forEach { ZkContractStateSerializerRegistry.register(it.first, it.second) }

            log.debug("Populating `${ZkCommandDataSerializerRegistry::class.simpleName}`")
            ServiceLoader.load(ZkCommandDataSerializerMapProvider::class.java).flatMap { it.list() }
                .also { if (it.isEmpty()) log.debug("No ZK Commands founds") }
                .forEach { ZkCommandDataSerializerRegistry.register(it.first, it.second) }
        }
    }

    override fun getSchemeId() = SCHEME_ID

    private val logger = loggerFor<BFLSerializationSchemeCandidate>()

    private val scheme: BinaryFixedLengthScheme = ByteBinaryFixedLengthScheme

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        logger.trace("Serializing tx component:\t${obj::class}")
        val zkNetworkParameters = context.zkNetworkParameters ?: error("ZKNetworkParameters must be defined")

        return ByteSequence.of(
            when (obj) {
                is SecureHash -> serializeSecureHash(obj)
                is TransactionState<*> -> serializeTransactionState(obj, zkNetworkParameters)
                is CommandData -> serializeCommandData(obj)
                is TimeWindow -> serializeTimeWindow(obj)
                is Party -> serializeNotary(obj, zkNetworkParameters)
                is StateRef -> serializeStateRef(obj, zkNetworkParameters)
                is List<*> -> serializeSignersList(obj, zkNetworkParameters, context.transactionMetadata)
                else -> error("Don't know how to serialize ${obj::class.qualifiedName}")
            }
        )
    }

    override fun <T : Any> deserialize(bytes: ByteSequence, clazz: Class<T>, context: SerializationSchemeContext): T {
        logger.trace("Deserializing tx component:\t$clazz")
        val serializedData = extractValidatedSerializedData(bytes)

        @Suppress("UNCHECKED_CAST") // If we managed to deserialize it, we know it will match T
        return when {
            SecureHash::class.java.isAssignableFrom(clazz) -> deserializeSecureHash(serializedData) as T
            Party::class.java.isAssignableFrom(clazz) -> deserializeNotary(serializedData) as T
            StateRef::class.java.isAssignableFrom(clazz) -> deserializeStateRef(serializedData) as T
            TimeWindow::class.java.isAssignableFrom(clazz) -> deserializeTimeWindow(serializedData) as T
            TransactionState::class.java.isAssignableFrom(clazz) -> deserializeTransactionState(serializedData) as T
            CommandData::class.java.isAssignableFrom(clazz) -> deserializeCommandData(serializedData) as T
            List::class.java.isAssignableFrom(clazz) -> deserializeSignersList(serializedData) as T
            else -> error("Don't know how to deserialize ${clazz.canonicalName}")
        }
    }

    private fun serializeSignersList(
        obj: List<*>,
        zkNetworkParameters: ZKNetworkParameters,
        transactionMetadata: ResolvedZKTransactionMetadata?
    ): ByteArray {
        @Suppress("UNCHECKED_CAST") // This is a conditional cast.
        val signersList = obj as? List<PublicKey> ?: error("Signers: Expected `List<PublicKey>`, Actual `${obj::class.qualifiedName}`")
        val participantSignatureSchemeId = zkNetworkParameters.participantSignatureScheme.schemeNumberID

        /*
         * Using the actual (non-fixed) signers.size when there is no tx metadata is ok,
         * because that means we are serializing a fully non-zkp transaction.
         * The serialized signers list of a non-zkp tx will never be used as input to a circuit,
         * only TransactionStates (outputs) will ever be used as input.
         */
        val numberOfSigners = transactionMetadata?.numberOfSigners ?: signersList.size
        val signersSerializer = FixedLengthListSerializer(
            numberOfSigners,
            PublicKeySerializer(participantSignatureSchemeId)
        )

        return scheme.encodeToBinary(signersSerializer, signersList).wrapSerialization(
            scheme,
            SignersSerializationMetadata(
                numberOfSigners, participantSignatureSchemeId
            ),
            SignersSerializationMetadata.serializer()
        )
    }

    private fun deserializeSignersList(serializedData: ByteArray): List<PublicKey> {
        val (metadata, data) = serializedData.unwrapSerialization(scheme, SignersSerializationMetadata.serializer())
        val serialization = data.toByteArray()

        val signersSerializer = FixedLengthListSerializer(
            metadata.numberOfSigners, PublicKeySerializer(metadata.participantSignatureSchemeId)
        )

        return scheme.decodeFromBinary(signersSerializer, serialization)
    }

    private fun serializeCommandData(obj: CommandData): ByteArray {
        val commandDataSerializer = ZkCommandDataSerializerRegistry[obj::class]

        return scheme.encodeToBinary(commandDataSerializer, obj).wrapSerialization(
            scheme,
            CommandDataSerializationMetadata(
                serializerId = ZkCommandDataSerializerRegistry.identify(obj::class)
            ),
            CommandDataSerializationMetadata.serializer()
        )
    }

    private fun deserializeCommandData(serializedData: ByteArray): CommandData {
        val (metadata, data) = serializedData.unwrapSerialization(scheme, CommandDataSerializationMetadata.serializer())
        val serialization = data.toByteArray()

        val commandDataSerializer = ZkCommandDataSerializerRegistry[metadata.serializerId]

        return scheme.decodeFromBinary(commandDataSerializer, serialization)
    }

    private fun serializeTransactionState(obj: TransactionState<*>, zkNetworkParameters: ZKNetworkParameters): ByteArray {
        val state = obj.data

        // Confirm that the TransactionState fields match the zkNetworkParameters
        zkNetworkParameters.attachmentConstraintType.validate(obj.constraint)
        zkNetworkParameters.notaryInfo.validate(obj.notary)

        val notarySignatureSchemeId = zkNetworkParameters.notaryInfo.signatureScheme.schemeNumberID
        val notarySerializer = PartySerializer(
            cordaSignatureId = notarySignatureSchemeId,
            // TODO transactionMetaData may contain specs for CordaX500
            cordaX500NameSerializer = CordaX500NameSerializer
        )

        val attachmentConstraintType = zkNetworkParameters.attachmentConstraintType
        val attachmentConstraintMetadata = AttachmentConstraintMetadata(
            AttachmentConstraintSerializerRegistry.identify(attachmentConstraintType.kClass),
            generateSpecForHashAttachmentConstraint(attachmentConstraintType),
            getSignatureSchemeIdForSignatureAttachmentConstraint(attachmentConstraintType)
        )
        val attachmentConstraintSerializer =
            AttachmentConstraintSerializerRegistry[attachmentConstraintType.kClass](attachmentConstraintMetadata)

        val transactionStateSerializer = TransactionStateSerializer(
            ZkContractStateSerializerRegistry[state::class], notarySerializer, attachmentConstraintSerializer
        )

        return scheme.encodeToBinary(transactionStateSerializer, obj).wrapSerialization(
            scheme,
            TransactionStateSerializationMetadata(
                serializerId = ZkContractStateSerializerRegistry.identify(state::class),
                notarySignatureSchemeId,
                attachmentConstraintMetadata
            ),
            TransactionStateSerializationMetadata.serializer()
        )
    }

    private fun deserializeTransactionState(serializedData: ByteArray): TransactionState<ContractState> {
        val (metadata, data) = serializedData.unwrapSerialization(scheme, TransactionStateSerializationMetadata.serializer())
        val serialization = data.toByteArray()

        val notarySerializer = PartySerializer(
            cordaSignatureId = metadata.notarySignatureSchemeId, cordaX500NameSerializer = CordaX500NameSerializer
        )
        val attachmentConstraintSerializer = AttachmentConstraintSerializerRegistry[metadata.attachmentConstraintMetadata.serializerId]
            .invoke(metadata.attachmentConstraintMetadata)

        val transactionStateSerializer = TransactionStateSerializer(
            ZkContractStateSerializerRegistry[metadata.serializerId],
            notarySerializer,
            attachmentConstraintSerializer
        )

        return scheme.decodeFromBinary(transactionStateSerializer, serialization)
    }

    private fun serializeTimeWindow(obj: TimeWindow) = scheme.encodeToBinary(TimeWindowSerializer, obj)

    private fun deserializeTimeWindow(serializedData: ByteArray) =
        scheme.decodeFromBinary(TimeWindowSerializer, serializedData)

    private fun serializeStateRef(obj: StateRef, zkNetworkParameters: ZKNetworkParameters): ByteArray {
        require(obj.txhash.algorithm == zkNetworkParameters.digestAlgorithm.algorithm)
        zkNetworkParameters.digestAlgorithm.digestLength

        val secureHashMetadata = when (val txhash = obj.txhash) {
            is SecureHash.SHA256 -> SHA256SecureHashSerializationMetadata
            is SecureHash.HASH -> SecureHashSerializationMetadata(txhash.algorithm, txhash.size)
        }

        val hashSerializer = SecureHashSerializer(secureHashMetadata.algorithm, secureHashMetadata.hashSize)

        return scheme.encodeToBinary(StateRefSerializer(hashSerializer), obj).wrapSerialization(
            scheme, secureHashMetadata, SecureHashSerializationMetadata.serializer()
        )
    }

    private fun deserializeStateRef(serializedData: ByteArray): StateRef {
        val (metadata, data) = serializedData.unwrapSerialization(scheme, SecureHashSerializationMetadata.serializer())
        val serialization = data.toByteArray()

        val secureHashSerializer = SecureHashSerializer(metadata.algorithm, metadata.hashSize)
        val stateRefSerializer = StateRefSerializer(secureHashSerializer)

        return scheme.decodeFromBinary(stateRefSerializer, serialization)
    }

    private fun serializeNotary(obj: Party, zkNetworkParameters: ZKNetworkParameters): ByteArray {
        val notarySignatureSchemeId = zkNetworkParameters.notaryInfo.signatureScheme.schemeNumberID
        val serializer = PartySerializer(
            cordaSignatureId = notarySignatureSchemeId,
            // TODO transactionMetaData may contain specs for CordaX500
            cordaX500NameSerializer = CordaX500NameSerializer
        )

        return scheme.encodeToBinary(serializer, obj).wrapSerialization(
            scheme, NotarySerializationMetadata(notarySignatureSchemeId), NotarySerializationMetadata.serializer()
        )
    }

    private fun deserializeNotary(serializedData: ByteArray): Party {
        val (metadata, data) = serializedData.unwrapSerialization(scheme, NotarySerializationMetadata.serializer())
        val serialization = data.toByteArray()

        val notarySerializer = PartySerializer(
            cordaSignatureId = metadata.notarySignatureSchemeId, cordaX500NameSerializer = CordaX500NameSerializer
        )

        return scheme.decodeFromBinary(notarySerializer, serialization)
    }

    private fun serializeSecureHash(obj: SecureHash): ByteArray {
        // Either AttachmentId, or NetworkParameters hash. Both are hardcoded to be SHA-256 in Corda.
        require(obj.algorithm == SHA2_256) {
            "Serializing Networkparameters hash or AttachmentId: expected hash algorithm $SHA2_256, found ${obj.algorithm}"
        }

        return scheme.encodeToBinary(SHA256SecureHashSerializer, obj)
    }

    private fun deserializeSecureHash(serializedData: ByteArray): SecureHash {
        // Either AttachmentId, or NetworkParameters hash. Both are hardcoded to be SHA-256 in Corda.
        return scheme.decodeFromBinary(SHA256SecureHashSerializer, serializedData)
    }

    private fun generateSpecForHashAttachmentConstraint(attachmentConstraintType: ZKAttachmentConstraintType): HashAttachmentConstraintSpec? =
        (attachmentConstraintType as? ZKAttachmentConstraintType.HashAttachmentConstraintType)?.let {
            HashAttachmentConstraintSpec(
                it.kClass.qualifiedName!!,
                it.digestLength
            )
        }

    private fun getSignatureSchemeIdForSignatureAttachmentConstraint(attachmentConstraintType: ZKAttachmentConstraintType): Int? =
        (attachmentConstraintType as? ZKAttachmentConstraintType.SignatureAttachmentConstraintType)?.signatureScheme?.schemeNumberID

    private fun extractValidatedSerializedData(bytes: ByteSequence): ByteArray {
        val customSerializationMagicLength by lazy { CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(SCHEME_ID).size }
        val foundSerializationMagic = CordaSerializationMagic(bytes.bytes.take(customSerializationMagicLength).toByteArray())

        val schemeIdUsedForSerialization =
            getSchemeIdIfCustomSerializationMagic(foundSerializationMagic)
                ?: error("Can't determine Serialization scheme ID used from serialized data. Found following CordaSerializationMagic: '${foundSerializationMagic.bytes}'")
        require(schemeIdUsedForSerialization == SCHEME_ID) {
            "Can't deserialize transaction component: it was serialized with scheme $schemeIdUsedForSerialization, but current scheme is $SCHEME_ID"
        }
        return bytes.bytes.drop(customSerializationMagicLength).toByteArray()
    }
}
