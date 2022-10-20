@file:Suppress("unused")
package kotlin

/**
 * Thrown after invocation of a function or property that was expected to return `Nothing`, but returned something instead.
 */
class KotlinNothingValueException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
