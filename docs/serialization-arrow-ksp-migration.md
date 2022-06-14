## Migration from Arrow Meta to Google KSP

To enable serialization `@ZKP` annotated classes, such classes are processed by Arrow Meta adding additional
appropriate from `kotlinx.serializable` and generating the correct serializers _in place_. That is, 
the original code is rewritten.

This approach can be critisized on the conceptual level, but on the technical level, it delivers the desired
results. Reasons to reconsider the approach and move on to only code generation are also very technical.

* After a rewrite, code is very difficult to debug.
* The status of Arrow Meta is dubious. 
  * Arrow Meta is not advertized anymore at [arrow-kt](arrow-kt) (as of June 2022)
  * Last distributions of the Arrow library [do not contain](pp-report) do not contain Meta.
  * API of the Arrow Meta library is [being actively changed](ve-report) to accommodate Kotlin IR representation. 
* Type resolution is _not_ a part of Arrow Meta, thus a custom  rudimantary type resolver has been created. This type resolver may behave unexpectedly during incremental compilation with ambiguous errors (see the first item).
* Code analysis is rudimentary. As the type resolution is (very much) suboptimal, certain invariant are not checked, but _trusted_, which may lead to unexpected errors at later stages of the compilation. For example, that a field `B` of a `ZKP`-annotated type `A` is also a `ZKP`-annotated type. 
* We anticipate problems with  data fields without backing fields.

This motivates to explore a different route to serialization using the lessons learnt in the Arrow Meta-based approach.\

Google Kotlin Symbol Processing library is used for code generation and is already employed in the project.

### Current state and transition vector
#### Current state
Arrow Meta plugin processes classes annotated with `ZKP` or `ZKPSurrogate` making them serializable.
The former is used to annotate own classes and the latter for 3rd-party classes which cannot be directly annotated
with `ZKP`. Classes annotated with `ZKPSurrogate` are called _surrogates_ and a good analogy for them is Data Transfer Objects (DTOs)
commonly used for database communication. Serializers of surrogates are invoked by applying a `Via<T>` annotation 
to a field with 3rd party type, as such 
```kotlin
@ZKP
data class MyClass(
  val field: @Via<Class3Surrogate> OutOfReach
)

@ZKPSurrogate(ConverterOutOfReach::class)
class OutOfReachSurrogate(val value: Int) : Surrogate<OutOfReach> {
  override fun toOriginal() = OutOfReach(value)
}

object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
  override fun from(original: OutOfReach) = OutOfReachSurrogate(original.value)
}

data class OutOfReach(val value: Int)
```

Serializers for `ZKP`-annotated `ContractState` and `CommandData` are registered in a special register and invoked any time a
`TransactionState` contain such a state or command. 

####Transition vector

Overall transition vector is instead of rewriting `ZKP`-annotated classes is to generate respective field-by-field
surrogates and converters. This is possible because:
* Every field shall contain sufficient information for its serialization. This behavior is no different from the one implemented in the Arrow Meta plugin.
* Every field of a class must be either directly assignable to its counterpart or via invocation of a converter specified in a `Via` annotation. The `Via`-requirement is no different from the one implemented in the Arrow Meta plugin.

Serialization of the data field in `TransactionState` will require (slightly) more care.
The command and states registry must now contain per command/state both a respective serializer and converter from an instance to its surrogate.

[arrow-kt]: https://arrow-kt.io/
[pp-report]: https://github.com/arrow-kt/arrow-meta/issues/869
[ve-report]: https://github.com/arrow-kt/arrow-meta/pull/956#issuecomment-992528341