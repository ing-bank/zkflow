package com.ing.zkflow.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class TreeKtTest {

    @Test
    fun `getValue should successfully return the value of a leaf`() {
        testLeaf.value shouldBe "success"
    }

    @Test
    fun `getValue should successfully return the value of a node`() {
        testNode.value shouldBe "success"
    }

    @Test
    fun `anyValue should work on a leaf in a tree`() {
        testNode.anyValue { it == "failure" } shouldBe true
    }

    @Test
    fun `anyValue should work on the root of a tree`() {
        testNode.anyValue { it == "success" } shouldBe true
    }

    @Test
    fun `anyValue should return false when no matches found`() {
        testNode.anyValue { it == "not found" } shouldBe false
    }

    @Test
    fun `toString should return a text representation of the tree`() {
        val tree = Tree.node<String, String>("root") {
            node("node1") {
                leaf("leaf1")
                leaf("leaf2")
            }
            leaf("leaf3\nwith multiple lines")
        }
        tree.toString() shouldBe """
            root
            ├── node1
            │   ├── leaf1
            │   └── leaf2
            └── ╗ leaf3
                ║ with multiple lines
                ╝
        """.trimIndent()
    }

    companion object {
        val testLeaf = Tree.leaf<String, String>("success")
        val testNode = Tree.node<String, String>("success") {
            leaf("failure")
        }
    }
}
