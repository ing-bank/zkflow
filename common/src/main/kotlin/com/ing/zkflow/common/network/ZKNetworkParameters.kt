package com.ing.zkflow.common.network

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme
import java.nio.file.Path

interface ZKNetworkParameters {
    /**
     * The version of these ZKNetworkParameters.
     *
     * This is used to load the networkparameters used to create a transaction when it is deserialized from storage
     * or when it is received in a backchain.
     */
    val version: Int

    /**
     * The participant [SignatureScheme] type required by this ZKFlow CorDapp. All parties on the network must use this scheme.
     */
    val participantSignatureScheme: SignatureScheme

    /**
     * The attachment constraint type required by this ZKFlow CorDapp for all states on its network.
     */
    val attachmentConstraintType: ZKAttachmentConstraintType

    val notaryInfo: ZKNotaryInfo

    /**
     * The digest algorithm to use for transaction Merkle tree hashing.
     */
    val digestAlgorithm: DigestAlgorithm

    /**
     * The serialization scheme to use for transaction component serialization.
     */
    val serializationSchemeId: Int

    /**
     * These settings control generation of debug information.
     */
    val debugSettings: DebugSettings
}

/**
 * Settings related to generating debug information.
 */
interface DebugSettings {
    /**
     * Flag controlling whether the serialization structure at runtime should be dumped into a file.
     * Note that this will be done for every serialization action, which might negatively impact performance.
     */
    val dumpSerializationStructure: Boolean

    /**
     * The directory where debug information will be stored.
     */
    fun debugDirectory(): Path
}
