package com.ing.zknotary.zinc.types.corda.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeBCECR1PublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCECR1PublicKeyTest>(
        scheme = Crypto.ECDSA_SECP256R1_SHA256,
        encodedSize = BCECSurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCECR1PublicKeyTest>()
}
