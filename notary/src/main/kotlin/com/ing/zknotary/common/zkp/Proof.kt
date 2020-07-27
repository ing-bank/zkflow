package com.ing.zknotary.common.zkp

data class Proof(val value: ByteArray, val publicData: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Proof

        if (!value.contentEquals(other.value)) return false
        if (!publicData.contentEquals(other.publicData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + publicData.contentHashCode()
        return result
    }
}
