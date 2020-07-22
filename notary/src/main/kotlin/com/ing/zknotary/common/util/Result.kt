package com.ing.zknotary.common.util

sealed class Result<T, E> {
    class Success<T, E>(val value: T) : Result<T, E>() {
        override val isSuccess = true
        override val isFailure = false
    }
    class Failure<T, E>(val value: E) : Result<T, E>() {
        override val isSuccess = false
        override val isFailure = true
    }

    abstract val isSuccess: Boolean
    abstract val isFailure: Boolean

    fun <V> map(op: (T) -> V): Result<V, E> {
        return when (this) {
            is Success -> Success(op(value))
            is Failure -> Failure(value)
        }
    }

    fun <V> mapFail(op: (E) -> V): Result<T, V> {
        return when (this) {
            is Success -> Success(value)
            is Failure -> Failure(op(value))
        }
    }

    fun expect(errorMessage: String): T {
        when (this) {
            is Success -> return value
            is Failure -> error(errorMessage)
        }
    }

    override fun toString(): String {
        return when (this) {
            is Success -> "Success($value)"
            is Failure -> "Failure($value)"
        }
    }
}
