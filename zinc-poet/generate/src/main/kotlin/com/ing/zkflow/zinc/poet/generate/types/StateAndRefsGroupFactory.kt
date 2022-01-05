package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.indent

class StateAndRefsGroupFactory(
    private val standardTypes: StandardTypes
) {
    fun createStructWithStateAndRefs(
        id: String,
        states: Map<BflModule, Int>,
        deserializedStateGroup: BflModule,
    ) = struct {
        name = id
        states.forEach {
            field {
                name = it.key.id.camelToSnakeCase() // TODO use utility function get contract class name
                type = list {
                    capacity = it.value
                    elementType = standardTypes.stateAndRef(it.key)
                }
            }
        }
        isDeserializable = false
        // Import types used in the [createFromStatesAndRefsFunction] function
        addImport(deserializedStateGroup)
        addImport(StandardTypes.stateRef)
        states.forEach {
            addImport(standardTypes.stateAndRef(it.key))
        }
        addFunction(createFromStatesAndRefsFunction(deserializedStateGroup, states))
    }

    private fun createFromStatesAndRefsFunction(
        deserializedStates: BflModule,
        transactionStates: Map<BflModule, Int>
    ) = zincFunction {
        var offset = 0
        val fieldCombiners = transactionStates.entries.joinToString("\n") { (type, count) ->
            val stateAndRef = standardTypes.stateAndRef(type)
            val listType = list {
                elementType = stateAndRef
                capacity = count
            }
            val fieldName = type.id.camelToSnakeCase()
            val result = """
                let $fieldName = {
                    let mut ${fieldName}_array: [${stateAndRef.id}; $count] = [${stateAndRef.id}::empty(); $count];
                    for i in (0 as u8)..$count {
                        ${fieldName}_array[i] = ${stateAndRef.id}::new(states.$fieldName.values[i], refs[i + $offset]);
                    }
                    ${listType.id}::list_of(${fieldName}_array)
                };
            """.trimIndent()
            offset += count
            result
        }
        val fieldAssignments = transactionStates.keys.joinToString("\n") {
            "${it.id.camelToSnakeCase()}: ${it.id.camelToSnakeCase()},"
        }
        name = "from_states_and_refs"
        parameter {
            name = "states"
            type = deserializedStates.toZincId()
        }
        parameter {
            name = "refs"
            type = zincArray {
                elementType = StandardTypes.stateRef.toZincId()
                size = "${transactionStates.values.sum()}"
            }
        }
        returnType = Self
        body = """
            ${fieldCombiners.indent(12.spaces)}
            
            Self {
                ${fieldAssignments.indent(16.spaces)}
            }
        """.trimIndent()
    }
}
