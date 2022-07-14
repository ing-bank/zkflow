## Serialization

Corda 4.8 entities exchange messages using an extended form of AMQP 1.0 as its binary wire [serialization protocol](serde48).

For ZKFlow additional messages must be passed to `zinc` for proving and verification. `zinc` implements a mathematical
abstraction of arithmetic circuits. A non-negotiable requirement for `zinc` is that all applicable inputs for any given circuit
1. [RQ1] **must not** differ in structure (this prohibits fields re-orderings too),
2. [RQ2] **must not** differ in size.

The AMQP serialization protocol preserves structure of classes and thus satisfies [RQ1],
the serialization length of a class instance depends on the contents of the instance, thus violating [RQ2]. 

Satisfying Requirement [RQ2] is non-trivial due to dynamic nature of Java or Kotlin; main obstacles are:
1. Collections' sizes must be known at compile time, e.g., size of a `List` or a `String`.
2. Exact implementation of an interface must be known at compile time, e.g., implementations for RSA and EdDSA public 
keys of `PublicKey` do differ both in structure and size.
3. Exact implementation of `null` must be known at compile time. Same argument holds as for the previous item.  
4. Every implementation must have a default value. For example, if a value is nullable, some appropriate value must be used
for serialization to satisfy the constant size requirement, or a collection is smaller than the expected size, a default
value is required to extend the collection to that size.

Item 3, unfortunately, prohibits the use of [Corda's pluggable serializers functionality](pluggable_serializers) because to define
an appropriate proxy object initial type of `null` must be known and there is no defined way to convey this information
to a pluggable serializer.

We conclude that the AMQP protocol is unfit for the purpose.

Corda 4.8 allows for custom [serialization schemes](custom_serde) via implementation of `CustomSerializationScheme` and
its consequent registration. We are leveraging this functionality to design a serialization protocol satisfying both
constant structure and constant size constraints.

### Approach
To implement a serialization scheme satisfying the requirements, we chose to leverage the following techniques and 
libraries:
1. The [Kotlinx serialization](kotlinx-serde) engine to generate serializers.

2. Annotations (we shall refer to them as "ZKP annotations") to let developers express 
   1. the upper bound of collections' sizes,
   2. the expected type of interface,
   3. the expected type of nullable fields,
   4. default values for the cases above.
   
3. [Kotlin Symbol Processing API](ksp-api), or KSP for short, for processing ZKP annotations and for registering the generated serializers for a subsequent
use in a custom serialization scheme.

#### Kotlinx serialization
Kotlinx serialization includes a compiler plugin which inspects the user code for `Serializable` annotations 
and generates appropriate serializers.

#### Annotations

| Annotation                                                       | Purpose                                                                                 | Requiring types                            |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------|--------------------------------------------|
| `Size(val size: Int)`                                            | Fix collection size                                                                     | `List`, `Map`, `Set`                       |
| `ASCIIChar`                                                      | An ASCII character occupies 1 byte                                                      | `Char`                                     |
| `UnicodeChar`                                                    | A Unicode character (codepoint) occupies 2 bytes                                        | `Char`                                     |
| `ASCII(val byteSize: Int)`                                       | Fix byte size of ASCII encoded string                                                   | `String`                                   |
| `UTF8(val byteSize: Int)`                                        | Fix byte size of UTF-8 encoded string                                                   | `String`                                   |
| `UTF16(val byteSize: Int)`                                       | Fix byte size of UTF-16 encoded string                                                  | `String`                                   |
| `UTF32(val byteSize: Int)`                                       | Fix byte size of UTF-32 encoded string                                                  | `String`                                   |
| `BigDecimalSize(val integerPart: Int, val fractionPart: Int)`    | Fix length of integer and fractional parts of a big decimal                             | `BigDecimal`                               |
| `Default<T : Any>(val provider: KClass<out DefaultProvider<T>>)` | Provide a default value for a nullable value or a (shorter than fixed size) collection. | Nullable values `X?`, `List`, `Map`, `Set` |
| `ZKP`                                                            | Designate a class for ZKP serialization                                                 | User class                                 |
| `Via<S : Surrogate<*>>`                                          | Annotation applied for 3rd party classes that cannot be annotated directly              | 3rd party classes                          | 

Annotation deserving a special annotation is `Via`. Its functional purpose similar [Corda's pluggable serializers](pluggable_serializers),
but it can be used together with `Default` making it much more powerful in conveying the expected type.
`Surrogate` interface is intended to be used as a specific representation of a class allow for simpler serialization,
its definition also requires conversion implementation to the original class.

#### Kotlin Symbol Processing API
KSP has been selected for **in-place** processing of ZKP annotations, i.e., rewriting the user code to generate
appropriate infrastructure and `Serializable` annotations employing that infrastructure for further processing by the Kotlinx serialization engine.

We may distinguish several kinds of serializers: 
* *constant*, serializer is known at compile-time, applies to any instance of a class and always produces a constant size
serialization, e.g., serializer for any `Int` will always produce a 4-bytes serialization. 
In `kotlin` terms, such serializers are objects.
* *configured*, serializer requires certain parameters, which are set system-wide and are known at compile time. Essentially,
configured serializers are instances of appropriate serializer classes constructed at compile-time. For example, by mandating
a signature scheme for `PublicKey`, all `Party`-s are going to be serialized with an instance of serializer for generic
`Party` with that fixed signature scheme; this instance is going to be deducible at compile time.
* *constructed*, serializers that are neither constant, nor configured, and thus are constructed at runtime using the runtime parameters.

Implementors of `CustomSerializationScheme` receive the following items for serialization. 
It is possible to directly invoke serializers designed with kotlinx serialization API for some of these items.

| Serialization item | Serialization | Construction parameters                                                                                                                                                                                                                                                                                 |
|--------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SecureHash`       | Configurable  | Digest algorithm. By Corda design, required to be SHA-256.                                                                                                                                                                                                                                              |
| `Party`            | Configurable  | Serializer for `Party` with a fixed signature scheme.<br/>Signature scheme is set system-wide at compile time.                                                                                                                                                                                          | 
| `StateRef`         | Constructed   | Serializer for `SecureHash` in `txhash`                                                                                                                                                                                                                                                                 |
| `TimeWindow`       | Direct        |                                                                                                                                                                                                                                                                                                         |
| `TransactionState` | Constructed   | <ul><li>Serializer for the inner `ContractState` in `data`</li><li>Serializer for `Party` in `notary`. Signature scheme is set system-wide at compile time.</li><li>Serializer for `AttachmentConstraint` in `constraint`. Kind of `AttachmentConstraint` is set system-wide at compile time.</li></ul> |
| `CommandData`      | Constructed   | Serializer for `CommandData`                                                                                                                                                                                                                                                                            |
| `List`             | Configurable  | This particular list is a list of signers and thus contains `PublicKey`s. Max expected size for private commands is required and is set at compile time; for public commands, actual size of the list is used.                                                                                          |

Configuration for configurable serializers is set in implementors of `ZKNetworkParameters`.
Serializers for `SecureHash`-s in `txhash` of `StateRef` are constructed at runtime, but all possible 
`SecureHash` serializers are known at compile time. On another hand, to correctly serialize `TransactionState`
and `CommandData`, serializers for `ContractState`'s and `CommandData`'s, must be discovered and accounted for easy
access at runtime. This process is executed as follows
* KSP scans the code and selecting implementations of `ContractState` and `CommandData` annotated with `ZKP`,
* for such classes `ContractStateSerializerRegistryProvider` and `CommandDataSerializerRegistryProvider` are generated
and registered with META-INF/services,
* at runtime, registered implementations of `ContractStateSerializerRegistryProvider` and `CommandDataSerializerRegistryProvider`
are loaded with `ServiceLoader` and stored in appropriate registries,
* these registries are queried for serializers during serialization of `TransactionState` and `CommandData`.

#### Limitations
Although Kotlinx serialization enables serialization of enum classes and objects, we currently do not support them.
The main reason is that ann appropriate translation mechanism between the Kotlin enums/objects and Zinc enums/objects
is currently absent. Implementation of such a translation is feasible but is left out scope due to time constraints.

[serde48]: https://docs.r3.com/en/platform/corda/4.8/enterprise/serialization.html
[pluggable_serializers]: https://docs.r3.com/en/platform/corda/4.8/open-source/cordapp-custom-serializers.html
[custom_serde]: https://docs.r3.com/en/api-ref/corda/4.8/open-source/kotlin/corda/net.corda.core.serialization/-custom-serialization-scheme/deserialize.html
[kotlinx-serde]: https://github.com/Kotlin/kotlinx.serialization
[ksp-api]: https://github.com/google/ksp