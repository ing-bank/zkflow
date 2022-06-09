package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.WitnessField
import com.ing.zkflow.common.zkp.ZKService
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor

/**
 * IMPORTANT NOTE: This mock service DOES NOT check smart contract rules, only fixed platform rules guaranteeing the backchain integrity.
 * This mock service should only be used to test backchain integrity, for example in the context of flow tests
 * where the confirmation of the contract rules is already covered by the contract tests.
 */
@Suppress("EXPERIMENTAL_API_USAGE", "DuplicatedCode")
public class MockZKService(private val digestService: DigestService) : ZKService {
    private val log = loggerFor<MockZKService>()

    /**
     * This mock version simply returns the Corda-serialized witness, so that we can use it in `verify()`
     * to do all the verifications
     */
    override fun prove(witness: Witness): ByteArray {
        log.info("Proving")
        return witness.serialize().bytes
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        log.info("Verifying proof")

        // This assumes that the proof (for testing only) is simply a serialized witness.
        val witness = proof.deserialize<Witness>()

        /*
         * Rule 1: The recalculated component leaf hashes should match the ones from the instance vtx.
         */
        verifyLeafHashesForComponentGroup(
            witness.outputsGroup.map { e -> e.serializedData },
            publicInput.outputComponentHashes,
            witness.privacySalt,
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal
        )

        /*
         * Rule 2: witness input and reference contents hashed with their nonce should equal the matching hash from publicInput.
         * This proves that prover did not change the contents of the input states
         */
        verifyUtxoContents(witness.serializedInputUtxos, witness.inputUtxoNonces, publicInput.inputUtxoHashes)
        verifyUtxoContents(witness.serializedReferenceUtxos, witness.referenceUtxoNonces, publicInput.referenceUtxoHashes)

        /*
         * Rule 3: The contract rules should verify
         *
         * IMPORTANT NOTE: This mock service DOES NOT check smart contract rules, only fixed platform rules guaranteeing the backchain integrity.
         * This mock service should only be used to test backchain integrity, for example in the context of flow tests
         * where the confirmation of the contract rules is already covered by the contract tests.
         */
    }

    private fun verifyLeafHashesForComponentGroup(serializedComponents: List<ByteArray>, expectedComponentHashes: List<SecureHash>, privacySalt: PrivacySalt, groupIndex: Int) {
        serializedComponents.forEachIndexed { index, serializedComponent ->
            val calculatedNonceFromWitness =
                serializedComponent.let { digestService.computeNonce(privacySalt, groupIndex, index) } // TODO

            val leafHashFromPublicInput = serializedComponent.let {
                expectedComponentHashes.getOrElse(index) {
                    error("Leaf hash not present in public input for component group $groupIndex component $index")
                }
            }

            val calculatedLeafHashFromWitness =
                calculatedNonceFromWitness.let { digestService.componentHash(it, OpaqueBytes(serializedComponent)) }

            if (leafHashFromPublicInput != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for reference $index does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicInput)."
            )
        }
    }

    private fun verifyUtxoContents(
        serializedUtxos: List<WitnessField>,
        utxoNonces: List<SecureHash>,
        expectedUtxoHashes: List<SecureHash>
    ) {
        serializedUtxos.map { e -> e.serializedData }
            .forEachIndexed { index, serializedReferenceUtxo ->
                val nonceFromWitness = utxoNonces.getOrElse(index) {
                    error("Nonce not present in public input for reference $index")
                }

                val leafHashFromPublicreference = expectedUtxoHashes.getOrElse(index) {
                    error("Leaf hash not present in public input for reference $index")
                }

                val calculatedLeafHashFromWitness =
                    digestService.componentHash(nonceFromWitness, OpaqueBytes(serializedReferenceUtxo))

                if (leafHashFromPublicreference != calculatedLeafHashFromWitness) error(
                    "Calculated leaf hash ($calculatedLeafHashFromWitness} for reference $index does " +
                        "not match the leaf hash from the public input ($leafHashFromPublicreference)."
                )
            }
    }
}
