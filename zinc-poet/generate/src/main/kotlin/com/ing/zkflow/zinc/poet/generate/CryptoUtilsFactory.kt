package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.getLengthConstant
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import java.nio.file.Path

const val CRYPTO_UTILS = "crypto_utils"
const val COMPUTE_NONCE = "compute_nonce"

class CryptoUtilsFactory {
    fun generateCryptoUtils(
        buildPath: Path,
    ) {
        buildPath.zincSourceFile("$CRYPTO_UTILS.zn") {
            for (it in listOf(privacySalt, digest)) {
                mod { module = it.getModuleName() }
                use { path = "${it.getModuleName()}::${it.getSerializedTypeDef().getName()}" }
                use { path = "${it.getModuleName()}::${it.getLengthConstant()}" }
                newLine()
            }
            mod { module = CONSTS }
            use { path = "$CONSTS::$U32_BITS" }
            newLine()
            use { path = "std::convert::to_bits" }
            use { path = "std::crypto::blake2s" }
            newLine()
            function {
                name = COMPUTE_NONCE
                parameter {
                    name = "privacy_salt_bits"
                    type = privacySalt.getSerializedTypeDef()
                }
                parameter {
                    name = "group_index"
                    type = ZincPrimitive.U32
                }
                parameter {
                    name = "internal_index"
                    type = ZincPrimitive.U32
                }
                returnType = digest.getSerializedTypeDef()
                body = """
                    let mut nonce = [false; ${privacySalt.getLengthConstant()} + $U32_BITS + $U32_BITS];
                    for i in 0..${privacySalt.getLengthConstant()} {
                        nonce[i] = privacy_salt_bits[i];
                    }
                
                    let group_index_bits = to_bits(group_index);
                    let internal_index_bits = to_bits(internal_index);
                
                    for i in (0 as u24)..$U32_BITS {
                        nonce[${privacySalt.getLengthConstant()} + i] = group_index_bits[i];
                        nonce[${privacySalt.getLengthConstant()} + $U32_BITS + i] = internal_index_bits[i];
                    }
                
                    blake2s(nonce)
                """.trimIndent()
            }
        }
    }
}
