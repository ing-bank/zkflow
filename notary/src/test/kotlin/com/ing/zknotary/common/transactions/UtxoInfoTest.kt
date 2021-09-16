package com.ing.zknotary.common.transactions

import io.kotest.matchers.shouldBe
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import org.junit.jupiter.api.Test

class UtxoInfoTest {
    class Foo(override val participants: List<AbstractParty> = emptyList()) : ContractState

    @Test
    fun `UtxoInfo can load state class from name`() {
        val info = UtxoInfo.build(
            stateRef = StateRef(SecureHash.zeroHash, 0),
            serializedContents = ByteArray(0),
            nonce = SecureHash.zeroHash,
            stateClass = Foo::class
        )

        info.stateClass shouldBe Foo::class
    }
}
