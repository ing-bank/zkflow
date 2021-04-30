package zinc.types

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import java.time.Duration
import java.time.Instant
import java.util.Currency
import javax.security.auth.x500.X500Principal
import kotlin.streams.toList

fun String?.toZincJson(size: Int): String {
    val result = (this ?: "").chars().toList().toMutableList()
    for (i in result.size until size) {
        result.add(0)
    }
    val charsJson = result.toIntArray().toPrettyJSONArray()
    val sizeJson = "\"${this?.length ?: 0}\""
    return "{\"chars\": $charsJson, " +
        "\"size\": $sizeJson}"
}

/* This method assumes that the mostSignificantBits of the id are 0 */
fun UniqueIdentifier.toZincJson(): String {
    // input validations
    id.mostSignificantBits shouldBe 0

    val hasExternalId = externalId?.let { "true" } ?: "false"
    val externalIdJson = externalId.toZincJson(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH)
    return "{\"has_external_id\": $hasExternalId, " +
        "\"external_id\": $externalIdJson, " +
        "\"id\": \"${id.leastSignificantBits}\"}"
}

fun LinearPointer<*>.toZincJson(): String {
    val pointerJson = pointer.toZincJson()
    val classNameJson = type.name.toZincJson(LinearPointerSurrogate.MAX_CLASS_NAME_SIZE)
    val isResolvedJson = isResolved.toString()
    return "{\"pointer\": $pointerJson, " +
        "\"class_name\": $classNameJson, " +
        "\"is_resolved\": $isResolvedJson}"
}

fun Instant.toZincJson(): String {
    return "{\"seconds\": \"$epochSecond\", " +
        "\"nanos\": \"$nano\"}"
}

fun Duration.toZincJson(): String {
    return "{\"seconds\": \"$seconds\", " +
        "\"nanos\": \"$nano\"}"
}

fun X500Principal.toZincJson(): String {
    val nameJson = name.toZincJson(1024)
    return "{\"name\": $nameJson}"
}

fun Currency.toZincJson(): String {
    val codeJson = currencyCode.toZincJson(3)
    return "{\"code\": $codeJson}"
}
