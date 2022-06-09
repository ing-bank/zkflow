package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import com.ing.zkflow.SerdeIndex
import com.ing.zkflow.SerdeLogger

private const val PROCESSING_UNIT = "USER CLASS INDEXER"

/**
 * Indexes every user class for further type resolution.
 */
val Meta.UserClassesIndexer: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            classDeclaration(
                this, match = {
                    SerdeLogger.phase(PROCESSING_UNIT) {
                        if (element.fqName != null) {
                            SerdeIndex.register(element.fqName!!, element.annotationEntries)
                        } else {
                            it.log("Class without an fqName:\n${element.text}")
                        }

                        false
                    }
                }
            ) { (ktClass, _) ->
                SerdeLogger.phase(PROCESSING_UNIT) {
                    // This function will never be called.
                    Transform.replace(
                        replacing = ktClass,
                        newDeclaration = identity()
                    )
                }
            }
        )
    }
