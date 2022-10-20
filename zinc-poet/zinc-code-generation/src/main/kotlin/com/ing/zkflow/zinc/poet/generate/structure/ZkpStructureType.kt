/**
 * This file contains all classes corresponding to the `src/main/zkp/structure.json` file.
 */
package com.ing.zkflow.zinc.poet.generate.structure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base class for the BFL structure in JSON format.
 */
@Serializable
sealed class ZkpStructureType {
    /**
     * Number of bytes needed to serialize this [ZkpStructureType].
     */
    abstract val byteSize: Int

    /**
     * Returns a sequence of all nested [ZkpStructureType]s.
     */
    open fun flatten(): Sequence<ZkpStructureType> = sequenceOf(this)

    /**
     * Recursively apply [transform] to all [ZkpStructureType].
     */
    open fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType = transform(this)

    abstract fun describe(): String

    /**
     * Flatten this [ZkpStructureType] and replace all inner classes by references.
     */
    fun toFlattenedClassStructure() = flatten()
        .filterIsInstance<ZkpStructureClass>()
        .map {
            it.rewrite { structureType ->
                replaceInnerClassesByReferences(structureType)
            }
        }

    /**
     * Replace inner classes by references, so that top-level classes remain classes.
     */
    private fun replaceInnerClassesByReferences(structureType: ZkpStructureType) =
        when (structureType) {
            is ZkpStructureClass -> structureType.copy(
                fields = structureType.fields.map { field ->
                    field.copy(
                        fieldType = field.fieldType.replaceAllClassesByReferences()
                    )
                }
            )
            else -> structureType
        }

    /**
     * Recursively replaces all [ZkpStructureClass] by [ZkpStructureClassRef].
     */
    private fun replaceAllClassesByReferences(): ZkpStructureType = rewrite {
        when (it) {
            is ZkpStructureClass -> it.ref()
            else -> it
        }
    }
}

@Serializable
data class ZkpStructureField(
    val fieldName: String,
    val fieldType: ZkpStructureType,
)

@Serializable
@SerialName("NULLABLE")
data class ZkpStructureNullable(
    override val byteSize: Int,
    val innerType: ZkpStructureType,
) : ZkpStructureType() {
    override fun flatten(): Sequence<ZkpStructureType> = super.flatten() + innerType.flatten()
    override fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType = transform(
        copy(
            innerType = innerType.rewrite(transform)
        )
    )

    override fun describe(): String = "nullable ${innerType.describe()}"
}

@Serializable
@SerialName("CLASS")
data class ZkpStructureClass(
    val serialName: String,
    val familyClassName: String?,
    val serializationId: Int?,
    override val byteSize: Int,
    val fields: List<ZkpStructureField>,
) : ZkpStructureType() {
    init {
        require(byteSize == fields.sumBy { it.fieldType.byteSize }) {
            "Sum of all the fields (${fields.sumBy { it.fieldType.byteSize }}) MUST equal $byteSize"
        }
    }

    override fun flatten(): Sequence<ZkpStructureType> = super.flatten() +
        fields.asSequence().flatMap { it.fieldType.flatten() }.distinct()

    override fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType {
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

    override fun describe(): String = "class $serialName (id: $serializationId)"

    /**
     * When a [ZkpStructureClass] has a [serializationId], then the [serialName] may be changed.
     * This happens for top-level contract states, to make sure that the command always uses the latest version.
     */
    fun isNotChanged(newClass: ZkpStructureClass): Boolean =
        (
            (serializationId == null && serialName == newClass.serialName) ||
                serializationId == newClass.serializationId
            ) &&
            familyClassName == newClass.familyClassName &&
            byteSize == newClass.byteSize &&
            fields == newClass.fields

    fun isChanged(newClass: ZkpStructureClass): Boolean = !isNotChanged(newClass)

    internal fun ref(): ZkpStructureClassRef = ZkpStructureClassRef(serialName, byteSize)
}

/**
 * [ZkpStructureType] for class references.
 * Class references are used to avoid duplicates in the final `src/main/zkp/structure.json` file.
 */
@Serializable
@SerialName("CLASS_REF")
data class ZkpStructureClassRef(
    val serialName: String,
    override val byteSize: Int,
) : ZkpStructureType() {
    override fun describe(): String = "class reference $serialName"
}

@Serializable
@SerialName("PRIMITIVE")
data class ZkpStructurePrimitive(
    val className: String,
    override val byteSize: Int,
) : ZkpStructureType() {
    override fun describe(): String = className
}

@Serializable
@SerialName("LIST")
data class ZkpStructureList(
    override val byteSize: Int,
    val capacity: Int,
    val elementType: ZkpStructureType,
) : ZkpStructureType() {
    override fun flatten(): Sequence<ZkpStructureType> = super.flatten() + elementType.flatten()
    override fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType {
        return transform(
            copy(
                elementType = elementType.rewrite(transform)
            )
        )
    }

    override fun describe(): String = "list($capacity) of ${elementType.describe()}"
}

@Serializable
@SerialName("ARRAY")
data class ZkpStructureArray(
    override val byteSize: Int,
    val capacity: Int,
    val elementType: ZkpStructureType,
) : ZkpStructureType() {
    override fun flatten(): Sequence<ZkpStructureType> = super.flatten() + elementType.flatten()
    override fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType {
        return transform(
            copy(
                elementType = elementType.rewrite(transform),
            )
        )
    }

    override fun describe(): String = "array($capacity) of ${elementType.describe()}"
}

@Serializable
@SerialName("MAP")
data class ZkpStructureMap(
    override val byteSize: Int,
    val capacity: Int,
    val keyType: ZkpStructureType,
    val valueType: ZkpStructureType,
) : ZkpStructureType() {
    override fun flatten(): Sequence<ZkpStructureType> = super.flatten() + keyType.flatten() + valueType.flatten()
    override fun rewrite(transform: (ZkpStructureType) -> ZkpStructureType): ZkpStructureType {
        return transform(
            copy(
                keyType = keyType.rewrite(transform),
                valueType = valueType.rewrite(transform),
            )
        )
    }

    override fun describe(): String = "map($capacity) from ${keyType.describe()} to ${valueType.describe()}"
}

@Serializable
@SerialName("STRING")
data class ZkpStructureString(
    override val byteSize: Int,
    val capacity: Int,
    val encoding: String,
) : ZkpStructureType() {
    override fun describe(): String = "$encoding string($capacity)"
}

@Serializable
@SerialName("ENUM")
data class ZkpStructureEnum(
    val serialName: String,
) : ZkpStructureType() {
    override val byteSize: Int = Int.SIZE_BYTES
    override fun describe(): String = serialName
}

@Serializable
@SerialName("BIG_DECIMAL")
data class ZkpStructureBigDecimal(
    override val byteSize: Int,
    val serialName: String,
    val integerSize: Int,
    val fractionSize: Int,
) : ZkpStructureType() {
    override fun describe(): String = serialName
}
