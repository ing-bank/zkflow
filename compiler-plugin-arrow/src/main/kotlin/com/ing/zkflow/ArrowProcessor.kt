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
            // PropertyAnnotator must be called first to add @kotlinx.serialization.Transient annotations/
            //
            PropertyAnnotator,
            ClassAnnotator,
        )
    }
}

internal val SerdeLogger = FileLogger("/tmp/serde-annotations.log")
