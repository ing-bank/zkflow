package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.CircuitMetaData
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.crypto.SecureHash
import java.nio.ByteBuffer

interface ZKCommandData : CommandData {
    val id: Int

    /**
     * Command data must contain padding configuration to produce a witness of the appropriate structure.
     * This witness is a serialization of the padded version of the Corda's component groups.
     * See consts.zn in folders corresponding to the respective commands.
     *
     * This public property will have NO influence on the fingerprint of the implementor
     * as ComponentPaddingConfiguration implements Fingerprintable itself with fingerprint
     * being an empty bytearray.
     */
    val paddingConfiguration: ComponentPaddingConfiguration

    /**
     * The information about the circuit for this command.
     *
     * This public property will have NO influence on the fingerprint of the implementor
     * as ComponentPaddingConfiguration implements Fingerprintable itself with fingerprint
     * being an empty bytearray.
     */
    val circuit: CircuitMetaData

    // TODO
    // This construction of the circuit id is temporary and will be replaced in the subsequent work.
    // The proper id must identify circuit and its version.
    // Also should probably go to CircuitMetaData above
    fun circuitId(): SecureHash = SecureHash.sha256(ByteBuffer.allocate(4).putInt(id).array())
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
