package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Setting
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SettingSurrogate<T: Any>(
    @FixedLength([PROPERTY_LENGTH])
    val property: String,
    val value: @Contextual T
) : Surrogate<Setting<T>> {
    override fun toOriginal() = Setting(property, value)

    companion object {
        const val PROPERTY_LENGTH = 20
    }
}

class SettingSerializer<T: Any>(valueSerializer: KSerializer<T>) :
    SurrogateSerializer<Setting<T>, SettingSurrogate<T>>(
        SettingSurrogate.serializer(valueSerializer),
        { SettingSurrogate(it.property, it.value) }
    )
