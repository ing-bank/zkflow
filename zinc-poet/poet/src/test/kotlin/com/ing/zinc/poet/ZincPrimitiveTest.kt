package com.ing.zinc.poet

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

internal class ZincPrimitiveTest {
    @Test
    fun `getId should return actual zinc type identifier`() {
        for (value in ZincPrimitive.values()) {
            // TODO this is dangerous, because it will not fail when someone renames the enum values.
            value.getId() shouldBe when (value) {
                ZincPrimitive.Unit -> "()"
                else -> value.name.toLowerCase(Locale.getDefault())
            }
        }
    }
}
