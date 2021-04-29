package zinc.types

import io.kotest.matchers.shouldBe
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import java.time.Instant
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
    val externalIdJson = externalId.toZincJson(100)
    return "{\"has_external_id\": $hasExternalId, " +
        "\"external_id\": $externalIdJson, " +
        "\"id\": \"${id.leastSignificantBits}\"}"
}

fun LinearPointer<*>.toZincJson(): String {
    val pointerJson = pointer.toZincJson()
    val classNameJson = type.name.toZincJson(192)
    val isResolvedJson = isResolved.toString()
    return "{\"pointer\": $pointerJson, " +
        "\"class_name\": $classNameJson, " +
        "\"is_resolved\": $isResolvedJson}"
}

fun Instant.toZincJson(): String {
    return "{\"seconds\": \"$epochSecond\", " +
        "\"nanos\": \"$nano\"}"
}
