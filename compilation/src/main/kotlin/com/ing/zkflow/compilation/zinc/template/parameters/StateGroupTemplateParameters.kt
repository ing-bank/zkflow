package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.common.zkp.metadata.ContractStateTypeCount
import com.ing.zkflow.common.zkp.metadata.ZincType
import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.util.camelToSnakeCase
import com.ing.zkflow.util.snakeCaseToCamel
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

data class StateGroupTemplateParameters(
    val componentName: String,
    val contractStateTypeCounts: List<ContractStateTypeCount>,
    val javaClass2ZincType: Map<KClass<out ContractState>, ZincType>
) :
    TemplateParameters(
        if (componentName.contains("output"))
            "platform_components_outputs_template.zn"
        else
            "platform_utxo_digests_template.zn",
        listOf()
    ) {

    override val typeName = componentName.capitalize()

    override fun getTargetFilename() = if (componentName.contains("output"))
        "platform_components_outputs.zn"
    else
        "platform_utxo_${componentName}_digests.zn"

    override fun getReplacements() = getTypeReplacements("COMPONENT_NAME_") +
        mapOf(
            "MODULE_PLACEHOLDER" to getContent(key = "module"),
            "USE_DECLARATION_PLACEHOLDER" to getContent(key = "useDeclaration"),
            "TYPE_PLACEHOLDER" to getContent(key = "serializedType"),
            "HASH_COMPUTATION_PLACEHOLDER" to if (componentName.contains("output")) getContent(key = "leafHash") else getContent(key = "utxoHash")
        )

    private val componentGroupSize = contractStateTypeCounts.sumBy { it.count }
    private val templates: Map<String, String> = mapOf(
        "module" to "mod serialized_${"COMPONENT_NAME_MODULE_NAME"}_tx_state_${"STATE_NAME_MODULE_NAME"};\n",

        "useDeclaration" to """use serialized_${"COMPONENT_NAME_MODULE_NAME"}_tx_state_${"STATE_NAME_MODULE_NAME"}::${"COMPONENT_NAME_CONSTANT_PREFIX"}_${"STATE_NAME_CONSTANT_PREFIX"}_GROUP_SIZE;
use serialized_${"COMPONENT_NAME_MODULE_NAME"}_tx_state_${"STATE_NAME_MODULE_NAME"}::Serialized${"COMPONENT_NAME_TYPE_NAME"}${"STATE_NAME_TYPE_NAME"}${"COMPONENT_TYPE_TYPE_NAME"};
""",

        "serializedType" to """    ${"STATE_NAME_MODULE_NAME"}: Serialized${"COMPONENT_NAME_TYPE_NAME"}${"STATE_NAME_TYPE_NAME"}${"COMPONENT_TYPE_TYPE_NAME"},
 """,
        "leafHash" to """
    let component_leaf_hashes_${"STATE_NAME_MODULE_NAME"} = serialized_${"COMPONENT_NAME_MODULE_NAME"}_tx_state_${"STATE_NAME_MODULE_NAME"}::compute_component_leaf_hashes(
    components.${"STATE_NAME_MODULE_NAME"},
    privacy_salt,
    ComponentGroupEnum::${"COMPONENT_NAME_CONSTANT_PREFIX"}S_GROUP as u32,
    element_index);
    for i in 0 as u32..${"COMPONENT_NAME_CONSTANT_PREFIX"}_${"STATE_NAME_CONSTANT_PREFIX"}_GROUP_SIZE as u32{
        component_leaf_hashes[element_index + i] = component_leaf_hashes_${"STATE_NAME_MODULE_NAME"}[i];
    }
    element_index += ${"COMPONENT_NAME_CONSTANT_PREFIX"}_${"STATE_NAME_CONSTANT_PREFIX"}_GROUP_SIZE as u32;
 """,
        "utxoHash" to """
    let utxo_hashes_${"STATE_NAME_MODULE_NAME"} = serialized_${"COMPONENT_NAME_MODULE_NAME"}_tx_state_${"STATE_NAME_MODULE_NAME"}::compute_utxo_hashes(
    utxos.${"STATE_NAME_MODULE_NAME"},
    nonces,
    element_index);

    for i in 0 as u32..${"COMPONENT_NAME_CONSTANT_PREFIX"}_${"STATE_NAME_CONSTANT_PREFIX"}_GROUP_SIZE as u32{
        utxo_hashes[element_index + i] = component_group_leaf_digest_from_bits_to_bytes(utxo_hashes_${"STATE_NAME_MODULE_NAME"}[i]);
    }
    element_index += ${"COMPONENT_NAME_CONSTANT_PREFIX"}_${"STATE_NAME_CONSTANT_PREFIX"}_GROUP_SIZE as u32;
 """,
    )

    private fun getContent(key: String): String {
        var content = ""

        if (key == "serializedType") {
            val structTemplate = "struct Serialized${"COMPONENT_NAME_TYPE_NAME"}${"COMPONENT_TYPE_TYPE_NAME"}{\n"
            val emptyStructTemplate = "struct Serialized${"COMPONENT_NAME_TYPE_NAME"}${"COMPONENT_TYPE_TYPE_NAME"} { }"

            content += if (componentGroupSize == 0) {
                emptyStructTemplate.replace("COMPONENT_NAME_TYPE_NAME", componentName.snakeCaseToCamel())
                    .replace("COMPONENT_TYPE_TYPE_NAME", if (componentName.contains("output")) "Group" else "Utxos")
            } else {
                structTemplate.replace("COMPONENT_NAME_TYPE_NAME", componentName.snakeCaseToCamel())
                    .replace("COMPONENT_TYPE_TYPE_NAME", if (componentName.contains("output")) "Group" else "Utxos")
            }
        }

        contractStateTypeCounts.forEach {
            if (componentGroupSize > 0) {
                val zincType = javaClass2ZincType[it.type]?.typeName
                    ?: error("No Zinc Type defined for ${it.type}")
                content += templates[key]
                    ?.replace("STATE_NAME_CONSTANT_PREFIX", zincType.camelToSnakeCase().toUpperCase())
                    ?.replace("STATE_NAME_MODULE_NAME", zincType.camelToSnakeCase())
                    ?.replace("STATE_NAME_TYPE_NAME", zincType)
                    ?.replace("COMPONENT_NAME_CONSTANT_PREFIX", componentName.toUpperCase())
                    ?.replace("COMPONENT_NAME_MODULE_NAME", componentName)
                    ?.replace("COMPONENT_NAME_TYPE_NAME", componentName.snakeCaseToCamel())
                    ?.replace("COMPONENT_TYPE_TYPE_NAME", if (componentName.contains("output")) "Group" else "Utxos")
            }
        }

        if (key == "serializedType" && componentGroupSize > 0) {
            content += "}\n"
        }

        return content
    }
}
