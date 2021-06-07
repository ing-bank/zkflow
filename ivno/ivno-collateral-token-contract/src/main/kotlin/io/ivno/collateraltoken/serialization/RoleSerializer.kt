package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.Serializable

@Serializable
data class RoleSurrogate(
    @FixedLength([VALUE_LENGTH])
    var value: String,
) : Surrogate<Role> {
    override fun toOriginal() = Role(value)

    companion object {
        const val VALUE_LENGTH = 20
    }
}

object RoleSerializer : SurrogateSerializer<Role, RoleSurrogate>(
    RoleSurrogate.serializer(),
    { RoleSurrogate(it.value) }
)