package com.ing.zkflow

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import com.ing.zkflow.plugins.serialization.ClassAnnotator
import com.ing.zkflow.plugins.serialization.UserClassesIndexer
import com.ing.zkflow.util.FileLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class ArrowProcessor : Meta {
    override fun intercept(ctx: CompilerContext): List<CliPlugin> {
        return listOf(
            UserClassesIndexer,
            ClassAnnotator,
        )
    }
}

internal val SerdeLogger: FileLogger by lazy {
    FileLogger(ArrowProcessor::class, withTimeStamp = true)
}

internal object SerdeIndex {
    data class UserClass(val fqName: FqName, val annotations: List<KtAnnotationEntry>)

    private val index = mutableMapOf<FqName, List<KtAnnotationEntry>>()

    fun register(fqName: FqName, annotations: List<KtAnnotationEntry>) {
        SerdeLogger.log("$fqName")

        require(index.putIfAbsent(fqName, annotations) == null) {
            "Class $fqName has been already registered"
        }
    }

    fun get(simpleName: String, packageName: String): UserClass? {
        val candidates = index.entries
            .filter { it.key.shortName().toString() == simpleName }

        return when (candidates.size) {
            0 -> null
            1 -> with(candidates.single()) {
                UserClass(key, value)
            }
            else -> {
                val single = candidates.singleOrNull {
                    it.key.toString().startsWith(packageName)
                }

                if (single == null) {
                    val err = candidates.joinToString(
                        prefix = "Cannot disambiguate between:\n",
                        separator = "/n"
                    ) { "${it.key}" }
                    SerdeLogger.log(err)
                    error(err)
                } else {
                    UserClass(single.key, single.value)
                }
            }
        }
    }

    override fun toString(): String =
        index.entries.joinToString(separator = "\n") { entry ->
            val annotations =
                entry.value.joinToString(separator = ", ", prefix = "[", postfix = "]") { ann -> ann.text }
            "${entry.key}: $annotations"
        }
}
