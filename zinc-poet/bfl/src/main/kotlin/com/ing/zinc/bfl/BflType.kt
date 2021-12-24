package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.WitnessGroupOptions
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
     * Number of bits in bfl-serialized form for instances of this type.
     */
    val bitSize: Int

    /**
     * Name of the type when used to generate a new struct name.
     *
     * e.g. U8Array12 for `[u8; 12]`
     */
    fun typeName(): String

    /**
     * Generate an expression to deserialize this type from a specific witness group.
     *
     * @param witnessGroupOptions the options for this witness group
     * @param offset the bit offset in the bit array
     * @param variablePrefix prefix to use for local variables, to avoid name clashes
     */
    fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String // = SERIALIZED
    ): String

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

    /**
     * Apply [visitor] to all types this [BflType] depends on.
     */
    fun accept(visitor: TypeVisitor)
}

/**
 * Visitor for [BflType]s.
 *
 * [TypeVisitor] can be used to extract information from a [BflType] and all dependant types.
 */
fun interface TypeVisitor {
    fun visitType(type: BflType)
}

internal fun BflType.aOrAn() = if ("AEIOH".contains(id[0])) "an" else "a"

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

fun BflType.toZincId() = id(id)
