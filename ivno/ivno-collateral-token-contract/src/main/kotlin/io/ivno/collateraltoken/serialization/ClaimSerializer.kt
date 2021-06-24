package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("m")
data class ClaimSurrogate<T : Any>(
    @FixedLength([PROPERTY_LENGTH])
    val property: String,
    val value: @Contextual T
) : Surrogate<Claim<T>> {
    override fun toOriginal() = Claim(property, value)

    companion object {
        const val PROPERTY_LENGTH = 20
    }
}

class ClaimSerializer<T: Any>(valueSerializer: KSerializer<T>) :
    SurrogateSerializer<Claim<T>, ClaimSurrogate<T>>(
        ClaimSurrogate.serializer(valueSerializer),
        { ClaimSurrogate(it.property, it.value) }
    )

// String surrogate needed so as to maintain the 1-letter length of the serial name of a polymorphic String
@Serializable
@SerialName("n")
data class StringSurrogate(
    @FixedLength([VALUE_LENGTH])
    val value: String,
) : Surrogate<String> {
    override fun toOriginal() = value

    companion object {
        const val VALUE_LENGTH = 7
    }
}

// surrogate serializer needed when the generic properties of an abstract class are of String type
object MyStringSerializer : SurrogateSerializer<String, StringSurrogate>(
    StringSurrogate.serializer(),
    { StringSurrogate(it) }
)

// Int surrogate needed so as to maintain the 1-letter length of the serial name of a polymorphic Int
@Serializable
@SerialName("o")
data class IntSurrogate(
    val value: Int
) : Surrogate<Int> {
    override fun toOriginal() = value
}

// surrogate serializer needed when the generic properties of an abstract class are of Int type
object MyIntSerializer : SurrogateSerializer<Int, IntSurrogate>(
    IntSurrogate.serializer(),
    { IntSurrogate(it) }
)