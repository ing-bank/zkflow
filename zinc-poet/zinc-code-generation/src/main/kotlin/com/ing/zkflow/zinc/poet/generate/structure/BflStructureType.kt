package com.ing.zkflow.zinc.poet.generate.structure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for the BFL structure in JSON format.
 */
@Serializable
sealed class BflStructureType {
    /**
     * Number of bytes needed to serialize this [BflStructureType].
     */
    abstract val byteSize: Int

    /**
     * Returns a sequence of all nested [BflStructureType]s.
     */
    open fun flatten(): Sequence<BflStructureType> = sequenceOf(this)

    /**
     * Recursively apply [transform] to all [BflStructureType].
     */
    open fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType = transform(this)

    /**
     * Flatten this [BflStructureType] and replace all inner classes by references.
     */
    fun toFlattenedClassStructure() = flatten()
        .filterIsInstance<BflStructureClass>()
        .map {
            it.rewrite { structureType ->
                replaceInnerClassesByReferences(structureType)
            }
        }

    /**
     * Replace inner classes by references, so that top-level classes remain classes.
     */
    private fun replaceInnerClassesByReferences(structureType: BflStructureType) =
        when (structureType) {
            is BflStructureClass -> structureType.copy(
                fields = structureType.fields.map { field ->
                    field.copy(
                        fieldType = field.fieldType.replaceAllClassesByReferences()
                    )
                }
            )
            else -> structureType
        }

    /**
     * Recursively replaces all [BflStructureClass] by [BflStructureClassRef].
     */
    private fun replaceAllClassesByReferences(): BflStructureType = rewrite {
        when (it) {
            is BflStructureClass -> it.ref()
            else -> it
        }
    }
}

/**
 * BFL Structure for fields in [BflStructureClass].
 */
@Serializable
data class BflStructureField(
    val fieldName: String,
    val fieldType: BflStructureType,
)

/**
 * [BflStructureType] for nullable types.
 */
@Serializable
@SerialName("NULLABLE")
data class BflStructureNullable(
    override val byteSize: Int,
    val innerType: BflStructureType,
) : BflStructureType() {
    override fun flatten(): Sequence<BflStructureType> = super.flatten() + innerType.flatten()
    override fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType = transform(
        copy(
            innerType = innerType.rewrite(transform)
        )
    )
}

/**
 * [BflStructureType] for classes with one or multiple fields.
 */
@Serializable
@SerialName("CLASS")
data class BflStructureClass(
    val className: String,
    val familyClassName: String?,
    override val byteSize: Int,
    val fields: List<BflStructureField>,
) : BflStructureType() {
    init {
        require(byteSize == fields.sumOf { it.fieldType.byteSize }) {
            "Sum of all the fields (${fields.sumOf { it.fieldType.byteSize }}) MUST equal $byteSize"
        }
    }

    override fun flatten(): Sequence<BflStructureType> = super.flatten() +
        fields.asSequence().flatMap { it.fieldType.flatten() }.distinct()

    override fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType {
        return transform(
            copy(
                fields = fields.map {
                    it.copy(
                        fieldType = it.fieldType.rewrite(transform)
                    )
                }
            )
        )
    }

    internal fun ref(): BflStructureClassRef = BflStructureClassRef(className, byteSize)
}

@Serializable
@SerialName("CLASS_REF")
data class BflStructureClassRef(
    val className: String,
    override val byteSize: Int,
) : BflStructureType()

@Serializable
@SerialName("PRIMITIVE")
data class BflStructurePrimitive(
    val className: String,
    override val byteSize: Int,
) : BflStructureType()

@Serializable
@SerialName("LIST")
data class BflStructureList(
    override val byteSize: Int,
    val capacity: Int,
    val elementType: BflStructureType,
) : BflStructureType() {
    override fun flatten(): Sequence<BflStructureType> = super.flatten() + elementType.flatten()
    override fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType {
        return transform(
            copy(
                elementType = elementType.rewrite(transform)
            )
        )
    }
}

@Serializable
@SerialName("ARRAY")
data class BflStructureArray(
    override val byteSize: Int,
    val capacity: Int,
    val elementType: BflStructureType,
) : BflStructureType() {
    override fun flatten(): Sequence<BflStructureType> = super.flatten() + elementType.flatten()
    override fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType {
        return transform(
            copy(
                elementType = elementType.rewrite(transform),
            )
        )
    }
}

@Serializable
@SerialName("MAP")
data class BflStructureMap(
    override val byteSize: Int,
    val capacity: Int,
    val keyType: BflStructureType,
    val valueType: BflStructureType,
) : BflStructureType() {
    override fun flatten(): Sequence<BflStructureType> = super.flatten() + keyType.flatten() + valueType.flatten()
    override fun rewrite(transform: (BflStructureType) -> BflStructureType): BflStructureType {
        return transform(
            copy(
                keyType = keyType.rewrite(transform),
                valueType = valueType.rewrite(transform),
            )
        )
    }
}

@Serializable
@SerialName("STRING")
data class BflStructureString(
    override val byteSize: Int,
    val capacity: Int,
    val encoding: String,
) : BflStructureType()

@Serializable
@SerialName("ENUM")
data class BflStructureEnum(
    val className: String,
) : BflStructureType() {
    override val byteSize: Int = Int.SIZE_BYTES
}

@Serializable
@SerialName("UNIT")
object BflStructureUnit : BflStructureType() {
    override val byteSize: Int = 0
}

@Serializable
@SerialName("BIG_DECIMAL")
data class BflStructureBigDecimal(
    override val byteSize: Int,
    val kind: String,
    val integerSize: Int,
    val fractionSize: Int,
) : BflStructureType()
