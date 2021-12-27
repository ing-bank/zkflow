package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import com.ing.zkflow.SerdeIndex
import org.jetbrains.kotlin.name.Name

private const val PROCESSING_UNIT = "User Classes Indexer"

/**
 * Indexes every user class for further type resolution.
 */
val Meta.UserClassesIndexer: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            classDeclaration(this, match = {
                val className = element.name

                if (className != null) {
                    val fqName = element.containingKtFile.packageFqName.child(Name.identifier(className))
                    SerdeIndex.register(fqName, element.annotationEntries)
                }

                false
            }) { (ktClass, _) ->
                // This function will never be called.
                Transform.replace(
                    replacing = ktClass,
                    newDeclaration = identity()
                )
            }
        )
    }
