// Required for [AutomaticPlaceholderConstraint]
@file:Suppress("DEPRECATION")
package com.ing.zkflow.processors.serialization.hierarchy

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.ksp.getCordaSignatureId
import com.ing.zkflow.ksp.getDigestAlgorithm
import com.ing.zkflow.processors.serialization.hierarchy.types.asBasic
import com.ing.zkflow.processors.serialization.hierarchy.types.asBigDecimal
import com.ing.zkflow.processors.serialization.hierarchy.types.asByteArray
import com.ing.zkflow.processors.serialization.hierarchy.types.asChar
import com.ing.zkflow.processors.serialization.hierarchy.types.asList
import com.ing.zkflow.processors.serialization.hierarchy.types.asMap
import com.ing.zkflow.processors.serialization.hierarchy.types.asNullable
import com.ing.zkflow.processors.serialization.hierarchy.types.asParty
import com.ing.zkflow.processors.serialization.hierarchy.types.asSet
import com.ing.zkflow.processors.serialization.hierarchy.types.asString
import com.ing.zkflow.processors.serialization.hierarchy.types.asUserType
import com.ing.zkflow.processors.serialization.hierarchy.types.asWithCordaSignatureId
import com.ing.zkflow.processors.serialization.hierarchy.types.asWithDigestAlgorithm
import com.ing.zkflow.serialization.serializer.BooleanSerializer
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.InstantSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.ShortSerializer
import com.ing.zkflow.serialization.serializer.UUIDSerializer
import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticHashConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticPlaceholderConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.WhitelistedByZoneAttachmentConstraintSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant
import java.util.UUID

/**
 * This function is expected to be called on constructor parameters.
 *
 * Any given parameter of certain type requires for its serialization a hierarchical sequence of
 * serializers. Such serializers are represented by Kotlin-poet's primitive [TypeSpec].
 * E.g., for a parameter
 *      myList: @Size(5) List<Int>
 * a sequence of [TypeSpec]'s
 *      object MyList_0: FixedLengthListSerializer(5, My_List_1)
 *      object MyList_1: WrappedFixedLengthSerializer<Int>(IntSerializer)
 * will be generated.
 */
internal fun KSValueParameter.getSerializingHierarchy(): SerializingHierarchy {
    val name = name?.asString() ?: error("Cannot get a name of $this")
    return type.getSerializingHierarchy(Tracker(name.capitalize()))
}

/**
 * [SerializingHierarchy] holds information allowing to build up a collection
 * of serializers for a given type.
 */
internal sealed class SerializingHierarchy(
    val definition: TypeSpec
) {
    class OfType(
        private val rootType: ClassName,
        val inner: List<SerializingHierarchy>,
        serializingObject: TypeSpec,
        private val isParameterizing: Boolean = true
    ) : SerializingHierarchy(serializingObject) {
        override val type: TypeName
            get() {
                val parametrization = inner.mapNotNull {
                    if (it is OfType && !it.isParameterizing) { return@mapNotNull null }
                    it.type
                }

                return if (parametrization.isNotEmpty()) {
                    rootType
                        .parameterizedBy(parametrization)
                        .copy(annotations = emptyList())
                } else {
                    rootType.copy(annotations = emptyList())
                }
            }
    }

    class OfNullable(
        val inner: SerializingHierarchy,
        serializingObject: TypeSpec
    ) : SerializingHierarchy(serializingObject) {
        override val type: TypeName
            get() = inner.type.copy(nullable = true)
    }

    class OfDefaultable(
        val inner: SerializingHierarchy,
        serializingObject: TypeSpec
    ) : SerializingHierarchy(serializingObject) {
        override val type: TypeName
            get() = inner.type
    }

    abstract val type: TypeName

    fun addTypesTo(container: TypeSpec.Builder) {
        container.addType(definition)
        when (this) {
            is OfType -> inner.forEach { it.addTypesTo(container) }
            is OfNullable -> inner.addTypesTo(container)
            is OfDefaultable -> inner.addTypesTo(container)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
internal fun KSTypeReference.getSerializingHierarchy(tracker: Tracker, ignoreNullability: Boolean = false, mustHaveDefault: Boolean = false): SerializingHierarchy {
    val type = resolve()

    if (type.isMarkedNullable && !ignoreNullability) {
        return asNullable(tracker)
    }

    // Invariant:
    // Nullability has been stripped by now.

    val fqName = type.declaration.qualifiedName?.asString() ?: error("Cannot determine a fully qualified name of ${type.declaration}")

    // TODO
    //   matching for basic types as in  KSTypeReference.isZKFlowSupportedPrimitive()

    return if (type.declaration.isAnnotationPresent(ZKP::class) || isAnnotationPresent(Via::class)) {
        asUserType(tracker, mustHaveDefault)
    } else {
        when (fqName) {
            // Types.
            Boolean::class.qualifiedName -> asBasic(tracker, BooleanSerializer::class)
            Byte::class.qualifiedName -> asBasic(tracker, ByteSerializer::class)
            Short::class.qualifiedName -> asBasic(tracker, ShortSerializer::class)
            Int::class.qualifiedName -> asBasic(tracker, IntSerializer::class)
            Long::class.qualifiedName -> asBasic(tracker, LongSerializer::class)
            Float::class.qualifiedName -> asBasic(tracker, FixedLengthFloatingPointSerializer.FloatSerializer::class)
            Double::class.qualifiedName -> asBasic(tracker, FixedLengthFloatingPointSerializer.DoubleSerializer::class)
            Char::class.qualifiedName -> asChar(tracker)
            String::class.qualifiedName -> asString(tracker)
            BigDecimal::class.qualifiedName -> asBigDecimal(tracker)
            Instant::class.qualifiedName -> asBasic(tracker, InstantSerializer::class)
            UUID::class.qualifiedName -> asBasic(tracker, UUIDSerializer::class)
            SecureHash::class.qualifiedName -> {
                val digestAlgorithm = getDigestAlgorithm().toClassName()
                asWithDigestAlgorithm(tracker, SecureHashSerializer::class, digestAlgorithm)
            }
            PublicKey::class.qualifiedName -> {
                val cordaSignatureId = getCordaSignatureId()
                asWithCordaSignatureId(tracker, PublicKeySerializer::class, cordaSignatureId)
            }
            AnonymousParty::class.qualifiedName -> {
                val cordaSignatureId = getCordaSignatureId()
                asWithCordaSignatureId(tracker, AnonymousPartySerializer::class, cordaSignatureId)
            }
            Party::class.qualifiedName -> asParty(tracker)
            AlwaysAcceptAttachmentConstraint::class.qualifiedName -> asBasic(
                tracker,
                AlwaysAcceptAttachmentConstraintSerializer::class
            )
            WhitelistedByZoneAttachmentConstraint::class.qualifiedName -> asBasic(
                tracker,
                WhitelistedByZoneAttachmentConstraintSerializer::class
            )
            AutomaticHashConstraint::class.qualifiedName -> asBasic(tracker, AutomaticHashConstraintSerializer::class)
            AutomaticPlaceholderConstraint::class.qualifiedName -> asBasic(
                tracker,
                AutomaticPlaceholderConstraintSerializer::class
            )
            HashAttachmentConstraint::class.qualifiedName -> {
                val digestAlgorithm = getDigestAlgorithm().toClassName()
                asWithDigestAlgorithm(tracker, HashAttachmentConstraintSerializer::class, digestAlgorithm)
            }
            SignatureAttachmentConstraint::class.qualifiedName -> {
                val cordaSignatureId = getCordaSignatureId()
                asWithCordaSignatureId(tracker, SignatureAttachmentConstraintSerializer::class, cordaSignatureId)
            }
            // Specialized collection.
            ByteArray::class.qualifiedName -> asByteArray(tracker)
            //
            // Generic collections.
            List::class.qualifiedName -> asList(tracker)
            Map::class.qualifiedName -> asMap(tracker)
            Set::class.qualifiedName -> asSet(tracker)
            //
            else -> TODO("Type ${type.declaration} must be appropriately annotated")
        }
    }
}
