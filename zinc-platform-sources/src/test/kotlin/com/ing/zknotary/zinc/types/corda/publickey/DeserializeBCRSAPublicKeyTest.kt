package com.ing.zknotary.zinc.types.corda.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeBCRSAPublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCRSAPublicKeyTest>(
        scheme = Crypto.RSA_SHA256,
        encodedSize = BCRSASurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCRSAPublicKeyTest>()
}
