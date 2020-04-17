package com.ing.zknotary.common.zkp

import net.corda.core.KeepForDJVM
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
@KeepForDJVM
class Proof(val bytes: ByteArray)