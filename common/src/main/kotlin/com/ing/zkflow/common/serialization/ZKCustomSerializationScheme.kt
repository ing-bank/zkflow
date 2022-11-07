package com.ing.zkflow.common.serialization

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.serialization.SerializationSchemeContext

interface ZKCustomSerializationScheme {
    companion object {
        const val CONTEXT_KEY_ZK_NETWORK_PARAMETERS = 192837465
        const val CONTEXT_KEY_TRANSACTION_METADATA = 2
    }
}

val SerializationSchemeContext.zkNetworkParameters: ZKNetworkParameters?
    get() = properties[ZKCustomSerializationScheme.CONTEXT_KEY_ZK_NETWORK_PARAMETERS] as? ZKNetworkParameters

val SerializationSchemeContext.transactionMetadata: ResolvedZKTransactionMetadata?
    get() = properties[ZKCustomSerializationScheme.CONTEXT_KEY_TRANSACTION_METADATA] as? ResolvedZKTransactionMetadata
