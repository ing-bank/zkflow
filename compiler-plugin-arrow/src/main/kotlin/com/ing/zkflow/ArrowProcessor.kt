package com.ing.zkflow

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import com.ing.zkflow.plugins.serialization.ClassAnnotator
import com.ing.zkflow.plugins.serialization.PropertyAnnotator
import com.ing.zkflow.plugins.serialization.UserClassesIndexer
import com.ing.zkflow.util.FileLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class ArrowProcessor : Meta {
    override fun intercept(ctx: CompilerContext): List<CliPlugin> {
        return listOf(
            UserClassesIndexer,
            // IMPORTANT
            // PropertyAnnotator must be called first to add @kotlinx.serialization.Transient annotations/
            //
            PropertyAnnotator,
            ClassAnnotator,
        )
    }
}

internal val SerdeLogger = FileLogger("/tmp/serde-annotations.log")

internal object SerdeIndex {
    data class UserClass(val fqName: FqName, val annotations: List<KtAnnotationEntry>)

    private val index = mutableMapOf<FqName, List<KtAnnotationEntry>>()

    fun register(fqName: FqName, annotations: List<KtAnnotationEntry>) {
        require(index.putIfAbsent(fqName, annotations) == null) {
            "Class $fqName has been already registered"
        }
    }

    operator fun get(simpleName: String): UserClass? = index.entries
        .find { it.key.shortName().toString() == simpleName }
        ?.let { UserClass(it.key, it.value) }

    override fun toString(): String =
        index.entries.joinToString(separator = "\n") { entry ->
            val annotations = entry.value.joinToString(separator = ", ", prefix = "[", postfix = "]") { ann -> ann.text }
            "${entry.key}: $annotations"
        }
}
