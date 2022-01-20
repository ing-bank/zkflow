package com.ing.zkflow.annotated.pilot.ivno.deps

import java.math.BigDecimal

/**
 * package io.dasl.contracts.v1.token
 */
data class BigDecimalAmount<T : Any> (val quantity: BigDecimal, val amountType: T)