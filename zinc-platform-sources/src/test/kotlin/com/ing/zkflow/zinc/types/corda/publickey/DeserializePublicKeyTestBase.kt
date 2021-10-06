package com.ing.zkflow.zinc.types.corda.publickey

import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import java.security.PublicKey

abstract class DeserializePublicKeyTestBase<T : DeserializePublicKeyTestBase<T>>(
    private val scheme: SignatureScheme,
    private val encodedSize: Int
) : DeserializationTestBase<T, DeserializePublicKeyTestBase.Data>(
    { it.data.toZincJson(encodedSize) },
) {

    @Serializable
    data class Data(
        val data: @Polymorphic PublicKey
    )

    fun testData() = listOf(
        Data(Crypto.generateKeyPair(scheme).public)
    )
}
