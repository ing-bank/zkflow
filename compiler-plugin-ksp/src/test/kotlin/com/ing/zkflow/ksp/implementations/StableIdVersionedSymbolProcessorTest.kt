package com.ing.zkflow.ksp.implementations

import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.UnversionedException
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.checkForUnversionedStatesAndCommands
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.commandData
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.contractStates
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.extractMarkers
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.noVersioningRequiredCommandData
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.reportPossibleUnversionedException
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.versioned
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.versionedCommandData
import com.ing.zkflow.ksp.implementations.StableIdVersionedSymbolProcessor.Companion.versionedContractStates
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

data class MyFqn(override val qualifiedName: String) : HasQualifiedName

class StableIdVersionedSymbolProcessorTest {
    @Test
    fun `extractMarkers should extract the markers`() {
        /**
         * Situation:
         * interface A: Versioned
         * class B: Versioned, ZKContractState
         * class C: Versioned, ZKCommandData
         * class D: Versioned, ZKContractState
         * class E: Versioned, ZKCommandData
         */
        val implementations = mapOf(
            versioned to listOf(a, b, c, d, e),
            versionedContractStates to listOf(b, d),
            versionedCommandData to listOf(c, e)
        )
        val actual = extractMarkers(implementations)
        actual shouldBe setOf("A")
    }

    @Test
    fun `extractMarkers should work correctly when only commands are passed`() {
        /**
         * Situation:
         * interface A, B, D: Versioned
         * class C: Versioned, ZKCommandData
         * class E: Versioned, ZKCommandData
         */
        val implementations = mapOf(
            versioned to listOf(a, b, c, d, e),
            versionedCommandData to listOf(c, e)
        )
        val actual = extractMarkers(implementations)
        actual shouldBe setOf("A", "B", "D")
    }

    @Test
    fun `extractMarkers should work correctly when only states are passed`() {
        /**
         * Situation:
         * interface A, C, E: Versioned
         * class B: Versioned, ZKCommandData
         * class D: Versioned, ZKCommandData
         */
        val implementations = mapOf(
            versioned to listOf(a, b, c, d, e),
            versionedContractStates to listOf(b, d)
        )
        val actual = extractMarkers(implementations)
        actual shouldBe setOf("A", "C", "E")
    }

    @Test
    fun `extractMarkers should return emptySet when no matches`() {
        val actual = extractMarkers(emptyMap())
        actual shouldBe emptySet()
    }

    @Test
    fun `reportPossibleUnversionedException should not throw when empty lists`() {
        reportPossibleUnversionedException(emptySet(), emptySet())
    }

    @Test
    fun `reportPossibleUnversionedException should throw a nice error`() {
        shouldThrow<UnversionedException> {
            reportPossibleUnversionedException(
                setOf("A", "B"),
                setOf("C", "D", "E")
            )
        }.message shouldBe """
            ERROR: Unversioned ZKContractState's found: [
                A,
                B,
            ].
            ERROR: Unversioned ZKCommandData's found: [
                C,
                D,
                E,
            ].
            Please ensure every ZKContractState and ZKCommandData implements the Versioned interface.
        """.trimIndent()
    }

    @Test
    fun `checkForUnversionedStatesAndCommands be successful if there are no unversioned classes`() {
        val implementations = mapOf(
            versioned to listOf(a, b, c, d, e),
            versionedContractStates to listOf(b, d),
            versionedCommandData to listOf(c, e),
            contractStates to listOf(b, d),
            commandData to listOf(c, e, f),
            noVersioningRequiredCommandData to listOf(f),
        )
        checkForUnversionedStatesAndCommands(implementations)
    }

    @Test
    fun `checkForUnversionedStatesAndCommands should throw`() {
        val implementations = mapOf(
            versioned to listOf(a, b, c, d, e),
            versionedContractStates to listOf(b),
            versionedCommandData to listOf(c),
            contractStates to listOf(b, d),
            commandData to listOf(c, e, f),
            noVersioningRequiredCommandData to listOf(f),
        )
        shouldThrow<UnversionedException> {
            checkForUnversionedStatesAndCommands(implementations)
        }.message shouldBe """
            ERROR: Unversioned ZKContractState's found: [
                D,
            ].
            ERROR: Unversioned ZKCommandData's found: [
                E,
            ].
            Please ensure every ZKContractState and ZKCommandData implements the Versioned interface.
        """.trimIndent()
    }

    companion object {
        private val a = MyFqn("A")
        private val b = MyFqn("B")
        private val c = MyFqn("C")
        private val d = MyFqn("D")
        private val e = MyFqn("E")
        private val f = MyFqn("F")
    }
}
