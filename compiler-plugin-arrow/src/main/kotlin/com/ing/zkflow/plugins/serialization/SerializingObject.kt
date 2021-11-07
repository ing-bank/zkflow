package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.Default
import com.ing.zkflow.Resolver
import com.ing.zkflow.serialization.serializer.BooleanSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.BigDecimalSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import java.math.BigDecimal
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
sealed class SerializingObject {
    abstract val cleanTypeDeclaration: String
    abstract val redeclaration: String
    abstract operator fun invoke(outer: Tracker): SerializationSupport
    abstract fun wrapDefault(): SerializingObject
    abstract fun wrapNull(): SerializingObject

    /**
     * Make a serializing object available under the name `outer`.
     *
     * Recurses to inner function with the same functionality but with a proper coordinate of the artifact.
     */
    operator fun invoke(outer: String): SerializationSupport = invoke(Tracker(outer, listOf(Coordinate.Numeric())))

    class Nullable internal constructor(
        private val child: SerializingObject,
        private val construction: (outer: Tracker, inner: Tracker) -> String
    ) : SerializingObject() {
        override val cleanTypeDeclaration = "${child.cleanTypeDeclaration}?"
        override val redeclaration = "${child.redeclaration}?"

        override fun invoke(outer: Tracker): SerializationSupport {
            val inner = outer.next()
            return SerializationSupport(outer, construction(outer, inner)).subsume(child(inner))
        }

        override fun wrapDefault() = this
        override fun wrapNull() = this
    }

    class WithDefault internal constructor(
        private val child: SerializingObject,
        private val construction: (outer: Tracker, inner: Tracker) -> String
    ) : SerializingObject() {
        override val cleanTypeDeclaration = child.cleanTypeDeclaration
        override val redeclaration = child.redeclaration

        override fun invoke(outer: Tracker): SerializationSupport {
            val inner = outer.next()
            return SerializationSupport(outer, construction(outer, inner)).subsume(child(inner))
        }

        override fun wrapNull() = Nullable(this) { outer, inner ->
            "object $outer: ${NullableSerializer::class.qualifiedName}<$cleanTypeDeclaration>($inner)"
        }

        override fun wrapDefault() = this
    }

    class ExplicitType internal constructor(
        private val original: KtTypeReference,
        serializer: KClass<out KSerializer<*>>,
        private val type: String,
        private val children: List<SerializingObject>,
        private val construction: (self: ExplicitType, outer: Tracker, inner: List<Tracker>) -> String
    ) : SerializingObject() {
        private val hasDefault = serializer.implementsInterface(KSerializerWithDefault::class)

        // TODO replace with those as in FuzzyType?
        override val cleanTypeDeclaration: String by lazy {
            children
                .joinToString(separator = ", ") { it.cleanTypeDeclaration }
                .let { if (it.isNotBlank()) "<$it>" else it }
                .let { inner -> "$type$inner" }
        }

        override val redeclaration: String by lazy {
            val annotationsDeclaration = original.annotationEntries.joinToString(separator = " ") { it.text }

            val inner = children
                .joinToString(separator = ", ") { it.redeclaration }
                .let { if (it.isNotBlank()) "<$it>" else it }

            "$annotationsDeclaration @${Contextual::class.qualifiedName!!} $type$inner"
        }

        override fun invoke(outer: Tracker): SerializationSupport {
            val inners = when (children.size) {
                0 -> return SerializationSupport(outer, construction(this, outer, emptyList()))
                1 -> listOf(outer.next())
                else -> List(children.size) { idx -> outer.literal(idx).numeric() }
            }

            return children.indices.fold(SerializationSupport(outer, construction(this, outer, inners))) { support, idx ->
                val inner = inners[idx]
                val child = children[idx]
                support.subsume(child(inner))
            }
        }

        override fun wrapDefault(): SerializingObject {
            if (hasDefault) return this

            // Inspect annotations to find either @com.ing.annotations.Default or @com.ing.annotations.Resolver
            val defaultProvider = with(original) {
                annotationSingleArgOrNull<Default<*>>()
                    ?: annotationOrNull<Resolver<*, *>>()?.let {
                        it.valueArguments.getOrNull(0)?.asElement()?.text
                    }
                    ?: error("Element $text requires either a ${Default::class} or ${Resolver::class} annotation")
            }.replace("::class", "").trim()

            return WithDefault(this) { outer, inner ->
                "object $outer: ${KSerializerWithDefault::class.qualifiedName}<$cleanTypeDeclaration>($inner, $defaultProvider.default)"
            }
        }

        override fun wrapNull() = with(wrapDefault()) {
            Nullable(this) { outer, inner ->
                "object $outer: ${NullableSerializer::class.qualifiedName}<$cleanTypeDeclaration>($inner)"
            }
        }
    }

    class FuzzyType internal constructor(
        private val original: KtTypeReference,
        private val construction: (self: FuzzyType, outer: Tracker) -> String
    ) : SerializingObject() {
        override val cleanTypeDeclaration: String by lazy { original.cleanTypeDeclaration(ignoreNullability = true) }
        override val redeclaration: String by lazy { original.attachAnnotation(Contextual::class) }

        override fun invoke(outer: Tracker) = SerializationSupport(outer, construction(this, outer))

        // TODO Abstract away this code for Explicit and Fuzzy types
        override fun wrapDefault(): SerializingObject {
            // Inspect annotations to find either @com.ing.annotations.Default or @com.ing.annotations.Resolver
            val defaultProvider = with(original) {
                annotationSingleArgOrNull<Default<*>>()
                    ?: annotationOrNull<Resolver<*, *>>()?.let {
                        it.valueArguments.getOrNull(0)?.asElement()?.text
                    }
                    ?: error("Element $text requires either a ${Default::class} or ${Resolver::class} annotation")
            }.replace("::class", "").trim()

            return WithDefault(this) { outer, inner ->
                "object $outer: ${KSerializerWithDefault::class.qualifiedName}<$cleanTypeDeclaration>($inner, $defaultProvider.default)"
            }
        }

        override fun wrapNull() = with(wrapDefault()) {
            Nullable(this) { outer, inner ->
                "object $outer: ${NullableSerializer::class.qualifiedName}<$cleanTypeDeclaration>($inner)"
            }
        }
    }

    @Suppress("FunctionName")
    companion object {
        // Primitive types.
        fun BOOLEAN(original: KtTypeReference) = ExplicitType(
            original, WrappedKSerializerWithDefault::class, Boolean::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<Boolean>(${BooleanSerializer::class.qualifiedName})"
        }

        fun INT(original: KtTypeReference) = ExplicitType(
            original, WrappedKSerializerWithDefault::class, Int::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<Int>(${IntSerializer::class.qualifiedName})"
        }

        fun LONG(original: KtTypeReference) = ExplicitType(
            original, WrappedKSerializerWithDefault::class, Long::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<Long>(${LongSerializer::class.qualifiedName})"
        }

        fun ASCII_CHAR(original: KtTypeReference) = ExplicitType(
            original, WrappedKSerializerWithDefault::class, Char::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<Char>(${ASCIICharSerializer::class.qualifiedName})"
        }

        fun UTF8_CHAR(original: KtTypeReference) = ExplicitType(
            original, WrappedKSerializerWithDefault::class, Char::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<Char>(${ASCIICharSerializer::class.qualifiedName})"
        }

        fun ASCII_STRING(original: KtTypeReference, maxLength: Int) = ExplicitType(
            original, FixedLengthASCIIStringSerializer::class, String::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${FixedLengthASCIIStringSerializer::class.qualifiedName}($maxLength)"
        }

        fun UTF8_STRING(original: KtTypeReference, maxLength: Int) = ExplicitType(
            original, FixedLengthUTF8StringSerializer::class, String::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${FixedLengthUTF8StringSerializer::class.qualifiedName}($maxLength)"
        }

        // Generic collections.
        fun LIST(original: KtTypeReference, maxSize: Int, item: SerializingObject) = ExplicitType(
            original, FixedLengthListSerializer::class, List::class.simpleName!!, listOf(item)
        ) { _, outer, inner ->
            val single = inner.singleOrNull() ?: error(" List must have a single parametrizing object")
            "object $outer: ${FixedLengthListSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
        }

        fun SET(original: KtTypeReference, maxSize: Int, item: SerializingObject) = ExplicitType(
            original, FixedLengthSetSerializer::class, Set::class.simpleName!!, listOf(item)
        ) { _, outer, inner ->
            val single = inner.singleOrNull() ?: error(" Set must have a single parametrizing object")
            "object $outer: ${FixedLengthSetSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
        }

        fun MAP(original: KtTypeReference, maxSize: Int, key: SerializingObject, value: SerializingObject) = ExplicitType(
            original, FixedLengthMapSerializer::class, Map::class.simpleName!!, listOf(key, value)
        ) { _, outer, inner ->
            val keyRef = inner.getOrNull(0)
                ?: error("To describe ${Map::class.qualifiedName}, names for key and value are required")
            val valueRef = inner.getOrNull(1)
                ?: error("To describe ${Map::class.qualifiedName}, names for key and value are required")

            """
                object $outer: ${FixedLengthMapSerializer::class.qualifiedName}<${key.cleanTypeDeclaration}, ${value.cleanTypeDeclaration}>(
                    $maxSize, $keyRef, $valueRef
                )
            """.trimIndent()
        }

        // Specialized collections, i.e., collections of primitive types.
        fun BYTE_ARRAY(original: KtTypeReference, maxSize: Int) = ExplicitType(
            original, FixedLengthByteArraySerializer::class, ByteArray::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${FixedLengthByteArraySerializer::class.qualifiedName}($maxSize)"
        }

        // Floating point types.
        fun BIG_DECIMAL(original: KtTypeReference, integerPart: Int, fractionPart: Int) = ExplicitType(
            original, BigDecimalSerializer::class, BigDecimal::class.simpleName!!, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${BigDecimalSerializer::class.qualifiedName}($integerPart, $fractionPart)"
        }

        // Surrogate serializer.
        fun SURROGATE(original: KtTypeReference, surrogateType: KtTypeElement, conversionProvider: String) = FuzzyType(original) { self, outer ->
            """
                object $outer: ${SurrogateSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}, ${surrogateType.text.trim()}>(
                    ${surrogateType.extractRootType().type}.serializer(),
                    { $conversionProvider.from(it) }
                )
            """.trimIndent()
        }

        // Own class serializer.
        fun OWN_CLASS(original: KtTypeReference) = FuzzyType(original) { self, outer ->
            val type = original.typeElement?.extractRootType()?.type ?: error("Cannot infer type of `$original`")
            "object $outer: ${WrappedKSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}>($type.serializer())"
        }
    }
}

data class SerializationSupport(private val entry: Tracker, val objects: List<String>) {
    constructor(entry: Tracker, value: String) : this(entry, listOf(value))

    val serializer = "$entry::class"

    fun subsume(subsumed: SerializationSupport) = SerializationSupport(entry, objects + subsumed.objects)
}
