package com.ing.zkflow

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import com.ing.zkflow.plugins.serialization.ClassAnnotator
import com.ing.zkflow.plugins.serialization.PropertyAnnotator
import com.ing.zkflow.util.FileLogger

class ArrowProcessor : Meta {
    override fun intercept(ctx: CompilerContext): List<CliPlugin> {
        return listOf(
            // IMPORTANT
            // If com.ing.plugin.getPropertyAnnotator is called first, some annotations from constructor parameters may be LOST.
            // Such behavior has been observed for annotated/ClassString.
            //
            ClassAnnotator,
            PropertyAnnotator
        )
    }
}

internal val SerdeLogger = FileLogger.to("/tmp/serde-annotations.log")
