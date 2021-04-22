package com.ing.zknotary.gradle.util

import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.gradle.extension.AmountTemplateParameters
import com.ing.zknotary.gradle.extension.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.extension.StringTemplateParameters
import com.ing.zknotary.gradle.extension.TemplateParameters
import com.ing.zknotary.gradle.extension.UniqueIdentifierTemplateParameters
import com.ing.zknotary.gradle.extension.camelToSnakeCase
import java.io.File

class TemplateRenderer(private val outputPath: File, private val templateResolver: (String) -> String) {

    fun generateTemplate(templateParameters: TemplateParameters) {
        val templateContents = templateResolver.invoke(templateParameters.templateFile)
        generateTemplate(templateContents, templateParameters)
    }

    fun generateTemplate(templateContents: String, templateParameters: TemplateParameters) {
        val baseTemplate =
            """
            //! GENERATED CODE. DO NOT EDIT
            //! Edit it in zk-notary/src/main/resources/zinc-platform-templates/${templateParameters.templateFile}
            //
            """.trimIndent() + "\n" + templateContents
        when (templateParameters) {
            is StringTemplateParameters -> generateStringTemplate(baseTemplate, templateParameters)
            is BigDecimalTemplateParameters -> generateBigDecimalTemplate(baseTemplate, templateParameters)
            is AmountTemplateParameters -> generateAmountTemplate(baseTemplate, templateParameters)
            UniqueIdentifierTemplateParameters -> generateUniqueIdentifierTemplate(baseTemplate, templateParameters)
        }
    }

    private fun generateStringTemplate(
        templateContents: String,
        templateParameters: StringTemplateParameters
    ) {
        val sizeSuffix = "${templateParameters.stringSize}"
        val stringContent = templateContents
            .replace("\${TYPE_NAME}", "String_$sizeSuffix")
            .replace("\${CONSTANT_PREFIX}", "STRING_$sizeSuffix")
            .replace("\${STRING_SIZE_PLACEHOLDER}", templateParameters.stringSize.toString())
        createOutputFile(outputPath.resolve("string_$sizeSuffix.zn"))
            .writeBytes(stringContent.toByteArray())
    }

    private fun generateBigDecimalTemplate(
        templateContents: String,
        templateParameters: BigDecimalTemplateParameters
    ) {
        val bigDecimalsContent = templateContents
            .replace("\${TYPE_NAME}", templateParameters.typeName)
            .replace("\${CONSTANT_PREFIX}", templateParameters.typeName.camelToSnakeCase().toUpperCase())
            .replace("\${INTEGER_SIZE_PLACEHOLDER}", templateParameters.integerSize.toString())
            .replace("\${FRACTION_SIZE_PLACEHOLDER}", templateParameters.fractionSize.toString())
        createOutputFile(outputPath.resolve("${templateParameters.typeName.camelToSnakeCase()}.zn"))
            .writeBytes(bigDecimalsContent.toByteArray())
    }

    private fun generateAmountTemplate(
        templateContents: String,
        templateParameters: AmountTemplateParameters
    ) {
        val bigDecimalsContent = templateContents
            .replace("\${TYPE_NAME}", templateParameters.typeName)
            .replace("\${CONSTANT_PREFIX}", templateParameters.typeName.camelToSnakeCase().toUpperCase())
            .replace("\${TOKEN_SIZE_PLACEHOLDER}", templateParameters.tokenSize.toString())
            .replace("\${BD_TYPE_NAME}", templateParameters.tokenDisplaySize.typeName)
            .replace(
                "\${BD_CONSTANT_PREFIX}",
                templateParameters.tokenDisplaySize.typeName.camelToSnakeCase().toUpperCase()
            )
            .replace("\${BD_MODULE_NAME}", templateParameters.tokenDisplaySize.typeName.camelToSnakeCase())
        createOutputFile(outputPath.resolve("${templateParameters.typeName.camelToSnakeCase()}.zn"))
            .writeBytes(bigDecimalsContent.toByteArray())
    }

    private fun generateUniqueIdentifierTemplate(templateContents: String, templateParameters: TemplateParameters) {
        val uniqueIdentifierContent = templateContents
            .replace("\${EXTERNAL_ID_STRING_SIZE}", UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toString())
        createOutputFile(outputPath.resolve(templateParameters.templateFile))
            .writeBytes(uniqueIdentifierContent.toByteArray())
    }

    fun generateMerkleUtilsCode(templateContents: String, constsContent: String) {
        val targetFile = createOutputFile(outputPath.resolve("merkle_utils.zn"))
        targetFile.writeText("//! Limited-depth recursion for Merkle tree construction\n")
        targetFile.appendText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText(
            """
mod platform_component_group_leaf_digest_dto;
mod platform_crypto_utils;
mod platform_node_digest_dto;

use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestBits;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BITS;
use platform_crypto_utils::pedersen_to_padded_bits;
use platform_crypto_utils::concatenate_component_group_leaf_digests;
use platform_crypto_utils::concatenate_node_digests;
use platform_node_digest_dto::NodeDigestBits;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BITS;
use std::crypto::pedersen;

        """
        )

        val fullMerkleLeaves = getFullMerkleTreeSize(constsContent)
        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "node_digests",
                digestCamelCase = "NodeDigest",
                digestBits = "NODE_DIGEST_BITS"
            )
        )

        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "component_group_leaf_digests",
                digestCamelCase = "ComponentGroupLeafDigest",
                digestBits = "COMPONENT_GROUP_LEAF_DIGEST_BITS"
            )
        )
    }

    fun generateMainCode(templateContents: String, constsContent: String) {
        val targetFile = createOutputFile(outputPath.resolve("main.zn"))
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText("//! The main module.")
        targetFile.appendText(
            """
mod consts;
mod contract_rules;
mod platform_component_group_leaf_digest_dto;
mod platform_merkle_tree;
mod platform_node_digest_dto;
mod platform_utxo_digests;
mod platform_zk_prover_transaction;
 
use consts::INPUT_GROUP_SIZE;
use consts::REFERENCE_GROUP_SIZE; 
use contract_rules::check_contract_rules;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BYTES;
use platform_merkle_tree::build_merkle_tree;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BYTES;
use platform_utxo_digests::compute_input_utxo_digests;
use platform_utxo_digests::compute_reference_utxo_digests;
use platform_zk_prover_transaction::Witness;
"""
        )
        val inputHashesCode = getUtxoDigestCode(constsContent, "input")
        val referenceHashesCode = getUtxoDigestCode(constsContent, "reference")

        val mainContent = templateContents
            .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashesCode)
            .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashesCode)
        targetFile.appendBytes(mainContent.toByteArray())
    }

    private fun createOutputFile(targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        targetFile.delete()
        targetFile.createNewFile()
        return targetFile
    }

    private fun getUtxoDigestCode(constsContent: String, componentGroupName: String): String {
        val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsContent)

        if (componentGroupSize != null) {
            return when {
                componentGroupSize > 0 -> {
                    """compute_${componentGroupName}_utxo_digests( 
            witness.transaction.${componentGroupName}s.components,
            witness.${componentGroupName}_nonces,
        )"""
                }
                componentGroupSize == 0 -> {
                    """[ComponentGroupLeafDigestDto {
            bytes: [0; COMPONENT_GROUP_LEAF_DIGEST_BYTES],
        }; ${componentGroupName.toUpperCase()}_GROUP_SIZE]"""
                }
                else -> {
                    throw IllegalArgumentException("Negative values are not allowed for ${componentGroupName.toUpperCase()}_GROUP_SIZE in consts.zn")
                }
            }
        } else {
            throw IllegalArgumentException("Unknown value for ${componentGroupName.toUpperCase()}_GROUP_SIZE in consts.zn")
        }
    }
}
