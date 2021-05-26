package com.ing.zknotary.zinc.types.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.getZincZKService
import kotlinx.serialization.SerialName
import net.corda.core.crypto.Crypto
import kotlin.reflect.full.findAnnotation

class DeserializeBCSphincs256PublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCSphincs256PublicKeyTest>(
        scheme = Crypto.SPHINCS256_SHA256,
        serialName = BCSphincs256Surrogate::class.findAnnotation<SerialName>()!!.value,
        encodedSize = BCSphincs256Surrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCSphincs256PublicKeyTest>()
}
