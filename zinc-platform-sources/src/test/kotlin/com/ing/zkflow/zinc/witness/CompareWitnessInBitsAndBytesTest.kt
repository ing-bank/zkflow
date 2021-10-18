package com.ing.zkflow.zinc.witness

import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled("This is a benchmark. Should only be enabled for benchmarks")
class CompareWitnessInBitsAndBytesTest {
    private val log = loggerFor<CompareWitnessInBitsAndBytesTest>()

    private val circuitFolderBits2Bytes: String = javaClass.getResource("/witness/TestBits2Bytes").path
    private val circuitFolderBytes2Bits: String = javaClass.getResource("/witness/TestBytes2Bits").path

    @Test
    fun `zinc compares the performance of witness-in-bits and witness-in-bytes`() {
        val bits2BytesFile = File("$circuitFolderBits2Bytes/timings.txt")
        val bytes2BitsFile = File("$circuitFolderBytes2Bits/timings.txt")

        if (!bits2BytesFile.exists()) {
            println("No timing info found for Bits2Bytes circuit")
        } else if (!bytes2BitsFile.exists()) {
            println("No timing info found for Bytes2Bits circuit")
        } else {
            val timingBits2Bytes = bits2BytesFile.readText()
            val timingBytes2Bits = bytes2BitsFile.readText()

            val setupRegex = "(Setup)\\s\\W\\s([0-9]*[.]?[0-9]+)".toRegex()
            val proveRegex = "(Prove)\\s\\W\\s([0-9]*[.]?[0-9]+)".toRegex()

            val setupBits2Bytes = setupRegex.find(timingBits2Bytes)?.groupValues?.get(2)?.toFloat()
            val setupBytes2Bits = setupRegex.find(timingBytes2Bits)?.groupValues?.get(2)?.toFloat()

            val proveBits2Bytes = proveRegex.find(timingBits2Bytes)?.groupValues?.get(2)?.toFloat()
            val proveBytes2Bits = proveRegex.find(timingBytes2Bits)?.groupValues?.get(2)?.toFloat()

            val reductionInProve = (100.0 * (proveBytes2Bits?.minus(proveBits2Bytes!!)!!)) / proveBytes2Bits
            val reductionInSetup = (100.0 * (setupBytes2Bits?.minus(setupBits2Bytes!!)!!)) / setupBytes2Bits

            println("\nReduction in setup time --> $reductionInSetup%")
            println("Reduction in proving time --> $reductionInProve%")
        }
    }
}
