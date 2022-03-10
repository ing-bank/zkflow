package com.ing.zinc.bfl

class BitWitnessBuilder {
    private val parts: MutableList<WitnessPart> = mutableListOf()

    fun bits(vararg bits: Int): BitWitnessBuilder {
        parts.add(WitnessPart.Bits(bits))
        return this
    }

    fun bytes(vararg bytes: Int): BitWitnessBuilder {
        parts.add(WitnessPart.Bytes(bytes))
        return this
    }

    fun build(): String {
        return parts.flatMap {
            it.generateBooleanSequence()
        }.joinToString {
            it.toString()
        }
    }
}

private sealed class WitnessPart {
    abstract fun generateBooleanSequence(): List<Boolean>

    class Bits(private val bits: IntArray) : WitnessPart() {
        init {
            bits.forEach {
                if (it < 0 || it > 1) {
                    throw IllegalArgumentException("Only '0' and '1' are allowed in bits part")
                }
            }
        }

        override fun generateBooleanSequence(): List<Boolean> {
            return bits.flatMap {
                listOf(
                    false, false, false, false,
                    false, false, false, it == 1
                )
            }
        }
    }

    class Bytes(private val bytes: IntArray) : WitnessPart() {
        init {
            bytes.forEach {
                if (it < 0 || it > 255) {
                    throw IllegalArgumentException("Only values in the range [0, 255] are allowed in bytes part")
                }
            }
        }

        override fun generateBooleanSequence(): List<Boolean> {
            return bytes.flatMap { byteValue ->
                0.rangeTo(7)
                    .reversed()
                    .map {
                        val mask = 1 shl it
                        (byteValue and mask) != 0
                    }
            }
        }
    }
}
