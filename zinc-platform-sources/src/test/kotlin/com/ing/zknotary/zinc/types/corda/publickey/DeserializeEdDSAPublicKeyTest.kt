package com.ing.zknotary.zinc.types.corda.publickey

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.getZincZKService
import net.corda.core.crypto.Crypto

class DeserializeEdDSAPublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeEdDSAPublicKeyTest>(
        scheme = Crypto.EDDSA_ED25519_SHA512,
        encodedSize = EdDSASurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeEdDSAPublicKeyTest>()
}
