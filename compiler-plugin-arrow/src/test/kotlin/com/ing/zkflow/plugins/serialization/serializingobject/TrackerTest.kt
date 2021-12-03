package com.ing.zkflow.plugins.serialization.serializingobject

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrackerTest {
    @Test
    fun `test tracker`() {
        val name = "test"
        val tracker = Tracker(name, listOf(Coordinate.Numeric()))
        //       1    _   A     _  0
        tracker.next().literal().numeric().toString() shouldBe "${name}_1_A_0"
    }
}
