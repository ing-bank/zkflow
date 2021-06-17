package io.ivno.collateraltoken.zinc.types.setting

import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.Setting
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SettingEqualsTest {
    private val zincZKService = getZincZKService<SettingEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(setting, setting, true)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different setting should not be equal`(testPair: Pair<Setting<String>, Setting<String>>) {
        performEqualityTest(testPair.first, testPair.second, false)
    }

    private fun performEqualityTest(
        left: Setting<String>,
        right: Setting<String>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(VALUE_LENGTH)
            )
            put(
                "right",
                right.toJsonObject(VALUE_LENGTH)
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        const val VALUE_LENGTH = 7

        val setting = Setting("Property 1", "Value 1")
        private val anotherSettingWithDifferentProperty = Setting("Property 2", "Value 1")
        private val anotherSettingWithDifferentValue = Setting("Property 1", "Value 2")

        @JvmStatic
        fun testData() = listOf(
            Pair(setting, anotherSettingWithDifferentProperty),
            Pair(setting, anotherSettingWithDifferentValue),
            Pair(anotherSettingWithDifferentProperty, anotherSettingWithDifferentValue)
        )

    }
}