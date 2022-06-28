package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.common.serialization.KClassSerializerProvider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.corda.core.contracts.ContractState
import net.corda.core.internal.writeText
import java.nio.file.Paths
import java.util.ServiceLoader
import kotlin.reflect.full.isSubclassOf

fun main() {
    val commandDataSerializerRegistryProviders = ServiceLoader.load(KClassSerializerProvider::class.java)
    val structureTypes = commandDataSerializerRegistryProviders
        .asSequence()
        .map { it.get().klass }
        .filter { it.isSubclassOf(ContractState::class) }
        .flatMap { BflStructureGenerator.generate(it.serializer().descriptor).toFlattenedClassStructure() }
        .distinct()
        .toList()
    val jsonString = Json.encodeToString(ListSerializer(BflStructureType.serializer()), structureTypes)
    Paths.get("src/bfl/structure.json").writeText(jsonString)
}
