package com.ing.zknotary.zinc.types.corda.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeBCSphincs256PublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCSphincs256PublicKeyTest>(
        scheme = Crypto.SPHINCS256_SHA256,
        encodedSize = BCSphincs256Surrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCSphincs256PublicKeyTest>()
}
