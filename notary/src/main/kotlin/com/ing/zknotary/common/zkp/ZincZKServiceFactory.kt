package com.ing.zknotary.common.zkp

import java.io.File
import java.time.Duration

class ZincZKServiceFactory() {
    companion object {
        fun create(
            circuitSrcPath: String,
            artifactFolder: String,
            buildTimeout: Duration = Duration.ofSeconds(5),
            setupTimeout: Duration = Duration.ofSeconds(30),
            provingTimeout: Duration = Duration.ofSeconds(30),
            verifyingTimeout: Duration = Duration.ofSeconds(5)
        ): ZincZKService {
            val compiledCircuitPath = "$artifactFolder/compiled-${File(circuitSrcPath).nameWithoutExtension}.znb"
            val provingKeyPath = "$artifactFolder/proving_key"
            val verifyingKeyPath = "$artifactFolder/verifying_key.txt"

            return ZincZKService(
                circuitSrcPath,
                compiledCircuitPath,
                ZincZKService.ZKSetup(provingKeyPath, verifyingKeyPath),
                buildTimeout,
                setupTimeout,
                provingTimeout,
                verifyingTimeout
            )
        }
    }
}
