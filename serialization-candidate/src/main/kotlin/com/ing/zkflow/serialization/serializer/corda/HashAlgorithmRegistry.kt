package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.crypto.blake2s256
import com.ing.zkflow.crypto.pedersen
import com.ing.zkflow.crypto.zinc
import net.corda.core.crypto.DigestService
import org.slf4j.LoggerFactory

object HashAlgorithmRegistry {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val algId2Name = mutableMapOf<Int, String>()
    private val algId2SecureHashSerializer = mutableMapOf<Int, SecureHashSerializer>()
    private val algName2DigestLength = mutableMapOf<String, Int>()

    init {
        // Ensures zinc, blake2s256 and pedersen algorithms exist in [DigestAlgorithmFactory] before registering here.
        register(DigestService.zinc.hashAlgorithm)
        register(DigestService.blake2s256.hashAlgorithm)
        register(DigestService.pedersen.hashAlgorithm)
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
        val algId = algorithmName.stableId
        val digestLength = DigestService(algorithmName).digestLength // throws IllegalArgumentException when algorithm not supported

        log.debug("Registering algorithm '$algorithmName' with id '$algId' and length '$digestLength'")

        if (algId2Name.containsKey(algId)) {
            throw HashAlgorithmRegistryError.AlgorithmAlreadyRegistered(
                algId,
                algorithmName,
                algId2Name[algId]!! // Safe because of containsKey check
            )
        }
        algId2SecureHashSerializer[algId] = SecureHashSerializer(algorithmName, digestLength)
        algId2Name[algId] = algorithmName
        algName2DigestLength[algorithmName] = digestLength
    }

    private fun registerIfNotKnown(algorithmName: String) {
        if (!algName2DigestLength.containsKey(algorithmName)) {
            register(algorithmName)
        }
    }

    /**
     * Retrieve [SecureHashSerializer] for [algorithmName].
     * Lazily tries to register [algorithmName] when not known yet.
     */
    operator fun get(algorithmName: String): SecureHashSerializer {
        registerIfNotKnown(algorithmName)
        return algId2SecureHashSerializer[algorithmName.stableId]
            ?: throw HashAlgorithmRegistryError.AlgorithmNotRegistered(algorithmName)
    }

    /**
     * Retrieve [SecureHashSerializer] for [algId].
     */
    operator fun get(algId: Int): SecureHashSerializer {
        return algId2SecureHashSerializer[algId]
            ?: throw HashAlgorithmRegistryError.AlgorithmNotRegistered(algId)
    }

    /**
     * Retrieve the length of the digest for [algorithmName].
     * Lazily tries to register [algorithmName] when not known yet.
     */
    fun getDigestLengthOf(algorithmName: String): Int {
        registerIfNotKnown(algorithmName)
        return algName2DigestLength[algorithmName]
            ?: throw HashAlgorithmRegistryError.AlgorithmNotRegistered(algorithmName)
    }
}

sealed class HashAlgorithmRegistryError(message: String) : IllegalArgumentException(message) {
    class AlgorithmAlreadyRegistered(id: Int, algorithmName: String, registeredAlgorithmName: String) :
        HashAlgorithmRegistryError("An algorithm with id '$id' is already registered as '$registeredAlgorithmName', while registering '$algorithmName'")
    class AlgorithmNotRegistered : HashAlgorithmRegistryError {
        constructor(algId: Int) : super("No algorithm registered for id '$algId'")
        constructor(algorithmName: String) : super("Algorithm '$algorithmName' not registered")
    }
}
