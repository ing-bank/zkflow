package io.ivno.collateraltoken.zinc.types

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.serializers.CordaX500NameSerializer
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.testing.toSizedIntArray
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.serialization.PermissionSurrogate
import io.ivno.collateraltoken.serialization.RoleSurrogate
import io.onixlabs.corda.bnms.contract.Permission
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.identity.CordaX500Name

fun Role.toZincJson() = toJsonObject().toString()
fun TokenDescriptor.toZincJson() = toJsonObject().toString()
fun Permission.toZincJson() = toJsonObject().toString()

fun <T: Enum<T>> T.toZincJson() = toJsonObject().toString()

fun String?.toJsonObject(size: Int) = buildJsonObject {
    put("chars", toSizedIntArray(size).toJsonArray())
    put("size", "${this@toJsonObject?.length ?: 0}")
}

fun Role.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(RoleSurrogate.VALUE_LENGTH))
}

fun TokenDescriptor.toJsonObject() = buildJsonObject {
    put("symbol", symbol.toJsonObject(32))
    put("issuer_name", issuerName.toJsonObject())
}

@JvmName("CordaX500NameJsonObject")
fun CordaX500Name.toJsonObject() = buildJsonObject {
    val name = serialize(this@toJsonObject, strategy = CordaX500NameSerializer)
    put("name", name.toJsonArray())
}

fun <T: Enum<T>> T.toJsonObject() = JsonPrimitive(this.ordinal.toString())

fun Permission.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(PermissionSurrogate.VALUE_LENGTH))
}