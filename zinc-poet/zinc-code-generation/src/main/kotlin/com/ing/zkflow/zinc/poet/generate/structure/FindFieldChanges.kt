package com.ing.zkflow.zinc.poet.generate.structure

/**
 * Compares two lists of fields, and returns the smallest number of [FieldChange]s between the lists.
 * This recursive algorithm has a complexity of O(3"n).
 */
fun findFieldChanges(
    savedFields: List<ZkpStructureField>,
    newFields: List<ZkpStructureField>
): List<FieldChange> =
    if (savedFields.isEmpty() && newFields.isEmpty()) {
        emptyList()
    } else if (newFields.isEmpty()) {
        savedFields.map { RemovedField(it) }
    } else if (savedFields.isEmpty()) {
        newFields.map { AddedField(it) }
    } else {
        if (savedFields[0] == newFields[0]) {
            findFieldChanges(savedFields.subList(1, savedFields.size), newFields.subList(1, newFields.size))
        } else {
            val removedField = listOf(RemovedField(savedFields[0])) +
                findFieldChanges(savedFields.subList(1, savedFields.size), newFields)
            val addedField = listOf(AddedField(newFields[0])) +
                findFieldChanges(savedFields, newFields.subList(1, newFields.size))
            val modifiedField = listOf(ModifiedField(savedFields[0], newFields[0])) +
                findFieldChanges(savedFields.subList(1, savedFields.size), newFields.subList(1, newFields.size))
            listOf(removedField, addedField, modifiedField).minByOrNull { it.size }!!
        }
    }

sealed class FieldChange {
    abstract fun describe(): String
}

data class AddedField(val field: ZkpStructureField) : FieldChange() {
    override fun describe(): String = "'${field.fieldName}': new field with type '${field.fieldType.describe()}'"
}

data class RemovedField(val field: ZkpStructureField) : FieldChange() {
    override fun describe(): String = "'${field.fieldName}': deleted"
}

data class ModifiedField(val original: ZkpStructureField, val modified: ZkpStructureField) : FieldChange() {
    override fun describe(): String {
        val originalTypeDescription = original.fieldType.describe()
        val modifiedTypeDescription = modified.fieldType.describe()
        val changeString = if (originalTypeDescription == modifiedTypeDescription) {
            if (original.fieldType.byteSize == modified.fieldType.byteSize) {
                null
            } else {
                "size of '$originalTypeDescription' changed from ${original.fieldType.byteSize} to ${modified.fieldType.byteSize} bytes"
            }
        } else {
            "type changed from '$originalTypeDescription' to '$modifiedTypeDescription'"
        }
        return if (original.fieldName == modified.fieldName) {
            "'${original.fieldName}'" + (changeString?.let { ": $it" } ?: "")
        } else {
            "'${original.fieldName}' renamed to '${modified.fieldName}'" + (changeString?.let { ", and $it" } ?: "")
        }
    }
}
