package com.ing.zkflow.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ClassesTest {
    class Nested

    @Test
    fun `scopedName should work for regular classes`() {
        ClassesTest::class.scopedName shouldBe "ClassesTest"
    }

    @Test
    fun `scopedName should work for nested classes`() {
        Nested::class.scopedName shouldBe "ClassesTestNested"
    }
}
