package com.ing.zknotary.common.zkp

import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

object ZKNulls {
    object NULL_PUBLIC_KEY : PublicKey, Comparable<PublicKey> {
        override fun getAlgorithm() = "NULL"
        override fun getEncoded() = ByteArray(44){0}
        override fun getFormat() = "NULL"
        override fun compareTo(other: PublicKey): Int = if (other == NULL_PUBLIC_KEY) 0 else -1
        override fun toString() = "NULL_KEY"
    }

    val NULL_ANONYMOUS_PARTY = AnonymousParty(NULL_PUBLIC_KEY)
    val NULL_PARTY = Party(CordaX500Name.parse("O=BOGUS,L=New York,C=US"), NULL_PUBLIC_KEY)

    /** A signature with a key and value of zero. Useful when you want a signature object that you know won't ever be used. */
//    val NULL_SIGNATURE = TransactionSignature(ByteArray(32), NullPublicKey, SignatureMetadata(1, -1))
}