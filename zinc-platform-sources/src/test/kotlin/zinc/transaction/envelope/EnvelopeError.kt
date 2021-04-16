package zinc.transaction.envelope

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

sealed class EnvelopeError(message: String) : IllegalArgumentException(message) {
    class StateRegistration(klass: KClass<out ContractState>) : EnvelopeError("ContractState ${klass.qualifiedName} has already been registered")
    class CommandRegistration(klass: KClass<out CommandData>) : EnvelopeError("CommandData ${klass.qualifiedName} has already been registered")

    class StateNoRegistration : EnvelopeError {
        constructor(klass: KClass<out ContractState>) : super("No registration for StateContract ${klass.qualifiedName}")
        constructor(id: Int) : super("No StateContract registered for id = $id")
    }

    class CommandNoRegistration : EnvelopeError {
        constructor (klass: KClass<out CommandData>) : super("No registration for CommandData ${klass.qualifiedName}")
        constructor(id: Int) : super("No CommandData registered for id = $id")
    }
}
