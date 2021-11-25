# Backwards Compatibility for States

As described on [Upgrading deployed
CorDapps](https://docs.r3.com/en/platform/corda/4.8/enterprise/node-operations-upgrade-CorDapps.html) and [Upgrading
CorDapps](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-CorDapps.html), newer versions of CorDapps can
make changes to states and contracts in two ways:

- **Implicit upgrade**: Use constraints to allow multiple implementations of contracts, so there is no need for
  upgrade transactions for the contract.
- **Explicit upgrade**: Upgrade contracts via a special *contract upgrade transaction*, requires all participants of a
  state to sign it using the contract upgrade flows.

In ZKFlow **Implicit upgrades** are used.

Current versions of the software should still be able to deserialize historical states from storage, for example in
order to reuse in new transactions. This document describes how we can support backwards compatibility in `ZKFlow`.

## Requirements

-   State classes MAY evolve in newer versions of the CorDapp.
-   The current version of the jar MUST be able to deserialize historical states, back to the first public release.

## Limitations

There are a couple of limitations/requirements that come from the current toolset.

- Zinc only supports fixed size arrays.

  Because the version determines what the serialized state looks like, `ZKTransactions` can only be exchanged between
  nodes running the same version of the CorDapp.

- Zinc does not allow array indices to be derived from the witness.
- Kotlinx-serialization only supports the primary constructor.

  This is important because Corda uses multiple constructors to support upgrades to state classes. So due to our
  choice of kotlinx-serialization we have to use multiple implementations/classes for different state versions.

## Guidelines

The design discussed in this document is based on the following guidelines and considerations.

- Offload the user as much as possible, to reduce the risk of human errors.
- Explicit and robust.
- Program against implementations.
- Individual/sequential upgrade transactions.
  
  This means that an upgrade from version 1 to 3 consists of 2 transactions: (1->2), (2->3). 

  - This might lead to larger chains, so alternatively the upgrades are combined into a single transaction: (1->3).

    This is more complicated, and leads to an exponential number of upgrade commands. For those reasons this is
    initially out of scope.

- In order to support deserialization of historical states, and upgrading them to the current versions, historical
  versions of the state classes must be available on the classpath.
- The current version of contracts is always defined in terms of current versions of the state classes involved.
  - The latest version of a state class MUST always have the same canonical class name, i.e. the version suffix will be
    added whenever changes are made to the base class. This will ensure that the contract does not change.
- States with a historical version will have to be upgraded in (a) separate
  upgrade transaction(s) before they can be used in a new transaction.
- All versions of a state class MUST be located in the same scope (i.e. the Contract), so that historical versions of
  state classes can be easily detected, f.e. by compiler plugins, annotation processors or reflection.

Upgrading process: TODO is this relevant in this document?

- Upgraded nodes only need the latest version of contracts, but do need old states, to be able to deserialize old states
  from storage.
- New nodes on the other hand technically only need the latest versions of contracts and states, because:
  - New nodes can create only new states.
  - New nodes do not have old states in storage to use as input for new transactions.
  - If another node wants to transact with a new node (or any other node) it should make sure to upgrade the state to
    the latest version beforehand. So the new node will only ever receive states of the latest version. Only when the
    new node has completed at least one transaction, it is no longer considered new, and may need to deserialized an old
    state version.
- We don't use the explicit upgrade process using `UpgradeTransactions` from Corda, but an implicit upgrade process
  using `SignatureAttachmentConstraints` on states. This does imply separate upgrade transactions for states as
  mentioned earlier.

## Upgrade operations on states

This section describes possible modifications to state classes.

The [Corda
documentation](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-CorDapps.html#writing-new-states-and-contracts)
only describes adding and removing fields. But because our BFL serialization scheme does not include any metadata about
the structure of the serialized data, we will have additional concerns about updates on state classes. This section
discusses the considered update operations and the impact on the serialized form and zinc contract validation code.

### Add new field with default

In this case a newer version of a state class will contain (an) additional field(s) with a default value. As long as a
default value is provided, it is always possible to migrate data from the previous version to the next. Note that the
same default value must be applied both in Kotlin and in Zinc code. In this case the serialized form changes, and the
new field(s) should be validated in the contract validation code (if applicable).

### Rename fields

In this case the name of existing fields changes. No changes to the serialized form, but the contract validation code
must be updated with the new names.

Since this operation will introduce an upgrade transaction, we advice to only apply rename operations together with
other operations that do add or remove data in the serialized form.

### Reorder fields

In this case the order of existing fields changes. The serialized form changes, but the contract validation code can
remain the same.

Since this operation will introduce an upgrade transaction, we advice to only apply reorder operations together with
other operations that do add or remove data in the serialized form.

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

For example consider the case where a list of bytes is resized, to create the new list from the old list, one would
need to write the following code:

```rust
struct NewState {
    foo: U32List8,  // list of size 8
}

struct OldState {
    foo: U32List4,  // list of size 4
}

impl OldState {
    // Convenience method to make contract upgrade validation code easier to write.
    fn upgrade(self) -> NewState {
        NewState::new_from_previous_version(self)
    }
}

impl NewState {
    // To be generated from @ZincFactoryMethod
    fn new_from_previous_version(old_state: OldState) -> Self {
        let foo = {
            let int_array: [u32; 8] = [0 as u32; 8];
            for i in 0..4 {
                int_array[i] = old_state.values[i];
            }
            U32List8::new(old_state.size, int_array);
        }
        Self {
            foo: foo,
        }
    }
}
```

Initially we will leave this up to the user, and later we will generate migration functions between collections of the
same type. In that case the upgrade code could be reduced to: (note the `resizeTo8` method) 

```rust
impl OldState {
    // Convenience method to make contract upgrade validation code easier to write.
    fn upgrade(self) -> NewState {
        NewState::new_from_previous_version(self)
    }
}

impl NewState {
    // To be generated from @ZincFactoryMethod
    fn new_from_previous_version(old_state: OldState) -> NewState {
        Self {
            foo: old_state.foo.resizeTo8(),
        }
    }
}
```

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
because the serialized form and/or the contract validation code changed. Since the `Foo` class could be originating from
a library this should be considered whenever stepping a dependency.

We will not (explicitly) support these nested upgrades. Although if programmers really want, they can do it explicitly
by writing their own upgrade transaction with corresponding contract validations in both Kotlin and Zinc. In this case
ZKFlow does not support the programmer with the upgrades.

As an alternative, programmers are adviced to only use classes in their state classes that are guaranteed to be fixed,
like primitives and ZKFlow-supported classes and collections of these. In this case all the updates can be applied in
the state class.

Technically it is possible for clients to introduce their own domain logic classes and include them in state classes,
however since that will prevent them from changing over time we strongly advice against it.

## Upgrading of contract definitions

NOTE: Not applicable because we assume that old contract validation functions are never needed anymore after ZKP.

TODO @mvdbos: Validate in Corda code whether old contract validation functions are never needed anymore after ZKP.

Corda documentation for contract upgrades: [Write the new state and contract
definitions](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-CorDapps.html#2-write-the-new-state-and-contract-definitions)

## State versioning: Alternative 1

Versioned state classes implement the `VersionedState` interface. This interface will hold a `version` identifier, which will
be used to populate the `SerializerMap`. Next to that it contains some methods to help with converting from older
versions to newer versions.

`VersionedState` classes can not contain fields with a `VersionedState` type. This means no recursion for updates. Only
explicitly supported types and collections of explicitly supported types are allowed, or types which cannot be updated
anymore. This also means that libraries that provide types to be used in contract states cannot be updated.

The serialized form must include the `version` id of the class, so that on deserialization the correct deserializer and
state version can be selected. Expected is that a `u16` is sufficient for this.

All different versions of the state classes exist as Kotlin classes in the `Contract` scope, and are linked together
with constructors. For every new version of a state class, the old latest version is renamed by adding the version to
the name, and a new data class is defined with the original name. The new data class MUST get a single argument
constructor to create this instance from the previous version, with corresponding Zinc code (in a `ZincFactoryMethod`
annotation) to perform the conversion in Zinc code.

Contracts/Commands will always be defined in terms of the latest version, which requires that whenever an old state is
used in a new transaction, it has to be upgraded (step-by-step) to the latest version first. We may automate this at a
later stage by providing an `UpgradeToLatestFlow`, which can be integrated in the different core flows for a seamless
experience. We may also have the node run these upgrades async in the background when idle.

Below is an example of what a state class could look like with two versions.

```kotlin
// Interface that marks a state class as versioned.
interface VersionedState: ContractState {
    val version: Int
    fun toNextVersion(): VersionedState { ... }
    fun toLatestVersion(): VersionedState { ... }
    // ...
}

// Annotation to perform the upgade on the zinc level.
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ZincFactoryMethod(val body: String)

// All state classes are defined inside the Contract class,
// so that annotation processors or compiler plugins can easily
// detect all corresponding state versions.
class MyContract: Contract {
    // The original state.
    // Note the version, i.e. 'V1', in the name.
    @ZKP
    data class MyStateV1(
        val name: @UTF(3) String,
    ): VersionedState {
        override val version = 1
    }

    // New state that adds the `id` field, with default 0.
    // Note the absense of the version, f.e. =V2=, in the name.
    @ZKP
    data class MyState(
        val id: Long,
        val name: @UTF(3) String,
    ): VersionedState {
        // Code inside this annotation is the body of a zinc method with a single
        // parameter that has the same name as the parameter in the real constructor,
        // i.e. in this case `previous`.
        @ZincFactoryMethod("Self::new(0, previous.name)")
        // Constructor for previous version.
        // Note the single argument of this constructor.
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

- Example upgrade:

```kotlin
class UpgradeMyStateV1ToMyState : ZKTransactionMetadataCommandData {
    override val transactionMetadata by transactionMetadata {
        commands { +UpgradeMyStateV1ToMyState::class }
    }

    @Transient
    override val metadata = commandMetadata {
        private = true
        numberOfSigners = 1
        inputs { 1 of hello.world.MyContract.MyStateV1::class }
        outputs { 1 of hello.world.MyContract.MyState::class }
    }

    fun verify(ltx: LedgerTransaction) {
        val input = ltx.inputStates[0] as VersionedState
        val output = ltx.outputStates[0] as VersionedState

        // Structure, i.e. correct version types already enforced by ZKFlow metadata
        require(output == hello.world.MyContract.MyState(input)) {
            "Not a valid transition from MyStateV1 to MyState"
        }
    }

    init {
        CommandDataSerializerMap.register(this::class)
    }
}
```

#### Generate Zinc upgrade transaction verification function

On the Zinc side ZKFlow will generate an upgrade circuit with the following verification function:

```rust
fn verify(ltx: LedgerTransaction) {
    let input = ltx.inputs.my_state[0].state;
    let output = ltx.outputs.my_state[0].state;
    
    assert!(output.equals(MyState::new_from_previous_version(input)));
    // or when we generate the `upgrade` convenience method:
    assert!(output.equals(input.upgrade()));
}
```

#### Zinc Upgrade circuit generation

Based on the above principles we can generate zinc circuits for each individual version step. So the programmers do not
need to write any more zinc than necessary in the `@ZincFactoryMethod`. From the `@ZincFactoryMethod`, a method
`new_from_previous_version` is generated on the new version, and a convenience method `upgrade` on the historical
version.

```rust
struct MyStateV1 {
    name: Utf8String3,
}

impl MyStateV1 {
    // Convenience method to make contract upgrade validation code easier to write.
    fn upgrade(self) -> MyState {
        MyState::new_from_previous_version(self)
    } 
}

struct MyState {
    id: i64,
    name: Utf8String3,
}

impl MyState {
    fn new_from_previous_version(previous: MyStateV1) -> Self {
        Self::new(0, previous.name)
    }
  
    fn new(id: i64, name: Utf8String3) -> Self { ... }
}
```

## State versionning: Alternative 2

Different constructors, same as Corda, with a changelog (like liquibase or flyway) describing the changes.

Because kotlinx-serialization only works with the primary constructor, multiple versions of the class need to be
generated from the changelog.

This alternative is regarded as currently being too complicated and requiring lots of work, might be revisited in the
future.

## Tasks

Must haves:

- [ ] [protocol] Implement `VersionedState` interface for versions in the same scope.
- [ ] [zinc-poet] Generate circuits for upgrade commands.
  - [ ] [zinc-poet] Include content of `@ZincFactoryMethod` in zinc code.
- [ ] Create compiler plugin that generates upgrade commands.

Should haves:

- [ ] [zinc-poet] generate migration methods to/from collections of the same type with different capacities

Could haves:

- [ ] Implement `UpgradeToLatestFlow` to combine more consecutive version steps in a single transaction
- [ ] Run upgrade commands for stored states async in the background when idle
