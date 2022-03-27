package com.ing.zkflow.network

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKFLowSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class ZKNetworkParametersProviderProcessorTest : ProcessorTest(ZKFLowSymbolProcessorProvider()) {
    @Test
    fun `ZKNetworkParametersProviderProcessor should correctly register stuff`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithProvider, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<ZKNetworkParameters>() shouldBe "com.ing.zkflow.network.TestNetworkParams"
    }

    companion object {
        private val kotlinFileWithProvider = SourceFile.kotlin(
            "TestNetworkParams.kt",
            """
                package com.ing.zkflow.network

                import com.ing.zkflow.common.network.ZKAttachmentConstraintType
                import com.ing.zkflow.common.network.ZKNetworkParameters
                import com.ing.zkflow.common.network.ZKNetworkParametersProvider
                import com.ing.zkflow.common.network.ZKNotaryInfo
                import com.ing.zkflow.common.zkp.ZKFlow
                import net.corda.core.crypto.DigestAlgorithm
                import net.corda.core.crypto.SignatureScheme

                class TestNetworkParams : ZKNetworkParameters {
                        override val version: Int = 1
                        override val participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
                        override val attachmentConstraintType: ZKAttachmentConstraintType = ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT_TYPE
                        override val notaryInfo: ZKNotaryInfo = ZKNotaryInfo(ZKFlow.DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME)
                        override val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
                        override val serializationSchemeId: Int = ZKFlow.DEFAULT_SERIALIZATION_SCHEME_ID
                }
        """
        )
    }
}
