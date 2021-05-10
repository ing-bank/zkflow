package io.ivno.collateraltoken.integration

class MockNetwork : IntegrationTest() {
    companion object {
        @JvmStatic
        fun main(vararg args: String) = MockNetwork().start {
            println("Ivno mock network started.")
        }
    }
}
