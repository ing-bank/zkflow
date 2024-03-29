package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincComment.Companion.zincComment
import org.intellij.lang.annotations.Language

interface ZincFunction : ZincInvocable, ZincFileItem {
    @ZincDslMarker
    class Builder {
        var name: String? = null
        private val parameters: MutableList<ZincParameter> = mutableListOf()
        var returnType: ZincType? = null

        @Language("Rust")
        var body: String? = null
        var comment: String? = null

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

        fun build(): ZincFunction {
            return ImmutableZincFunction(
                requireNotNull(name) { "Required value `name` is null." },
                parameters.toList(),
                requireNotNull(returnType) { "Required value `returnType` is null." },
                requireNotNull(body) { "Required value `body` is null." },
                comment?.let { zincComment(it) }
            )
        }
    }

    companion object {
        private data class ImmutableZincFunction(
            private val name: String,
            private val parameters: List<ZincParameter>,
            private val returnType: ZincType,
            private val body: String,
            private val comment: ZincComment?,
        ) : ZincFunction {
            override fun getName(): String = name
            override fun getParameters(): List<ZincParameter> = parameters
            override fun getReturnType(): ZincType = returnType
            override fun getBody(): String = body
            override fun getComment(): ZincComment? = comment
            override fun generate(): String {
                val parameterString = getParameters().joinToString(",\n") {
                    "${if (it.isMutable()) "mut " else ""}${it.getName()}: ${it.getType().getId()}"
                }
                val definitionString = if (getParameters().isNotEmpty()) """
                    fn ${getName()}(
                        ${parameterString.indent(24.spaces)},
                    )
                """.trimIndent() else """
                    fn ${getName()}()
                """.trimIndent()
                val commentString = getComment()?.let { it.generate() + "\n" } ?: ""
                return commentString + """
                    ${definitionString.indent(20.spaces)} -> ${getReturnType().getId()} {
                        ${getBody().indent(24.spaces)}
                    }
                """.trimIndent()
            }
        }

        fun zincFunction(init: Builder.() -> Unit): ZincFunction = Builder().apply(init).build()
    }
}
