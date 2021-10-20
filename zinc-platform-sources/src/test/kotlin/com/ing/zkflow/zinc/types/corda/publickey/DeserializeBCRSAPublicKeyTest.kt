package com.ing.zkflow.zinc.types.corda.publickey

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zkflow.testing.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeBCRSAPublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCRSAPublicKeyTest>(
        scheme = Crypto.RSA_SHA256,
        encodedSize = BCRSASurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCRSAPublicKeyTest>()
}
