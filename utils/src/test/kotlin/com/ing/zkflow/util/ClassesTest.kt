package com.ing.zkflow.util

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.jvmName

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

    @Test
    fun `jvmClassNamePermutations should work as documented`() {
        "example.MyClass.InnerClass".jvmClassNamePermutations() shouldContainInOrder listOf(
            "example.MyClass.InnerClass",
            "example.MyClass\$InnerClass",
            "example\$MyClass.InnerClass",
            "example\$MyClass\$InnerClass"
        )
    }

    @Test
    fun `tryGetKClass should work correctly for normal classes`() {
        "com.ing.zkflow.util.ClassesTest".tryGetKClass<Any>() shouldBe ClassesTest::class
        ClassesTest::class.jvmName shouldBe "com.ing.zkflow.util.ClassesTest"
    }

    @Test
    fun `tryGetKClass should work correctly for nested classes`() {
        "com.ing.zkflow.util.ClassesTest.Nested".tryGetKClass<Any>() shouldBe Nested::class
        Nested::class.jvmName shouldBe "com.ing.zkflow.util.ClassesTest\$Nested"
    }
}
