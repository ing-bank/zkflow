package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.serializer.ExactLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer

enum class FixedLengthType(val serialName: String) {
    EXACT_LIST(ExactLengthListSerializer::class.qualifiedName!!),
    LIST(FixedLengthListSerializer::class.qualifiedName!!),
    MAP(FixedLengthMapSerializer::class.qualifiedName!!),
    SET(FixedLengthSetSerializer::class.qualifiedName!!),
    BYTE_ARRAY(FixedLengthByteArraySerializer::class.qualifiedName!!),
    STRING(FixedLengthStringSerializer::class.qualifiedName!!),
    BYTE(Byte::class.qualifiedName!!),
    SHORT(Short::class.qualifiedName!!),
    INT(Int::class.qualifiedName!!),
    LONG(Long::class.qualifiedName!!),
    UBYTE(UByte::class.qualifiedName!!),
    USHORT(UShort::class.qualifiedName!!),
    UINT(UInt::class.qualifiedName!!),
    ULONG(ULong::class.qualifiedName!!),
    BOOLEAN(Boolean::class.qualifiedName!!),
    ;

    companion object {
        @JvmStatic
        fun tryFromSerialName(serialName: String): FixedLengthType? {
            return values().find { it.serialName == serialName }
        }

        @JvmStatic
        fun fromSerialName(serialName: String): FixedLengthType {
            return tryFromSerialName(serialName)
                ?: throw IllegalArgumentException("'$serialName' is not a ${FixedLengthType::class}.")
        }
    }
}
