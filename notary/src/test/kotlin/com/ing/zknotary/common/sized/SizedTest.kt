package com.ing.zknotary.common.sized

import com.ing.zknotary.annotations.FixLength
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.SizedList
import com.ing.zknotary.annotations.UseDefault
import org.junit.Test

@Suppress("USELESS_IS_CHECK")
class SizedTest {
    @Sized
    class SimpleState(val simple: Int)

    @Test
    fun `class with simple types must be sizeable`() {
        val state = SimpleState(3)
        val sized = SimpleStateSized(state)

        assert(sized.simple is Int)
    }

    @Sized
    class ListState(
        val shallow: @FixLength(7) List<Int>,
        val deep: @FixLength(5) List<@FixLength(2) List<Int>>
    )

    @Test
    fun `class with list of ints must be sizeable`() {
        val state = ListState(List(3) { 2 }, List(2) { List(1) { 1 } })
        val sized = ListStateSized(state)

        assert(
            sized.shallow is SizedList<*> &&
                sized.shallow.list.size == 7 &&
                sized.shallow.originalSize == state.shallow.size
        )
        assert(
            sized.deep is SizedList<*> &&
                sized.deep.list.size == 5 &&
                sized.deep.originalSize == state.deep.size
        )
    }

    @Sized
    class SimpleCompoundState(
        val pair: Pair<Int, Int>
    )

    @Test
    fun `class with compound types must be sizeable`() {
        val state = SimpleCompoundState(Pair(19, 84))
        val sized = SimpleCompoundStateSized(state)

        assert(
            sized.pair is Pair<Int, Int>
        )
    }

    @Sized
    class ComplexCompoundState(
        val triple: Triple<Int, Int, Int>
    )

    @Test
    fun `class with complex compound types must be sizeable`() {
        val state = ComplexCompoundState(Triple(19, 84, 23))
        val sized = ComplexCompoundStateSized(state)

        assert(
            sized.triple is Triple<Int, Int, Int>
        )
    }

    @Sized
    class DeepCompoundState(
        val deep:
            @FixLength(5) List<
                @FixLength(2) List<
                    Pair<
                        Int,
                        @FixLength(2) List<Int>>>>
    )

    @Test
    fun `class with deep compound types must be sizeable`() {
        val state = DeepCompoundState(listOf(listOf(Pair(19, listOf(84)))))
        val sized = DeepCompoundStateSized(state)

        val l1 = sized.deep
        assert(l1 is SizedList<*> && l1.list.size == 5 && l1.originalSize == 1)

        val l2 = l1.list[0]
        assert(l2 is SizedList<*> && l2.list.size == 2 && l2.originalSize == 1)

        val l3 = l2.list[0]
        assert(l3 is Pair<*, *>)

        val l4f = l3.first
        assert(l4f is Int)

        val l4s = l3.second
        assert(l4s is SizedList<*> && l4s.list.size == 2 && l4s.originalSize == 1)

        val l5 = l4s.list[0]
        assert(l5 is Int)
    }

    @Sized
    class StateL0(val simple: @FixLength(5) List<Int>)

    @Sized
    class StateL1(val complex: @FixLength(5) List<StateL0>)

    @Test
    fun `class with custom sizeable types must be sizeable`() {
        val state = StateL1(listOf(StateL0(listOf(19))))
        val sized = StateL1Sized(state)

        val l1 = sized.complex
        assert(l1 is SizedList<*> && l1.list.size == 5 && l1.originalSize == 1)

        val l1element = l1.list[0]
        assert(l1element is StateL0Sized)

        val l2 = l1element.simple
        assert(l2 is SizedList<*> && l2.list.size == 5 && l2.originalSize == 1)
    }

    class Defaultable(val n: Int) {
        constructor() : this(0)
    }

    @Sized
    class ListWithDefault(
        val simple: @UseDefault Defaultable,
        val shallow: @FixLength(5) List<@UseDefault Defaultable>
    )

    @Test
    fun `class with custom defaultable types must be sizeable`() {
        val state = ListWithDefault(Defaultable(2), listOf())
        val sized = ListWithDefaultSized(state)

        val l1 = sized.shallow
        assert(l1 is SizedList<*> && l1.list.size == 5 && l1.originalSize == 0)

        val element = l1.list[0]
        assert(element is Defaultable && element.n == 0)
    }

    // // TODO
    // @Test
    // fun `Show generated FixedTestState`() {
    //     val original = TestContract.TestState(ZKNulls.NULL_PARTY, 1)
    //     val fixed = FixedTestState(original)
    //     println(fixed::class.memberProperties)
    // }

    // // TODO
    // class FixedLengthGeneratedTest {
    //     private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    //
    //     @Test
    //     @Suppress("UNCHECKED_CAST")
    //     fun `generated fixed state must have identical visible properties`() {
    //         val original = TestContract.TestState(alice.party, 1)
    //         val fixed = FixedTestState(original)
    //         original::class.memberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach {
    //             it as KProperty1<Any, *>
    //             val origVal = it.get(original)
    //
    //             val fixedProp = fixed::class.memberProperties.single { fixedProp -> fixedProp.name == it.name } as KProperty1<Any, *>
    //             val fixedVal = fixedProp.get(fixed)
    //             println("${it.name}: \n\t\t\"$origVal\" \n\t\t\"$fixedVal\"")
    //             assertEquals(origVal, fixedVal)
    //         }
    //     }
    // }
}
