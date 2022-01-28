package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.util.Locale

open class PublicKeySerializer(cordaSignatureId: Int) : KSerializerWithDefault<PublicKey> {
    companion object {
        @Suppress("MagicNumber")
        val schemeIdSize = mapOf(
            Crypto.RSA_SHA256.schemeNumberID to 422,
            Crypto.ECDSA_SECP256K1_SHA256.schemeNumberID to 88,
            Crypto.ECDSA_SECP256R1_SHA256.schemeNumberID to 91,
            Crypto.EDDSA_ED25519_SHA512.schemeNumberID to 44,
            Crypto.SPHINCS256_SHA256.schemeNumberID to 1097,
        )

        /**
         * Copies [Crypto.generateKeyPair] but replaces secureRandom with a fixed value.
         */
        fun fixedPublicKey(signatureScheme: SignatureScheme): PublicKey {
            val keyPairGenerator = KeyPairGenerator.getInstance(signatureScheme.algorithmName, Crypto.findProvider(signatureScheme.providerName))
            val insecureRandom = SecureRandom.getInstance("SHA1PRNG")
            insecureRandom.setSeed(ByteArray(1) { 1 })

            if (signatureScheme.algSpec != null)
                keyPairGenerator.initialize(signatureScheme.algSpec, insecureRandom)
            else
                keyPairGenerator.initialize(signatureScheme.keySize!!, insecureRandom)
            return keyPairGenerator.generateKeyPair().public
        }
    }

    private val cordaSignatureScheme = Crypto.findSignatureScheme(cordaSignatureId)
    private val encodedSize = schemeIdSize[cordaSignatureId]
        ?: error("Verify mapping between Corda signature schemes and public key annotations")
    internal val algorithmNameIdentifier = Crypto.findSignatureScheme(cordaSignatureId).schemeCodeName
        .toLowerCase(Locale.getDefault())
        .split("_")
        .joinToString("") {
            it.replace("rsa", "Rsa")
                .replace("dsa", "Dsa")
                .capitalize(Locale.getDefault())
        }

    override val default: PublicKey = fixedPublicKey(cordaSignatureScheme)

    private val strategy = FixedLengthByteArraySerializer(encodedSize)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PublicKey$algorithmNameIdentifier") {
        element("bytes", strategy.descriptor)
    }

    override fun serialize(encoder: Encoder, value: PublicKey) = with(value.encoded) {
        require(size == encodedSize) {
            """
                Expected a public key corresponding to `${cordaSignatureScheme.schemeCodeName}` (${cordaSignatureScheme.desc}).
                Such instances are encodable with $encodedSize bytes.
                Got instance requiring $size bytes for encoding.
            """.trimIndent()
        }

        encoder.encodeSerializableValue(strategy, this)
    }

    override fun deserialize(decoder: Decoder): PublicKey =
        Crypto.decodePublicKey(cordaSignatureScheme, decoder.decodeSerializableValue(strategy))
}
