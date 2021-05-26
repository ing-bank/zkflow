package io.ivno.collateraltoken.workflow

import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.MAX_PAGE_SIZE
import net.corda.core.node.services.vault.PageSpecification

const val IVNO_DEFAULT_PAGE_NUMBER: Int = 1
const val IVNO_DEFAULT_PAGE_SIZE: Int = 1000

internal val IVNO_DEFAULT_PAGE_SPECIFICATION: PageSpecification
    get() = PageSpecification(IVNO_DEFAULT_PAGE_NUMBER, IVNO_DEFAULT_PAGE_SIZE)

internal val IVNO_MAX_PAGE_SPECIFICATION: PageSpecification
    get() = PageSpecification(DEFAULT_PAGE_NUM, MAX_PAGE_SIZE)
