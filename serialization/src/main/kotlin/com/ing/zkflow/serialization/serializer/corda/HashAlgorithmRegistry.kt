package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.crypto.blake2s256
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import org.slf4j.LoggerFactory

object HashAlgorithmRegistry {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val algId2Name = mutableMapOf<Int, String>()

    init {
        // Ensures algos exists in [DigestAlgorithmFactory] before registering here.
        register(DigestService.blake2s256.hashAlgorithm)
        register(SecureHash.SHA2_256)
    }

    // hashCode() is stable for Strings in Java, see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#hashCode()
    private val String.stableId: Int
        get() = hashCode()

    /**
     * Register [algorithmName].
     * @throws IllegalArgumentException when [algorithmName] is not supported
     */
    @Synchronized
    fun register(algorithmName: String) {
        DigestAlgorithmFactory.create(algorithmName)

        val algId = algorithmName.stableId
        log.debug("Registering algorithm '$algorithmName' with id '$algId'")

        if (algId2Name.containsKey(algId)) {
            throw HashAlgorithmRegistryError.AlgorithmAlreadyRegistered(
                algId,
                algorithmName,
                algId2Name[algId]!! // Safe because of containsKey check
            )
        }
        algId2Name[algId] = algorithmName
    }

    /**
     * Retrieve [algId] for [algorithmName].
     */
    operator fun get(algorithmName: String): Int =
        algId2Name.entries.singleOrNull { it.value == algorithmName }?.key
            ?: throw HashAlgorithmRegistryError.AlgorithmNotRegistered(algorithmName)

    /**
     * Retrieve [algorithmName] for [algId].
     */
    operator fun get(algId: Int): String = algId2Name[algId]
        ?: throw HashAlgorithmRegistryError.AlgorithmNotRegistered(algId)
}

sealed class HashAlgorithmRegistryError(message: String) : IllegalArgumentException(message) {
    class AlgorithmAlreadyRegistered(id: Int, algorithmName: String, registeredAlgorithmName: String) :
        HashAlgorithmRegistryError("An algorithm with id '$id' is already registered as '$registeredAlgorithmName', while registering '$algorithmName'")
    class AlgorithmNotRegistered : HashAlgorithmRegistryError {
        constructor(algId: Int) : super("No algorithm registered for id '$algId'")
        constructor(algorithmName: String) : super("Algorithm '$algorithmName' not registered")
    }
}
