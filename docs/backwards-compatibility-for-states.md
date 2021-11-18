# Backwards Compatibility for States

As described on [Upgrading deployed
CorDapps](https://docs.r3.com/en/platform/corda/4.8/enterprise/node-operations-upgrade-cordapps.html) and [Upgrading
CorDapps](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-cordapps.html), newer versions of CorDapps can
make changes to states and contracts. However newer versions of the software should still be able to deserialize old
states. This document describes how we can support backwards compatibility in `ZKFlow`.

## Requirements

-   State classes MAY evolve in newer versions of the cordapp
-   The current version of the jar MUST be able to deserialize states serialized with an older version of the jar, back
    to the first public release

## Limitations

There are a couple of limitations/requirements that come from the current toolset.

- Zinc only supports fixed sized arrays, and array indices cannot be derived from the witness

- Corda requires that old states (and contract definitions) are available on the classpath of CorDapps

  Note: Due to the Zero-Knowledge-Proof we no longer need the old contract definitions anymore.

- Kotlinx-serialization only supports the primary constructor

  This is important because Corda uses multiple constructors to support upgrades to state classes. So due to our
  choice of kotlinx-serialization we have to use multiple implementations/classes for different state versions.

## Guidelines

- Offload the user as much as possible, to reduce the risk of human errors
- Explicit and robust
- Individual/sequential upgrade transactions
  
  This means that an upgrade from version 1 to 3 consists of 2 transactions: 1->2, 2->3 

  - This might lead to larger chains, so alternatively the upgrades are combined into a single transaction: 1->3

    This is more complicated, and leads to an exponential number of upgrade commands. Therefor this is out of scope 

- Program against implementations
- States with a historical version that are to be used in a new transaction will have to be upgraded with an explicit
  Upgrade command
- As described in [Preserve the existing state and contract
  definitions](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-cordapps.html#1-preserve-the-existing-state-and-contract-definitions),
  old state (and contract definitions) must be available on the classpath in newer versions of CorDapps
- The latest version of a stateclass MUST always have the same canonical class name.
- All versions of a state class MUST be located in the same scope (i.e. the Contract)

- We don't need to validate old Kotlin contract rules anymore, because we have a zero-knowledge proof for it.
- New nodes only need the latest version of contracts, but do need old states.
- We don't use the explicit upgrade process, but an implicit upgrade process.

## Upgrade operations on states

This section describes possible modifications to state classes.

The [Corda
documentation](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-cordapps.html#writing-new-states-and-contracts)
only describes adding and removing fields. But because with our BFL serialization scheme and the fixed arrays in Zinc,
we will have additional concerns about resizing collections.

### Add new field with default

In this case a newer version of a stateclass will contain (an) additional field(s) with a default value. As long as a
default value is provided, it is always possible to migrate data from an old version to the next. Note that the same
default value must be applied both in Kotlin and in Zinc code. In this case the serialized form changes, and the new
field(s) should be validated in the contract validation code (if applicable).

### Rename fields

In this case the name of existing fields changes. No changes to the serialized form, but the contract validation code
must be updated with the new names.

### Reorder fields

In this case the order of existing fields changes. The serialized form changes, but the contract validation code can
remain the same.

### Remove fields

In this case existing fields are removed. This is a destructive operation and leads to data-loss. The serialized
form and the contract validation code changes.

### Changing collection sizes, like Lists, Arrays and Strings

In this case collection sizes are either shrunk or grown. Making collection sizes smaller is a destructive operation and
will lead to data-loss when the actual data is shrunk. The serialized form changes and contract validation code may need
to be updated to account for the new sizes.

This type of operation is harder to support than the operations above, because Zinc array sizes are fixed. If we want to
support this, `zinc-poet` must generate additional methods to make the conversions. Alternatively we leave this up to
the user, but that is no good user experience.

Initially we will leave this up to the user, and later we will generate migration functions between collections of the
same size.

### Modifying nested field types

In this case the serialized representation of an existing field changes using any of the above operations.

Any change in any of the fields will lead to a change in the serialized format of all parent classes that depend on this
type. This principle works recursively, so changes in \`base\` classes ripple through to the level of state classes.
Programmers must be aware of all the places that use this class, making this prone to human errors.

This option is regarded too complicated, and is decided against.

#### Example showcase

Consider the following situation:

```kotlin
interface VersionedState
annotation class UTF8(val size: Int)
annotation class ZKP

@ZKP
data class Foo(
    val foo: Int,
) : VersionedState {
    override val version = 1
}

class FirstExampleContract: Contract {
    @ZKP
    data class Bar(
        val bar: Int,
        val foo: Foo,
    ) : VersionedState {
        override val version = 1
    }
}

class SecondExampleContract: Contract {
    @ZKP
    data class Baz(
        val baz: @UTF8(32) String,
        val foo: Foo,
    ) : VersionedState {
        override val version = 1
    }
}
```

Whenever `Foo` is modified, both `Bar` and `Baz` will have to be updated even though there are no visual changes,
because the serialized form and/or the contract verification code changed.

Since the `Foo` class could be originating from a library this should be considered whenever stepping a dependency.

We will not (explicitely) support these nested upgrades. Although if programmers really want, they can do it themselves.
As an alternative programmers are adviced to only use classes in their state classes that are garanteed to not evolve.

## Upgrade operations on transactions/contracts

NOTE: Not applicable because we assume that old contract validation functions are never needed anymore after ZKP.

TODO @mvdbos: Validate in Corda code whether old contract validation functions are never needed anymore after ZKP.

Corda documentation for contract upgrades: [Write the new state and contract
definitions](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-cordapps.html#2-write-the-new-state-and-contract-definitions)

## Alternative 1

Versioned classes implement the `VersionedState` interface. This interface will hold a `version` identifier, which will
be used to populate the `SerializerMap`. Next to that it contains some methods to help with converting from older
versions to newer versions.

`VersionedState` classes can not contain fields with a `VersionedState` type. This means no recursion for updates. Only
explicitly supported types and collections of explicitly supported types are allowed, or types which cannot be updated
anymore. This also means that libraries that provide types to be use in contract states cannot be updated.

The serialized form must include the `version` id of the class, so that on deserialization the correct deserializer and
state version can be selected. Expected is that a `u16` is sufficient for this.

All different versions of the state classes exist as Kotlin classes in the `Contract` scope, and are linked together
with constructors. For every new version of a state class, the old latest version is renamed by adding the version to
the name, and a new data class is defined with the original name. The new data class MUST get a single argument
constructor to create this instance from the previous version, with corresponding Zinc code (in a `ZincFactoryMethod`
annotation) to perform the conversion in Zinc code.

Contracts/Commands will always be defined in terms of the latest version, which requires that whenever an old state is
used in a new transaction, it has to be upgraded (step-by-step) to the latest version first. TODO can we automate this
for the client, or do clients need to take care of this themselves?

Below is an example of what a state class could look like with two versions.

```kotlin
interface VersionedState: ContractState {
    val version: Int
    // ...
}

@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ZincFactoryMethod(val code: String)

// sealed interface is used to bind the different versions together,
// so that annotation processors or compiler plugins can easily
// detect all corresponding state versions.
// It is recommended that all implementations are embedded in the
// sealed interface definition, for the above reason.
class MyContract: Contract {
    // The original state.
    // Note the version, i.e. 'V1', in the name.
    @ZKP
    data class MyStateV1(
        val name: @UTF(3) String,
    ): MyStateI, VersionedState(1) {
        override val version = 1
    }

    // New state that adds the `id` field, with default 0.
    // Note the absense of the version, f.e. =V2=, in the name.
    @ZKP
    data class MyState(
        val id: Long,
        val name: @UTF(3) String,
    ): MyStateI, VersionedState {
        // Constructor for previous version.
        // Note the single argument of this constructor.
        @ZincFactoryMethod("""
            fn newFromPreviousVersion(previous: MyStateV1): Self {
                Self {
                    id: 0,
                    name: previous.name
                }
            }
        """)
        constructor(previous: MyStateV1) : this(0, previous.name)

        override val version = 2
    }
}
```

### ZKFlow support for upgrades
`ZKFlow` will aid clients with these upgrades with the following features.

#### Kotlin Compiler Plugin to generate Upgrade commands
Based on the above principles we can create a Kotlin compiler plugin that can generate upgrade commands. And we can
generate upgrade circuits in zinc code.

### Zinc Upgrade circuit generation
Based on the above principles we can generate zinc circuits for each individual version step. So the programmers do not
need to write anymore zinc than necessary in the `@ZincFactoryMethod`.

## Alternative 2

Different constructors, same as Corda, with a changelog (like liquibase or flyway) describing the changes.

Because kotlinx-serialization only works with the primary constructor, multiple versions of the class need to be
generated from the changelog.

This alternative is regarded as currently being too complicated and requiring lots of work, might be revisited in the
future.

## Tasks

Must haves:
- [ ] [protocol] Implement `VersionedState` interface for versions in the same scope.
- [ ] [zinc-poet] Include content of `@ZincFactoryMethod` in zinc code.
- [ ] Create compiler plugin that generates upgrade commands.
- [ ] [zinc-poet] Generate circuits for upgrade commands.

Should haves:
- [ ] [zinc-poet] generate migration methods to/from collections of the same type with different capacities
