package zinc.types

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSupportedAlgorithm
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Date
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

fun ByteArray.toZincJson(size: Int): String {
    val byteArray = ByteArray(SecureHashSurrogate.BYTES_SIZE)
    for (i in indices) {
        byteArray[i] = this[i]
    }

    return """
        {
            "size": "${this.size}",
            "bytes": ${byteArray.toPrettyJSONArray()}
        }
    """.trimIndent()
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

fun Date.toZincJson(): String {
    return "{\"millis\": \"$time\"}"
}

fun ZonedDateTime.toZincJson(): String {
    val zoneIdHash = when (zone) {
        is ZoneOffset -> 0
        else -> zone.id.hashCode()
    }
    return "{\"year\": \"$year\", " +
        "\"month\": \"$monthValue\", " +
        "\"day_of_month\": \"$dayOfMonth\", " +
        "\"hour\": \"$hour\", " +
        "\"minute\": \"$minute\", " +
        "\"second\": \"$second\", " +
        "\"nano_of_second\": \"$nano\", " +
        "\"zone_offset_seconds\": \"${offset.totalSeconds}\", " +
        "\"zone_id_hash\": \"$zoneIdHash\"}"
}

fun TimeWindow.toZincJson(): String {
    val zero = "{\"seconds\": \"0\", \"nanos\": \"0\"}"

    val fromTime = """{
        "is_null": ${this.fromTime == null},
        "instant": ${this.fromTime?.toZincJson() ?: zero}
    }
    """.trimIndent()

    val untilTime = """{
        "is_null": ${this.untilTime == null},
        "instant": ${this.untilTime?.toZincJson() ?: zero}
    }
    """.trimIndent()

    return """{
        "from_time": $fromTime,
        "until_time": $untilTime
    }
    """.trimIndent()
}

fun PrivacySalt.toZincJson() = """{
    "bytes": ${this.bytes.toPrettyJSONArray()}
}
""".trimIndent()

fun SecureHash.toZincJson(): String = """
    {
        "algorithm": "${SecureHashSupportedAlgorithm.fromAlgorithm(algorithm).id}",
        "bytes": ${bytes.toZincJson(SecureHashSurrogate.BYTES_SIZE)}
    }
""".trimIndent()

fun StateRef.toZincJson(): String = """
    {
        "hash": ${txhash.toZincJson()},
        "index": "$index"
    }
""".trimIndent()
