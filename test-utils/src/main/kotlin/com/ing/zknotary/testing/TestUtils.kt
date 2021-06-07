package com.ing.zknotary.testing

import com.ing.zknotary.common.zkp.ZKNulls.fixedKeyPair
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto.DEFAULT_SIGNATURE_SCHEME
import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.unspecifiedCountry
import net.corda.testing.core.TestIdentity
import java.io.File
import java.time.Duration

public fun TestIdentity.Companion.fixed(
    organisation: String,
    signatureScheme: SignatureScheme = DEFAULT_SIGNATURE_SCHEME
): TestIdentity {

    val fixedKeyPair = fixedKeyPair(signatureScheme)
    return TestIdentity(
        CordaX500Name(
            organisation,
            fixedKeyPair.public.toStringShort(),
            CordaX500Name.unspecifiedCountry
        ),
        fixedKeyPair
    )
}

public inline fun <reified T : Any> getZincZKService(
    buildTimeout: Duration = Duration.ofSeconds(5),
    setupTimeout: Duration = Duration.ofSeconds(300),
    provingTimeout: Duration = Duration.ofSeconds(300),
    verificationTimeout: Duration = Duration.ofSeconds(1)
): ZincZKService {
    val zincTestFolder = T::class.java.name
        .substringAfterLast("zinc.types")
        .replace(".", File.separator)
    val circuitFolder: String = T::class.java.getResource(zincTestFolder)?.path
        ?: throw IllegalStateException("Zinc test source folder not found: $zincTestFolder")
    return ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = buildTimeout,
        setupTimeout = setupTimeout,
        provingTimeout = provingTimeout,
        verificationTimeout = verificationTimeout,
    )
}

public fun bytesToWitness(bytes: ByteArray): String = buildJsonObject {
    put("witness", bytes.toJsonArray())
}.toString()

public fun Byte.asUnsigned(): Int = this.toInt() and 0xFF

public fun ByteArray.resizeTo(newSize: Int): ByteArray = ByteArray(newSize) { if (it < size) this[it] else 0 }
public fun IntArray.resizeTo(newSize: Int): IntArray = IntArray(newSize) { if (it < size) this[it] else 0 }
public fun String?.toSizedIntArray(size: Int): IntArray = (this ?: "").chars().toArray().resizeTo(size)

public fun ByteArray.toJsonArray(): JsonArray = buildJsonArray { map { add("${it.asUnsigned()}") } }
public fun IntArray.toJsonArray(): JsonArray = buildJsonArray { map { add("$it") } }
