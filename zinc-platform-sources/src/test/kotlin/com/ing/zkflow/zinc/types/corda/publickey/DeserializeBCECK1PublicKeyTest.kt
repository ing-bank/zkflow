package com.ing.zkflow.zinc.types.corda.publickey

import com.ing.zkflow.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeBCECK1PublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeBCECK1PublicKeyTest>(
        scheme = Crypto.ECDSA_SECP256K1_SHA256,
        encodedSize = BCECSurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBCECK1PublicKeyTest>()
}
