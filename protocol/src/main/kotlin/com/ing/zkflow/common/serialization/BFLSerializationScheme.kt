package com.ing.zkflow.common.serialization

import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.network.attachmentConstraintSerializer
import com.ing.zkflow.common.network.notarySerializer
import com.ing.zkflow.common.network.signerSerializer
import com.ing.zkflow.serialization.infra.CommandDataSerializationMetadata
import com.ing.zkflow.serialization.infra.NetworkSerializationMetadata
import com.ing.zkflow.serialization.infra.SecureHashSerializationMetadata
import com.ing.zkflow.serialization.infra.SignersSerializationMetadata
import com.ing.zkflow.serialization.infra.TransactionStateSerializationMetadata
import com.ing.zkflow.serialization.infra.unwrapSerialization
import com.ing.zkflow.serialization.infra.wrapSerialization
import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.ExactLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAlgorithmRegistry
import com.ing.zkflow.serialization.serializer.corda.SHA256SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.StateRefSerializer
import com.ing.zkflow.serialization.serializer.corda.TimeWindowSerializer
import com.ing.zkflow.serialization.serializer.corda.TransactionStateSerializer
import com.ing.zkflow.serialization.toTree
import com.ing.zkflow.util.ensureFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.SHA2_256
import net.corda.core.crypto.algorithm
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import net.corda.core.identity.Party
import net.corda.core.internal.writeText
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils.Companion.getSchemeIdIfCustomSerializationMagic
import net.corda.core.utilities.ByteSequence
import net.corda.core.utilities.loggerFor
import net.corda.serialization.internal.CordaSerializationMagic
import java.nio.file.Files
import java.security.PublicKey

open class BFLSerializationScheme : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 713325187
    }

    override fun getSchemeId() = SCHEME_ID

    private val logger = loggerFor<BFLSerializationScheme>()

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
                is StateRef -> serializeStateRef(obj)
                is List<*> -> serializeSignersList(obj, zkNetworkParameters)
                else -> error("Don't know how to serialize ${obj::class.qualifiedName}")
            }.wrapSerialization(
                scheme,
                NetworkSerializationMetadata(zkNetworkParameters.version),
                NetworkSerializationMetadata.serializer()
            )
        )
    }

    override fun <T : Any> deserialize(bytes: ByteSequence, clazz: Class<T>, context: SerializationSchemeContext): T {
        logger.trace("Deserializing tx component:\t$clazz")
        val wrappedSerializedData = extractValidatedSerializedData(bytes)

        val (metadata, serializedData) = wrappedSerializedData.unwrapSerialization(scheme, NetworkSerializationMetadata.serializer())

        val zkNetworkParameters = ZKNetworkParametersServiceLoader.getVersion(metadata.networkParametersVersion)
            ?: error("ZKNetworkParameters version '${metadata.networkParametersVersion}' not found")

        @Suppress("UNCHECKED_CAST") // If we managed to deserialize it, we know it will match T
        return when {
            SecureHash::class.java.isAssignableFrom(clazz) -> deserializeSecureHash(serializedData) as T
            Party::class.java.isAssignableFrom(clazz) -> deserializeNotary(serializedData, zkNetworkParameters) as T
            StateRef::class.java.isAssignableFrom(clazz) -> deserializeStateRef(serializedData) as T
            TimeWindow::class.java.isAssignableFrom(clazz) -> deserializeTimeWindow(serializedData) as T
            TransactionState::class.java.isAssignableFrom(clazz) -> deserializeTransactionState(serializedData, zkNetworkParameters) as T
            CommandData::class.java.isAssignableFrom(clazz) -> deserializeCommandData(serializedData) as T
            List::class.java.isAssignableFrom(clazz) -> deserializeSignersList(serializedData, zkNetworkParameters) as T
            else -> error("Don't know how to deserialize ${clazz.canonicalName}")
        }
    }

    private fun serializeSignersList(
        obj: List<*>,
        zkNetworkParameters: ZKNetworkParameters
    ): ByteArray {
        @Suppress("UNCHECKED_CAST") // This is a conditional cast.
        val signersList = obj as? List<PublicKey> ?: error("Signers: Expected `List<PublicKey>`, Actual `${obj::class.qualifiedName}`")

        /*
         * Using the actual (non-fixed) signers.size is ok, because we're either serializing a fully
         * non-zkp transaction or the number of signers is validated to match the command metadata in
         * com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata.verifyCommandsAndSigners
         */
        val numberOfSigners = signersList.size
        val signersSerializer = FixedLengthListSerializer(
            numberOfSigners,
            zkNetworkParameters.signerSerializer
        )

        return encodeAndWrap(
            signersList, signersSerializer,
            SignersSerializationMetadata(numberOfSigners),
            SignersSerializationMetadata.serializer()
        )
    }

    private fun deserializeSignersList(
        serializedData: ByteArray,
        zkNetworkParameters: ZKNetworkParameters
    ): List<PublicKey> {
        val (metadata, serialization) = serializedData.unwrapSerialization(scheme, SignersSerializationMetadata.serializer())

        val signersSerializer = FixedLengthListSerializer(
            metadata.numberOfSigners,
            zkNetworkParameters.signerSerializer
        )

        return scheme.decodeFromBinary(signersSerializer, serialization)
    }

    private fun serializeCommandData(obj: CommandData): ByteArray {
        val commandDataSerializer = CommandDataSerializerRegistry[obj::class]

        return encodeAndWrap(
            obj, commandDataSerializer,
            CommandDataSerializationMetadata(
                serializerId = CommandDataSerializerRegistry.identify(obj::class)
            ),
            CommandDataSerializationMetadata.serializer()
        )
    }

    private fun deserializeCommandData(serializedData: ByteArray): CommandData {
        val (metadata, serialization) = serializedData.unwrapSerialization(scheme, CommandDataSerializationMetadata.serializer())

        val commandDataSerializer = CommandDataSerializerRegistry[metadata.serializerId]

        return scheme.decodeFromBinary(commandDataSerializer, serialization)
    }

    private fun serializeTransactionState(obj: TransactionState<*>, zkNetworkParameters: ZKNetworkParameters): ByteArray {
        val state = obj.data

        // Confirm that the TransactionState fields match the zkNetworkParameters
        zkNetworkParameters.attachmentConstraintType.validate(obj.constraint)
        zkNetworkParameters.notaryInfo.validate(obj.notary)

        val stateSerializerId = ContractStateSerializerRegistry.identify(state::class)
        val transactionStateSerializer = getTransactionStateSerializer(zkNetworkParameters, stateSerializerId)

        return encodeAndWrap(
            obj, transactionStateSerializer,
            TransactionStateSerializationMetadata(stateSerializerId),
            TransactionStateSerializationMetadata.serializer()
        )
    }

    private fun deserializeTransactionState(serializedData: ByteArray, zkNetworkParameters: ZKNetworkParameters): TransactionState<ContractState> {
        val (metadata, serialization) = serializedData.unwrapSerialization(scheme, TransactionStateSerializationMetadata.serializer())

        val transactionStateSerializer = getTransactionStateSerializer(zkNetworkParameters, metadata.serializerId)

        return scheme.decodeFromBinary(transactionStateSerializer, serialization)
    }

    private fun getTransactionStateSerializer(
        zkNetworkParameters: ZKNetworkParameters,
        stateSerializerId: Int
    ): TransactionStateSerializer<ContractState> = TransactionStateSerializer(
        ContractStateSerializerRegistry[stateSerializerId],
        zkNetworkParameters.notarySerializer,
        zkNetworkParameters.attachmentConstraintSerializer
    )

    private fun serializeTimeWindow(obj: TimeWindow) = scheme.encodeToBinary(TimeWindowSerializer, obj)

    private fun deserializeTimeWindow(serializedData: ByteArray) =
        scheme.decodeFromBinary(TimeWindowSerializer, serializedData)

    private fun serializeStateRef(obj: StateRef): ByteArray {
        val digestAlgorithm = DigestAlgorithmFactory.create(obj.txhash.algorithm)
        val secureHashMetadata = SecureHashSerializationMetadata(HashAlgorithmRegistry[digestAlgorithm.algorithm])

        val secureHashSerializer = SecureHashSerializer(digestAlgorithm)
        val stateRefSerializer = StateRefSerializer(secureHashSerializer)

        return encodeAndWrap(
            obj, stateRefSerializer,
            secureHashMetadata,
            SecureHashSerializationMetadata.serializer()
        )
    }

    private fun deserializeStateRef(serializedData: ByteArray): StateRef {
        val (secureHashMetadata, serialization) = serializedData.unwrapSerialization(scheme, SecureHashSerializationMetadata.serializer())

        val algorithm = HashAlgorithmRegistry[secureHashMetadata.hashAlgorithmId]
        val secureHashSerializer = SecureHashSerializer(DigestAlgorithmFactory.create(algorithm))
        val stateRefSerializer = StateRefSerializer(secureHashSerializer)

        return scheme.decodeFromBinary(stateRefSerializer, serialization)
    }

    private fun serializeNotary(obj: Party, zkNetworkParameters: ZKNetworkParameters): ByteArray {
        return scheme.encodeToBinary(zkNetworkParameters.notarySerializer, obj)
    }

    private fun deserializeNotary(serializedData: ByteArray, zkNetworkParameters: ZKNetworkParameters): Party {
        return scheme.decodeFromBinary(zkNetworkParameters.notarySerializer, serializedData)
    }

    private fun serializeSecureHash(obj: SecureHash): ByteArray {
        // Either AttachmentId, or NetworkParameters hash. Both are hardcoded to be SHA-256 in Corda.
        require(obj.algorithm == SHA2_256) {
            "Serializing NetworkParameters hash or AttachmentId: expected hash algorithm $SHA2_256, found ${obj.algorithm}"
        }

        return scheme.encodeToBinary(SHA256SecureHashSerializer, obj)
    }

    private fun deserializeSecureHash(serializedData: ByteArray): SecureHash {
        // Either AttachmentId, or NetworkParameters hash. Both are hardcoded to be SHA-256 in Corda.
        return scheme.decodeFromBinary(SHA256SecureHashSerializer, serializedData)
    }

    private val customSerializationMagicLength by lazy { CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(SCHEME_ID).size }
    private fun extractValidatedSerializedData(serializedData: ByteSequence): ByteArray {
        val foundSerializationMagic = CordaSerializationMagic(serializedData.bytes.take(customSerializationMagicLength).toByteArray())

        val schemeIdUsedForSerialization =
            getSchemeIdIfCustomSerializationMagic(foundSerializationMagic)
                ?: error("Can't determine Serialization scheme ID used from serialized data. Found following CordaSerializationMagic: '${foundSerializationMagic.bytes}'")
        require(schemeIdUsedForSerialization == SCHEME_ID) {
            "Can't deserialize transaction component: it was serialized with scheme $schemeIdUsedForSerialization, but current scheme is $SCHEME_ID"
        }
        return serializedData.bytes.drop(customSerializationMagicLength).toByteArray()
    }

    private fun <T : Any, M : Any> encodeAndWrap(
        txComponent: T,
        txComponentSerializer: KSerializer<T>,
        metadata: M,
        metadataSerializer: KSerializer<M>
    ): ByteArray {
        // TODO Make debugging output optional, maybe configurable in [ZKNetworkParameters], or with System Property
        saveSerializationStructureForDebug(txComponent, txComponentSerializer, metadata, metadataSerializer)
        return scheme
            .encodeToBinary(txComponentSerializer, txComponent)
            .wrapSerialization(
                scheme,
                metadata,
                metadataSerializer
            )
    }

    /**
     * The temporary directory where schema files will be written.
     * This is a val, so will hold for the whole lifetime of this instance.
     */
    private val tempDirectory by lazy {
        Files.createTempDirectory("zkflow-bfl-structure-")
    }

    private fun <M : Any, T : Any> saveSerializationStructureForDebug(
        txComponent: T,
        txComponentSerializer: KSerializer<T>,
        metadata: M,
        metadataSerializer: KSerializer<M>
    ) {
        val descriptor = buildClassSerialDescriptor(serialNameFor(txComponent, metadata)) {
            element("corda-magic", ExactLengthListSerializer(customSerializationMagicLength, ByteSerializer).descriptor)
            element("network-metadata", NetworkSerializationMetadata.serializer().descriptor)
            element("tx-component-metadata", metadataSerializer.descriptor)
            element("tx-component", txComponentSerializer.descriptor)
        }

        tempDirectory
            .ensureFile("${descriptor.serialName.camelToSnakeCase()}.txt")
            .writeText(
                toTree(descriptor).toString()
            )
    }

    private fun <M : Any, T : Any> serialNameFor(txComponent: T, metadata: M): String {
        val prefix = metadata::class.simpleName!!.removeSuffix("SerializationMetadata")
        val suffix = when (txComponent) {
            is TransactionState<*> -> "${txComponent.data::class.simpleName}"
            is List<*> -> {
                val size = when (metadata) {
                    is SignersSerializationMetadata -> metadata.numberOfSigners
                    else -> error("List is expected to be used for Signers")
                }
                "${txComponent::class.simpleName}$size"
            }
            else -> "${txComponent::class.simpleName}"
        }
        return "$prefix$suffix"
    }
}
