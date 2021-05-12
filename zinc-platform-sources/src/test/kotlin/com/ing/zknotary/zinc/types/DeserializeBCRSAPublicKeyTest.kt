package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.SerialName
import net.corda.core.crypto.Crypto
import kotlin.reflect.full.findAnnotation

class DeserializeBCRSAPublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCRSAPublicKeyTest>(
        scheme = Crypto.RSA_SHA256,
        serialName = BCRSASurrogate::class.findAnnotation<SerialName>()!!.value,
        encodedSize = BCRSASurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCRSAPublicKeyTest>()
}
