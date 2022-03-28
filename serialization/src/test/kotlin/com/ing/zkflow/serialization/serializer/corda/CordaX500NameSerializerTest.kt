package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CordaX500NameSerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `CordaX500Name must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(CordaX500Names.DefaultCordaX500Name, CordaX500Names.cordaX500Name)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `CordaX500Name's must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(CordaX500Names.DefaultCordaX500Name, CordaX500Names.cordaX500Name).size shouldBe
            engine.serialize(CordaX500Names.DefaultCordaX500Name, CordaX500Name(organisation = "DOWN", locality = "NY", country = "US")).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with CordaX500Names must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(CordaX500Names.serializer(), CordaX500Names.someCordaX500Names)
    }

    @Suppress("ClassName")
    @Serializable
    data class CordaX500Names(
        @Serializable(with = DefaultCordaX500Name::class) val defaultCordaX500Name: CordaX500Name,
        @Serializable(with = CustomCordaX500Name::class) val customCordaX500Name: CordaX500Name,
    ) {
        object DefaultCordaX500Name : WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(CordaX500NameSerializer)
        object CustomCordaX500Name : SurrogateSerializer<CordaX500Name, CordaX500NameSurrogate>(
            CordaX500NameSurrogate.serializer(), { CordaX500NameConverter.from(it) }
        )

        companion object {
            val cordaX500Name = CordaX500Name(organisation = "IN", locality = "AMS", country = "NL")
            private val otherCordaX500Name = CordaX500Name(organisation = "OUT", locality = "AMS", country = "NL")
            val someCordaX500Names = CordaX500Names(cordaX500Name, otherCordaX500Name)
        }
    }
}

@Suppress("ClassName")
@Serializable
data class CordaX500NameSurrogate(
    @Serializable(with = Concat_0::class) val concat: String
) : Surrogate<CordaX500Name> {
    object Concat_0 : FixedLengthStringSerializer(20)

    override fun toOriginal(): CordaX500Name =
        CordaX500Name.parse(concat)
}

object CordaX500NameConverter : ConversionProvider<CordaX500Name, CordaX500NameSurrogate> {
    override fun from(original: CordaX500Name): CordaX500NameSurrogate =
        CordaX500NameSurrogate("$original")
}
