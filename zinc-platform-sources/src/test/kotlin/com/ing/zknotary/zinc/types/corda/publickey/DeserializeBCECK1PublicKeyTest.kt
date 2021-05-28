package com.ing.zknotary.zinc.types.corda.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.getZincZKService
import kotlinx.serialization.SerialName
import net.corda.core.crypto.Crypto
import kotlin.reflect.full.findAnnotation

class DeserializeBCECK1PublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCECK1PublicKeyTest>(
        scheme = Crypto.ECDSA_SECP256K1_SHA256,
        serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
        encodedSize = BCECSurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCECK1PublicKeyTest>()
}
