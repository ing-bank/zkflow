package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.dactyloscopy.Fingerprintable
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import java.nio.ByteBuffer

interface ZKContractState : Fingerprintable, ContractState

interface ZKCommandData : Fingerprintable, CommandData {
    val id: Int

    val paddingConfiguration: ComponentPaddingConfiguration

    override fun fingerprint(): ByteArray =
        ByteBuffer.allocate(4).putInt(id).array()
}
