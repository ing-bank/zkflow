package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincComment.Companion.zincComment

interface ZincMethod : ZincInvokeable {
    /**
     * This flag signals whether this method may modify its struct.
     */
    fun isMutable(): Boolean

    @ZincDslMarker
    class Builder {
        var name: String? = null
        private val parameters: MutableList<ZincParameter> = mutableListOf()
        var returnType: ZincType? = null
        var body: String? = null
        var comment: String? = null
        var mutable: Boolean = false

        fun addParameter(parameter: ZincParameter): Builder {
            this.parameters.add(parameter)
            return this
        }

        fun addParameters(parameters: Collection<ZincParameter>): Builder {
            this.parameters.addAll(parameters)
            return this
        }

        fun parameter(init: ZincParameter.Builder.() -> Unit): Builder {
            parameters.add(ZincParameter.zincParameter(init))
            return this
        }

        fun build(): ZincMethod {
            return ImmutableZincMethod(
                requireNotNull(name) { "Required value `name` is null." },
                parameters.toList(),
                requireNotNull(returnType) { "Required value `returnType` is null." },
                requireNotNull(body) { "Required value `body` is null." },
                comment?.let { zincComment(it) },
                mutable
            )
        }
    }

    companion object {
        private data class ImmutableZincMethod(
            private val name: String,
            private val parameters: List<ZincParameter>,
            private val returnType: ZincType,
            private val body: String,
            private val comment: ZincComment?,
            private val mutable: Boolean
        ) : ZincMethod {
            override fun isMutable(): Boolean = mutable
            override fun getName(): String = name
            override fun getParameters(): List<ZincParameter> = parameters
            override fun getReturnType(): ZincType = returnType
            override fun getBody(): String = body
            override fun getComment(): ZincComment? = comment
            override fun generate(): String {
                val parameters = listOf((if (isMutable()) "mut " else "") + "self") +
                    getParameters().map {
                        "${if (it.isMutable()) "mut " else ""}${it.getName()}: ${it.getType().getId()}"
                    }
                val parameterString = parameters.joinToString(",\n") { it }
                val commentString = getComment()?.let { it.generate() + "\n" } ?: ""
                return commentString + """
                    fn ${getName()}(
                        ${parameterString.indent(24.spaces)},
                    ) -> ${getReturnType().getId()} {
                        ${getBody().indent(24.spaces)}
                    }
                """.trimIndent()
            }
        }

        fun zincMethod(init: Builder.() -> Unit): ZincMethod = Builder().apply(init).build()
    }
}
