package com.ing.zkflow.common.serialization.json.corda

import com.ing.dlt.zkkrypto.util.asUnsigned
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.PrivacySalt

object PrivacySaltSerializer : KSerializer<PrivacySalt> {
    override val descriptor: SerialDescriptor = listSerialDescriptor<Int>()

    override fun serialize(encoder: Encoder, value: PrivacySalt) {
        // TODO: should these bytes be made unsigned?
        encoder.encodeSerializableValue(ListSerializer(Int.serializer()), value.bytes.map { it.asUnsigned() }.toList())
    }

    override fun deserialize(decoder: Decoder): PrivacySalt = throw NotImplementedError()
}
