package com.ing.zinc.bfl

import com.ing.zinc.poet.ZincType.Companion.id

/**
 * Represents a type.
 */
interface BflType {
    /**
     * Identifier as used in zinc source code.
     *
     * e.g. u16, [u8; 12], ...
     */
    val id: String

    /**
     * Number of bytes in bfl-serialized form for instances of this type.
     */
    val bitSize: Int

    /**
     * Name of the type when used to generate a new struct name.
     */
    fun typeName(): String

    /**
     * Generate an expression to deserialize this type from a byte array.
     * @param options the options for this deserialization
     */
    fun deserializeExpr(options: DeserializationOptions): String

    /**
     * Generate a null expression for this type.
     */
    fun defaultExpr(): String

    /**
     * Generate an equals check for the variables [self] and [other], according to this type.
     */
    fun equalsExpr(self: String, other: String): String

    /**
     * Generate a size expression for this type.
     */
    fun sizeExpr(): String = "$bitSize as u24"

    fun accept(visitor: TypeVisitor)

    companion object {
        const val SERIALIZED_VAR = "serialized"
        const val BITS_PER_BYTE = 8
        const val BYTES_PER_INT = 4
    }
}

fun BflType.aOrAn() = if ("AEIOH".contains(id[0])) "an" else "a"

fun interface TypeVisitor {
    fun visitType(type: BflType)
}

fun Sequence<BflType>.allModules(transform: BflModule.() -> Unit) {
    flatMap { it.resolveAllTypes() }
        .distinctModules()
        .forEach(transform)
}

fun BflType.allModules(transform: BflModule.() -> Unit) {
    resolveAllTypes()
        .distinctModules()
        .forEach(transform)
}

fun Sequence<BflType>.distinctModules(): Sequence<BflModule> = distinctBy { it.id }
    .filterIsInstance<BflModule>()

private fun BflType.resolveAllTypes(): Sequence<BflType> {
    val types = mutableListOf<BflType>()
    val visitor: TypeVisitor = object : TypeVisitor {
        override fun visitType(type: BflType) {
            type.accept(this)
            types.add(type)
        }
    }
    visitor.visitType(this)
    return types.asSequence()
}

internal fun BflType.toZincId() = id(id)
