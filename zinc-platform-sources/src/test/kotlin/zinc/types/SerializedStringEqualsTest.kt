package zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import kotlinx.serialization.Serializable
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class SerializedStringEqualsTest {
    private val log = loggerFor<SerializedStringEqualsTest>()
    private val zincZKService = getZincZKService<SerializedStringEqualsTest>()

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
        val expected = "${data.first == data.second}"

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with different strings, equal sizes`() {
        val data = Data("Some.String", "some.other.string")
        val input = data.toWitnessJson()
        val expected = "${data.first == data.second}"

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with strings of different sized, first longer`() {
        val data = Data("some.string.extended", "some.string")
        val input = data.toWitnessJson()
        val expected = "${data.first == data.second}"

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with strings of different sized, second longer`() {
        val data = Data("some.string", "some.string.extended")
        val input = data.toWitnessJson()
        val expected = "${data.first == data.second}"

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
    }
}
