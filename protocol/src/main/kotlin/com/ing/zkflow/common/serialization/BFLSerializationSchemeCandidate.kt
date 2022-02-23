package com.ing.zkflow.common.serialization

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.serialization.infra.AttachmentConstraintMetadata
import com.ing.zkflow.serialization.infra.AttachmentConstraintSerializerMap
import com.ing.zkflow.serialization.infra.CommandDataSerializationMetadata
import com.ing.zkflow.serialization.infra.HashAttachmentConstraintSpec
import com.ing.zkflow.serialization.infra.NotarySerializationMetadata
import com.ing.zkflow.serialization.infra.SecureHashSerializationMetadata
import com.ing.zkflow.serialization.infra.SerializerMap
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
import com.ing.zkflow.serialization.serializer.corda.DummyCommandDataSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.StateRefSerializer
import com.ing.zkflow.serialization.serializer.corda.TimeWindowSerializer
import com.ing.zkflow.serialization.serializer.corda.TransactionStateSerializer
import kotlinx.serialization.KSerializer
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.utilities.ByteSequence
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.DummyCommandData
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.ServiceLoader

@Suppress("UNCHECKED_CAST", "LongMethod", "ComplexMethod")
open class BFLSerializationSchemeCandidate : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 713325187

        object ZkContractStateSerializerMap : SerializerMap<ContractState>()
        object ZkCommandDataSerializerMap : SerializerMap<CommandData>()

        init {
            val log = LoggerFactory.getLogger(this::class.java)

            log.debug("Populating up `${ZkContractStateSerializerMap::class.simpleName}`")
            ServiceLoader.load(ZKContractStateSerializerMapProvider::class.java)
                .flatMap { it.list() }
                .also { if (it.isEmpty()) log.debug("No ZK states founds") }
                .forEach { ZkContractStateSerializerMap.register(it.first, it.second) }

            log.debug("Populating up `${ZkCommandDataSerializerMap::class.simpleName}`")
            ServiceLoader.load(ZkCommandDataSerializerMapProvider::class.java)
                .flatMap { it.list() }
                .also { if (it.isEmpty()) log.debug("No ZK Commands founds") }
                .forEach { ZkCommandDataSerializerMap.register(it.first, it.second) }
                .also { ZkCommandDataSerializerMap.register(DummyCommandData::class, DummyCommandDataSerializer) }
        }
    }

    override fun getSchemeId() = SCHEME_ID

    private val logger = loggerFor<BFLSerializationSchemeCandidate>()

    private val cordaSerdeMagicLength =
        CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(SCHEME_ID).size

    private val scheme: BinaryFixedLengthScheme = ByteBinaryFixedLengthScheme

    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        logger.trace("Deserializing tx component:\t$clazz")
        val serializedData = bytes.bytes.drop(cordaSerdeMagicLength).toByteArray()

        return when {
            SecureHash::class.java.isAssignableFrom(clazz) -> {
                // TODO we need to know which version to construct
                // options:
                //   - metadata
                //   - zkNetworkParameters
                //
                val (metadata, data) = serializedData.unwrapSerialization(scheme, SecureHashSerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val secureHashSerializer = SecureHashSerializer(metadata.algorithm, metadata.hashSize)

                scheme.decodeFromBinary(secureHashSerializer, serialization) as T
            }

            Party::class.java.isAssignableFrom(clazz) -> {
                // This is notary.
                val (metadata, data) = serializedData.unwrapSerialization(scheme, NotarySerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val notarySerializer = PartySerializer(
                    cordaSignatureId = metadata.notarySignatureSchemeId,
                    cordaX500NameSerializer = CordaX500NameSerializer
                )

                scheme.decodeFromBinary(notarySerializer, serialization) as T
            }

            StateRef::class.java.isAssignableFrom(clazz) -> {
                val (metadata, data) = serializedData.unwrapSerialization(scheme, SecureHashSerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val secureHashSerializer = SecureHashSerializer(metadata.algorithm, metadata.hashSize)
                val stateRefSerializer = StateRefSerializer(secureHashSerializer)

                scheme.decodeFromBinary(stateRefSerializer, serialization) as T
            }

            TimeWindow::class.java.isAssignableFrom(clazz) -> {
                scheme.decodeFromBinary(TimeWindowSerializer, serializedData) as T
            }

            TransactionState::class.java.isAssignableFrom(clazz) -> {
                val (metadata, data) = serializedData.unwrapSerialization(scheme, TransactionStateSerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val stateSerializer = ZkContractStateSerializerMap.retrieve(metadata.serializerId)
                val notarySerializer = PartySerializer(
                    cordaSignatureId = metadata.notarySignatureSchemeId,
                    cordaX500NameSerializer = CordaX500NameSerializer
                )
                val attachmentConstraintSerializer = AttachmentConstraintSerializerMap
                    .retrieve(metadata.attachmentConstraintMetadata.serializerId)
                    .invoke(metadata.attachmentConstraintMetadata) as KSerializer<AttachmentConstraint>

                val transactionStateSerializer = TransactionStateSerializer(stateSerializer, notarySerializer, attachmentConstraintSerializer)

                scheme.decodeFromBinary(transactionStateSerializer, serialization) as T
            }

            CommandData::class.java.isAssignableFrom(clazz) -> {
                val (metadata, data) = serializedData.unwrapSerialization(scheme, CommandDataSerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val commandDataSerializer = ZkCommandDataSerializerMap.retrieve(metadata.serializerId)

                scheme.decodeFromBinary(commandDataSerializer, serialization) as T
            }

            List::class.java.isAssignableFrom(clazz) -> {
                // This case will be triggered when commands will be reconstructed.
                // This involves deserialization of the SIGNERS_GROUP to reconstruct commands.
                val (metadata, data) = serializedData.unwrapSerialization(scheme, SignersSerializationMetadata.serializer())
                val serialization = data.toByteArray()

                val signersSerializer = FixedLengthListSerializer(
                    metadata.numberOfSigners,
                    PublicKeySerializer(metadata.participantSignatureSchemeId)
                )

                scheme.decodeFromBinary(signersSerializer, serialization) as T
            }

            else -> error("Me no deserialize. Me sad. ${clazz.canonicalName}")
        }
    }

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        logger.trace("Serializing tx component:\t${obj::class}")

        val transactionMetadata =
            context.properties[ZKCustomSerializationScheme.CONTEXT_KEY_TRANSACTION_METADATA] as? ResolvedZKTransactionMetadata
        val zkNetworkParameters =
            context.properties[ZKCustomSerializationScheme.CONTEXT_KEY_ZK_NETWORK_PARAMETERS] as? ZKNetworkParameters
                ?: error("ZKNetworkParameters must be defined")

        val serialization = when (obj) {
            is SecureHash -> {
                // TODO Align this whole thing with DigestAlgorithm in NetworkParameters
                //      For example: a hash algorithm should be annotated with HashDigest(val digest: KClass<out DigestAlgorithm>)
                //      that is, every annotation referring to a hash impl must refer to a specific impl
                // TODO Find a way to get expected sizes (NOT ACTUAL as it is now) of hashes.
                val secureHashMetadata = when (obj) {
                    is SecureHash.SHA256 -> SecureHashSerializationMetadata(
                        SecureHashSerializer.sha256Algorithm,
                        SecureHashSerializer.sha256HashLength
                    )
                    is SecureHash.HASH -> SecureHashSerializationMetadata(
                        obj.algorithm,
                        obj.size
                    )
                }

                val secureHashSerializer = SecureHashSerializer(secureHashMetadata.algorithm, secureHashMetadata.hashSize)

                scheme
                    .encodeToBinary(secureHashSerializer, obj)
                    .wrapSerialization(
                        scheme,
                        secureHashMetadata,
                        SecureHashSerializationMetadata.serializer()
                    )
            }

            is TransactionState<*> -> {
                val state = obj.data

                // Confirm that the TransactionState fields match the zkNetworkParameters
                zkNetworkParameters.attachmentConstraintType.validate(obj.constraint)
                zkNetworkParameters.notaryInfo.validate(obj.notary)

                // The following cast is OK, its validity is guaranteed by the inner structure of `ContractStateSerializerMap`.
                // If `[]`-access succeeds, then the cast MUST also succeed.
                @Suppress("UNCHECKED_CAST")
                val stateSerializer = ZkContractStateSerializerMap[state::class] as KSerializer<ContractState>

                val notarySignatureSchemeId = zkNetworkParameters.notaryInfo.signatureScheme.schemeNumberID
                val notarySerializer = PartySerializer(
                    cordaSignatureId = notarySignatureSchemeId,
                    // TODO transactionMetaData may contain specs for CordaX500
                    cordaX500NameSerializer = CordaX500NameSerializer
                )

                val attachmentConstraintType = zkNetworkParameters.attachmentConstraintType
                val attachmentConstraintMetadata = AttachmentConstraintMetadata(
                    AttachmentConstraintSerializerMap.identify(attachmentConstraintType.kClass),
                    generateSpecForHashAttachmentConstraint(attachmentConstraintType),
                    getSignatureSchemeIdForSignatureAttachmentConstraint(attachmentConstraintType)
                )
                val attachmentConstraintSerializer =
                    AttachmentConstraintSerializerMap[attachmentConstraintType.kClass](attachmentConstraintMetadata)
                        as KSerializer<AttachmentConstraint>

                val transactionStateSerializer = TransactionStateSerializer(
                    stateSerializer,
                    notarySerializer,
                    attachmentConstraintSerializer
                )

                scheme
                    .encodeToBinary(transactionStateSerializer, obj)
                    .wrapSerialization(
                        scheme,
                        TransactionStateSerializationMetadata(
                            serializerId = ZkContractStateSerializerMap.identify(state::class),
                            notarySignatureSchemeId,
                            attachmentConstraintMetadata
                        ),
                        TransactionStateSerializationMetadata.serializer()
                    )
            }

            is CommandData -> {
                // The following cast is OK, its validity is guaranteed by the inner structure of `ContractStateSerializerMap`.
                // If `[]`-access succeeds, then the cast MUST also succeed.
                val commandDataSerializer = ZkCommandDataSerializerMap[obj::class] as KSerializer<CommandData>

                scheme
                    .encodeToBinary(commandDataSerializer, obj)
                    .wrapSerialization(
                        scheme,
                        CommandDataSerializationMetadata(
                            serializerId = ZkCommandDataSerializerMap.identify(obj::class)
                        ),
                        CommandDataSerializationMetadata.serializer()
                    )
            }

            is TimeWindow -> {
                scheme.encodeToBinary(TimeWindowSerializer, obj)
            }

            is Party -> {
                // This is notary.
                val notarySignatureSchemeId = zkNetworkParameters.notaryInfo.signatureScheme.schemeNumberID
                val serializer = PartySerializer(
                    cordaSignatureId = notarySignatureSchemeId,
                    // TODO transactionMetaData may contain specs for CordaX500
                    cordaX500NameSerializer = CordaX500NameSerializer
                )

                scheme
                    .encodeToBinary(serializer, obj)
                    .wrapSerialization(
                        scheme,
                        NotarySerializationMetadata(notarySignatureSchemeId),
                        NotarySerializationMetadata.serializer()
                    )
            }

            is StateRef -> {
                val secureHashMetadata = when (val txhash = obj.txhash) {
                    is SecureHash.SHA256 -> SecureHashSerializationMetadata(
                        SecureHashSerializer.sha256Algorithm,
                        SecureHashSerializer.sha256HashLength
                    )
                    is SecureHash.HASH -> SecureHashSerializationMetadata(
                        txhash.algorithm,
                        txhash.size
                    )
                }

                val hashSerializer = SecureHashSerializer(secureHashMetadata.algorithm, secureHashMetadata.hashSize)

                scheme
                    .encodeToBinary(StateRefSerializer(hashSerializer), obj)
                    .wrapSerialization(
                        scheme,
                        secureHashMetadata,
                        SecureHashSerializationMetadata.serializer()
                    )
            }

            is List<*> -> {
                /*
                 * This case will be triggered when SIGNERS_GROUP will be processed.
                 * Components of this group are lists of signers of commands.
                */
                val signers = obj as? List<PublicKey> ?: error("Signers: Expected `List<PublicKey>`, Actual `${obj::class.qualifiedName}`")

                val participantSignatureSchemeId = zkNetworkParameters.participantSignatureScheme.schemeNumberID

                /*
                 * Using the actual (non-fixed) signers.size when there is no tx metadata is ok,
                 * because that means we are serializing a fully non-zkp transaction.
                 * The serialized signers list of a non-zkp tx will never be used as input to a circuit,
                 * only TransactionStates (outputs) will ever be used as input.
                 */
                val numberOfSigners = transactionMetadata?.numberOfSigners ?: signers.size
                val signersSerializer = FixedLengthListSerializer(
                    numberOfSigners,
                    PublicKeySerializer(participantSignatureSchemeId)
                )

                scheme
                    .encodeToBinary(signersSerializer, signers)
                    .wrapSerialization(
                        scheme,
                        SignersSerializationMetadata(
                            numberOfSigners,
                            participantSignatureSchemeId
                        ),
                        SignersSerializationMetadata.serializer()
                    )
            }

            else -> error("Me no serialize, Me sad. ${obj::class.qualifiedName}")
        }

        return ByteSequence.of(serialization)
    }

    private fun generateSpecForHashAttachmentConstraint(attachmentConstraintType: ZKAttachmentConstraintType): HashAttachmentConstraintSpec? =
        (attachmentConstraintType as? ZKAttachmentConstraintType.HashAttachmentConstraintType)
            ?.let { HashAttachmentConstraintSpec(it.kClass.qualifiedName!!, it.digestLength) }

    private fun getSignatureSchemeIdForSignatureAttachmentConstraint(attachmentConstraintType: ZKAttachmentConstraintType): Int? =
        (attachmentConstraintType as? ZKAttachmentConstraintType.SignatureAttachmentConstraintType)
            ?.signatureScheme
            ?.schemeNumberID
}
