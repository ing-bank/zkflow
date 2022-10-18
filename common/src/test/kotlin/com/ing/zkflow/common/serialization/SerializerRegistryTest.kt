package com.ing.zkflow.common.serialization

import com.ing.zkflow.common.versioning.TestStateV1
import com.ing.zkflow.common.versioning.TestStateV2
import com.ing.zkflow.common.versioning.TestStateV3
import com.ing.zkflow.common.versioning.UnknownState
import com.ing.zkflow.serialization.infra.SerializerRegistryError
import com.ing.zkflow.serialization.register
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.ContractState
import org.junit.jupiter.api.Test

internal class SerializerRegistryTest {
    private object TestSerializerRegistry : SerializerRegistry<ContractState>()

    init {
        TestSerializerRegistry.register(TestStateV1::class, TestStateV1.serializer())
        TestSerializerRegistry.register(TestStateV2::class, TestStateV2.serializer())
        TestSerializerRegistry.register(TestStateV3::class, TestStateV3.serializer())
    }

    @Test
    fun `get for class should return the correct serializer`() {
        TestSerializerRegistry[TestStateV1::class] shouldBe TestStateV1.serializer()
    }

    @Test
    fun `get for unknown class should raise ClassNotRegistered`() {
        shouldThrow<SerializerRegistryError.ClassNotRegistered> {
            TestSerializerRegistry[UnknownState::class]
        }.message shouldBe "No registration for class com.ing.zkflow.common.versioning.UnknownState. Please annotate it with @ZKP or annotate a surrogate with @ZKPSurrogate."
    }

    @Test
    fun `get for id should return the correct serializer`() {
        TestSerializerRegistry[TestStateV1::class.hashCode()] shouldBe TestStateV1.serializer()
    }

    @Test
    fun `get for unknown id should raise ClassNotRegistered`() {
        shouldThrow<SerializerRegistryError.ClassNotRegistered> {
            TestSerializerRegistry[0]
        }.message shouldBe "No Class registered for id = 0. Please annotate it with @ZKP or annotate a surrogate with @ZKPSurrogate."
    }

    @Test
    fun `identify class should return the correct id`() {
        TestSerializerRegistry.identify(TestStateV2::class) shouldBe TestStateV2::class.hashCode()
    }

    @Test
    fun `identify unknown class should raise ClassNotRegistered`() {
        shouldThrow<SerializerRegistryError.ClassNotRegistered> {
            TestSerializerRegistry.identify(UnknownState::class)
        }.message shouldBe "No registration for class com.ing.zkflow.common.versioning.UnknownState. Please annotate it with @ZKP or annotate a surrogate with @ZKPSurrogate."
    }

    @Test
    fun `tryGetKClass should return the correct class`() {
        TestSerializerRegistry.getKClass(TestStateV3::class.hashCode()) shouldBe TestStateV3::class
    }

    @Test
    fun `tryGetKClass for unknown id should raise ClassNotRegistered`() {
        shouldThrow<SerializerRegistryError.ClassNotRegistered> {
            TestSerializerRegistry.getKClass(0)
        }.message shouldBe "No Class registered for id = 0. Please annotate it with @ZKP or annotate a surrogate with @ZKPSurrogate."
    }
}
