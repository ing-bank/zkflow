package com.ing.zknotary.common.serializer

import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.LedgerTransaction

@CordaService
class NoopZKInputSerializer(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), ZKInputSerializer {
    override fun serializeWitness(tx: LedgerTransaction, signatures: List<ByteArray>) = ByteArray(0)
    override fun serializeInstance(zkTransactionId: SecureHash) = ByteArray(0)
}