package com.ing.zknotary.common.serialization.json.corda

import com.ing.dlt.zkkrypto.util.asUnsigned
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

object SecureHashSerializer : KSerializer<SecureHash> {
    override val descriptor: SerialDescriptor = listSerialDescriptor<Int>()

    override fun serialize(encoder: Encoder, value: SecureHash) {
        // TODO: should these bytes be made unsigned?
        encoder.encodeSerializableValue(ListSerializer(Int.serializer()), value.bytes.map { it.asUnsigned() }.toList())
    }

    override fun deserialize(decoder: Decoder): SecureHash = throw NotImplementedError()
}
