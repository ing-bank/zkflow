package com.ing.zkflow.annotated.ivno.deps

import net.corda.core.identity.AbstractParty

/**
 * package io.onixlabs.corda.bnms.contract
 */
data class Network(val value: String, val operator: AbstractParty?)
