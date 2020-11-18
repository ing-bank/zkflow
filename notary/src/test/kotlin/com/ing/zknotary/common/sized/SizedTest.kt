package com.ing.zknotary.common.sized

import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.WrappedList
import org.junit.Test
import kotlin.reflect.full.memberProperties

class SizedTest {
    @Sized
    class State(
        val simple: Int,
        val shallow: @Sized(7) List<Int>,
        val deep: @Sized(5) List<@Sized(2) List<Int>>
    )

    @Test
    fun `class with list of ints must be sizeable`() {
        val state = State(3, List(3) { 2 }, List(2) { List(1) { 1 } })
        val sized = StateSized(state)

        assert(sized.simple is Int)
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

        println(sized::class.memberProperties)
    }
}
