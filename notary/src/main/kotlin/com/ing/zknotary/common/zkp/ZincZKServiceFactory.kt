package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import java.io.File
import java.time.Duration

class ZincZKServiceFactory() {
    companion object {
        fun create(
            circuitSrcPath: String,
            artifactFolder: String,
            buildTimeout: Duration,
            setupTimeout: Duration,
            provingTimeout: Duration,
            verifyingTimeout: Duration
        ): ZincZKService {
            val circuitSrc = File(circuitSrcPath)
            require(circuitSrc.exists()) { "Cannot find circuit at $circuitSrcPath" }

            val publicDataPath = "$artifactFolder/public-data.json"
            val witnessPath = "$artifactFolder/witness.json"
            val compiledCircuitPath = "$artifactFolder/compiled-${circuitSrc.nameWithoutExtension}.znb"
            val build = ZincZKService.completeZincCommand(
                "${ZincZKService.Compile} $circuitSrcPath --output $compiledCircuitPath " +
                    "--public-data $publicDataPath --witness $witnessPath",
                buildTimeout
            )
            if (build is Result.Failure) {
                error(build.value)
            }

            val provingKeyPath = "$artifactFolder/proving_key"
            val verifyingKeyPath = "$artifactFolder/verifying_key.txt"
            val setup = ZincZKService.completeZincCommand(
                "${ZincZKService.Setup} --circuit $compiledCircuitPath " +
                    "--proving-key $provingKeyPath --verifying-key $verifyingKeyPath",
                setupTimeout
            )
            if (setup is Result.Failure) {
                error(setup.value)
            }

            return ZincZKService(
                compiledCircuitPath,
                ZincZKService.ZKSetup(publicDataPath, provingKeyPath, verifyingKeyPath),
                provingTimeout,
                verifyingTimeout
            )
        }
    }
}
