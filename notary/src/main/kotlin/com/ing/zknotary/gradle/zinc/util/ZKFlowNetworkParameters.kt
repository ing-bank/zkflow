package com.ing.zknotary.gradle.zinc.util

import net.corda.core.crypto.Crypto

object ZKFlowNetworkParameters {
    val notaryKeySchemeCodename = Crypto.EDDSA_ED25519_SHA512.schemeCodeName
    val signerKeySchemeCodename = Crypto.EDDSA_ED25519_SHA512.schemeCodeName
}
