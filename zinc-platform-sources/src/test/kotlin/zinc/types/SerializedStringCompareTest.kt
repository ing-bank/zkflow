package zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import kotlinx.serialization.Serializable
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class SerializedStringCompareTest {
    private val log = loggerFor<SerializedStringCompareTest>()
    private val zincZKService = getZincZKService<SerializedStringCompareTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc equals with equal strings`() {
        val data = Data("some.string", "some.string")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with different strings, first smaller`() {
        val data = Data("Some.String", "some.string")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with different strings, first larger`() {
        val data = Data("some.string", "some.strinG")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Serializable
    private data class Data(
        @FixedLength([32])
        val first: String,
        @FixedLength([32])
        val second: String
    ) {
        fun toWitnessJson(): String {
            val dataJson = serialize(this, Data.serializer()).toJsonArray()
            return "{\"witness\":$dataJson}"
        }

        fun getCompareResult(): Int {
            val result = first.compareTo(second)
            return if (result < 0) -1
            else if (result > 0) 1
            else 0
        }
    }
}
