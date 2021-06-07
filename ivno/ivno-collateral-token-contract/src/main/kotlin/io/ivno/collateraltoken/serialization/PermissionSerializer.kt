package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Permission
import kotlinx.serialization.Serializable

@Serializable
data class PermissionSurrogate(
    @FixedLength([VALUE_LENGTH])
    var value: String,
) : Surrogate<Permission> {
    override fun toOriginal() = Permission(value)

    companion object {
        const val VALUE_LENGTH = 20
    }
}

object PermissionSerializer : SurrogateSerializer<Permission, PermissionSurrogate>(
    PermissionSurrogate.serializer(),
    { PermissionSurrogate(it.value) }
)