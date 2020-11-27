package com.ing.zknotary.common.sized

import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.WrappedList
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
        val shallow: @Sized(7) List<Int>,
        val deep: @Sized(5) List<@Sized(2) List<Int>>
    )

    @Test
    fun `class with list of ints must be sizeable`() {
        val state = ListState(List(3) { 2 }, List(2) { List(1) { 1 } })
        val sized = ListStateSized(state)

        assert(
            sized.shallow is WrappedList &&
                sized.shallow.list.size == 7 &&
                sized.shallow.originalSize == state.shallow.size
        )
        assert(
            sized.deep is WrappedList &&
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
            @Sized(5) List<
                @Sized(2) List<
                    Pair<
                        Int,
                        @Sized(2) List<Int>>>>
    )

    @Test
    fun `class with deep compound types must be sizeable`() {
        val state = DeepCompoundState(listOf(listOf(Pair(19, listOf(84)))))
        val sized = DeepCompoundStateSized(state)

        val l1 = sized.deep
        assert(l1 is WrappedList<*> && l1.list.size == 5 && l1.originalSize == 1)

        val l2 = l1.list[0]
        assert(l2 is WrappedList<*> && l2.list.size == 2 && l2.originalSize == 1)

        val l3 = l2.list[0]
        assert(l3 is Pair<*, *>)

        val l4f = l3.first
        assert(l4f is Int)

        val l4s = l3.second
        assert(l4s is WrappedList<*> && l4s.list.size == 2 && l4s.originalSize == 1)

        val l5 = l4s.list[0]
        assert(l5 is Int)
    }
}
