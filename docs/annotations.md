Class instances need to be serialized for inter-corda and corda-zinc transport. Serialization framework is kotlinx.serialization.
Using that framework requires some knowledge of the serialization domain. We'd like to let the developer annotate
classes staying in a "common knowledge" or "natural" domain. The following annotation offer sufficiently flexibility to
derive the required serializers enabling the transport.

## Annotations

| Annotation | Purpose | Requiring types |
| --- | --- | --- |
| `Size(val size: Int)` | Fix collection size | `List`, `Map`, `Set` |
| `UTF8(val length: Int)` | Fix length of an UTF8 string | `String` | 
| `ASCII(val length: Int)` | Fix length of an ASCII string | `String` | 
|`BigDecimalSize`| Fix length of integer and fractional parts of a big decimal | `BigDecimal`|
| `Default<T : Any>(val provider: KClass<out DefaultProvider<T>>)` | Provide a default value for a nullable value ora (shorter than fixed size) collection. | Nullable values `X?`, `List`, `Map`, `Set` | 
| `Converter<T: Any>(val provider: KClass<out ConversionProvider<T, out Surrogate<T>>>)` | 3rd party classes that cannot be annotated directly will require conversion to/from annotatable surrogates. | 3rd party classes | 
|<pre>Resolver<T: Any>(<br>    val defaultProvider: KClass<out DefaultProvider<T>>,<br>    val converterProvider: KClass<out ConversionProvider<T, out Surrogate<T>>><br>)</pre>|Convenience annotation combining `Default` and `Converter` | Collections of 3rd party classes, nullable core types and nullable 3rd party classes | 
|`ZKP`| Designate an entry class for ZKP serialization | Any user class|

These annotations make use of the following interfaces

|Interface | Purpose |
| ---- | ---- |
```interface DefaultProvider<T: Any> { val default: T }``` | Nullables and collections will require default values |
```interface ConversionProvider<T: Any, S: Surrogate<T>> { fun from(original: T): S }``` | 3rd party classes will require conversion to/from surrogates.
|

These annotations are used to create kotlinx `Serializable(with=)` annotations to enable fixed-length serialization.

## Annotation processing
There are two options available targeting Kotlin (not Java).

|     |KSP   | Kotlin Arrow Meta |
| --- | --- | --- |
| Description | Kotlin Symbol Processing library. Allows processing Kotlin code on the syntactic level. | A part of the Kotlin Arrow offering. Meta is a compiler plugin |
| Write new code| YES, can be facilitated, for example, by Kotlin Poet. | YES, Kotlin Poet|
| Re-write code | NO | YES|
| Features | <ul><li>Existing code will have to be regenerated, e.g., `@ZKP` classes</li><li>Code re-generation can be coupled with Zinc code generation directly using the __same__ source</li><li>Pre-compile time failure: lack of required information will be evident before the "actual" compilation takes place</li></ul>| <ul><li>Precise code replacements can be executed in the right places, such as `@ZKP` to `@Serializable`</li><li>Zinc code needs to be generated using different means, for example, with `SerialDescriptor` of `kotlinx`</li><li>Compile time failure: `kotlinx` will fail compilation if there will insufficient serialization information</li><li>Depends on KSP</li></ul> |