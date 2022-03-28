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

- State classes MAY evolve in newer versions of the CorDapp.
- The current version of the jar MUST be able to deserialize historical states, back to the first public release.

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

For example consider the case where a list of integer numbers is resized, to create the new list from the old list, one
would need to write the following code:

```rust
struct OldState {
    foo: U32List4,  // list of size 4
}

struct NewState {
    foo: U32List8,  // list of size 8
}

impl OldState {
    // To be generated from @ZincUpgradeMethod
    fn upgrade(self) -> NewState {
        // resize field foo from 4 td 8.
        let foo = {
            let int_array: [u32; 8] = [0 as u32; 8];
            for i in 0..4 {
                int_array[i] = self.foo.values[i];
            }
            U32List8::new(self.foo.size, int_array);
        };
        NewState {
            foo: foo,
        }
    }
}
```

Initially we will leave this up to the user, and later we will generate migration functions between collections of the
same type. In that case the upgrade code could be reduced to: (note the `resizeTo8` method) 

```rust
impl OldState {
    // To be generated from @ZincUpgradeMethod
    fn upgrade(self) -> NewState {
        // resize field foo from 4 to 8.
        NewState::new(self.foo.resizeTo8())
    }
}
```

### Modifying nested field types

In this case the serialized representation of an existing field changes using any of the above operations. The contract
verification code might need to be updated as well.

Any change in any of the fields will lead to a change in the serialized format of all parent classes that depend on this
type. This principle works recursively, so changes in `base` classes ripple through to the level of state classes.
Programmers must be aware of all the places that use this class, making this prone to human errors.

The way that we can support this is by making the version updates in the state classes very explicit. This way the
programmer is forced to think about the updates, and will not accidentally apply upgrades without stepping versions. 

## Upgrading of contract definitions

NOTE: Not applicable because we assume that old contract validation functions are never needed anymore after ZKP.

TODO @mvdbos: Validate in Corda code whether old contract validation functions are never needed anymore after ZKP.

Corda documentation for contract upgrades: [Write the new state and contract
definitions](https://docs.r3.com/en/platform/corda/4.8/open-source/upgrading-CorDapps.html#2-write-the-new-state-and-contract-definitions)

## State versioning: Alternative 1

Versioned classes implement the `Versioned` interface. This interface will hold a `version` identifier, which will be
used to populate the `SerializerMap`. The serialized form of a `Versioned` class will include the `version` id of the
class, so that on deserialization the correct deserializer and state version can be selected. Expected is that a `u16`
is sufficient for this. Next to that it contains some methods to help with converting from older versions to newer
versions.

As a new class will have to be introduced for every upgrade, we recommend adding a version suffix to the class name,
i.e. `"${className}V${version}"`. 

ZKFlow will provide tooling to help to manage the version upgrades. In order for these tools to find all related classes,
they must be located in the same scope. It is recommended for state classes to be located inside the `Contract` scope.
For every new version of a state class, the latest version will have to be copied and given a new name. After that, the
required modifications can be made to the latest class, and the upgrade from the previous to the latest version will
have to be defined in the `upgrade()` method definition. The `upgrade` method MUST be annotated with
`@ZincUpgradeMethod` where the corresponding upgrade is described in Zinc code.

Previous state classes MAY NOT be changed, because that could break backwards compatibility, meaning that old states
cannot be deserialized anymore.

### State classes versus normal classes

There is a difference between state classes and classes that are used in state classes. To make sure that inside a
certain Contract or Command the latest version of the state class is used, one could choose to use a consistent name for
the latest version of a state class, more specifically the base name. This will make sure that all code that refers to
that state class is always using the latest version, and doesn't need to be explicitly upgraded.

This practice is strongly adviced against for 'normal' classes that might be updated in the future, as otherwise all
usages of this class will be updated automatically. 

### Automatic upgrades

Contracts/Commands will always be defined in terms of the latest version, which requires that whenever an old state is
used in a new transaction, it has to be upgraded (step-by-step) to the latest version first. We may automate this at a
later stage by providing an `UpgradeToLatestFlow`, which can be integrated in the different core flows for a seamless
experience. We may also have the node run these upgrades async in the background when idle.

### Documented example code

Below is an example of a `Parent` state class that embeds a `Child` class which is updated twice.

```kotlin
package com.ing.zkflow.common.contracts.recursive

import com.ing.zkflow.Size
import com.ing.zkflow.ZKP
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction

/**
 * Interface that supports upgrading to newer iterations of the same data type.
 */
interface Versioned {
    /**
     * Version number. Will be included in the serialized form. Will be serialized as `u16`. It is recommended to use an
     * increasing version number, for readability and maintainability.
     */
    val version: Int

    /**
     * Specifies an upgrade to the next version. This means that whenever a new version is introduced, the upgrade must
     * be described on the previous version.
     */
    fun upgrade(): Versioned?
}

/**
 * Convenience interface for the latest version of a class. This will allow users to quickly locate the latest version
 * of a specific type. When introducing another version, the existing [LatestVersion] will have to be replaced with
 * [Versioned], and the upgrade will have to be defined.
 */
interface LatestVersion : Versioned {
    override fun upgrade(): Versioned? = null
}

/**
 * Recursively upgrade to the latest version.
 */
fun Versioned.toLatestVersion(): Versioned = upgrade()?.toLatestVersion() ?: this

/**
 * Upgrade to a specific [version].
 */
fun Versioned.upgradeToVersion(version: Int): Versioned? {
    return if (version == this.version) {
        this
    } else {
        upgrade()?.upgradeToVersion(version)
    }
}

/**
 * Upgrade until an instance of [T] is obtained.
 */
inline fun <reified T : Versioned> Versioned.upgradeTo(): T {
    // inline fun cannot be recursive, so we iterate
    var next = upgrade()
    while (next != null && next !is T) {
        next = next.upgrade()
    }
    return requireNotNull(next) {
        "Could not upgrade $this to ${T::class}"
    } as T
}

/**
 * Annotation specifying the [body] of a zinc method on the enclosing type.
 * The return type of this method is derived from the return type of the [Versioned.upgrade] method. 
 * 
 * ```rust
 * impl MyTypeV1 {
 *     fn upgrade(self) -> MyTypeV2 {
 *         [body]
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
annotation class ZincUpgradeMethod(val body: String)

/// Child data class versions
@ZKP
@Suppress("MagicNumber")
data class ChildV1(
    val name: @Size(8) String
) : Versioned {
    override val version: Int = 1
    // Note the return type is not [Versioned], but [ChildV2].
    @ZincUpgradeMethod("ChildV2::new(0, self.name)")
    override fun upgrade(): ChildV2 = ChildV2(0, name)
}

@ZKP
@Suppress("MagicNumber")
data class ChildV2(
    val id: Int,
    val name: @Size(8) String
) : Versioned {
    override val version: Int = 2
    @ZincUpgradeMethod("ChildV3::new(self.id, self.name, String32::empty())")
    override fun upgrade(): ChildV3 = ChildV3(id, name, "")
}

@ZKP
@Suppress("MagicNumber")
data class ChildV3(
    val id: Int,
    val name: @Size(8) String,
    val description: @Size(32) String,
) : LatestVersion {
    override val version: Int = 3
}

/// Example contract
@Suppress("MagicNumber")
class MyContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }

    /// Parent state class versions
    @ZKP
    data class ParentV1(
        val child: ChildV1
    ) : Versioned, ContractState {
        override val version: Int = 1
        @ZincUpgradeMethod("ParentV2::new(self.child.upgrade())")
        override fun upgrade(): ParentV2 = ParentV2(child.upgradeTo())
        override val participants: List<AnonymousParty> = emptyList()
    }

    @ZKP
    data class ParentV2(
        val child: ChildV2
    ) : Versioned, ContractState {
        override val version: Int = 2
        @ZincUpgradeMethod("Parent::new(self.child.upgrade())")
        override fun upgrade(): Parent = Parent(child.upgradeTo())
        override val participants: List<AnonymousParty> = emptyList()
    }

    // Note that the version is not included here, because this is the [LatestVersion] of a state class.
    @ZKP
    data class Parent(
        val child: ChildV3
    ) : LatestVersion, ContractState {
        override val version: Int = 3
        override val participants: List<AnonymousParty> = emptyList()
    }

    class Issue : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            network {
                attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class // to simplify DSL tests
            }
            commands {
                +Issue::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class // to simplify DSL tests
            private = true
            circuit {
                name = "issue_parent_state"
            }
            outputs {
                1 of Parent::class
            }
            numberOfSigners = 1
        }
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
class UpgradeParentV2ToParent : ZKTransactionMetadataCommandData {
    override val transactionMetadata by transactionMetadata {
        commands { +UpgradeParentV2ToParent::class }
    }

    @Transient
    override val metadata = commandMetadata {
        private = true
        numberOfSigners = 1
        inputs { 1 of hello.world.MyContract.ParentV2::class }
        outputs { 1 of hello.world.MyContract.Parent::class }
    }

    fun verify(ltx: LedgerTransaction) {
        val input = ltx.inputStates[0] as Versioned
        val output = ltx.outputStates[0] as Versioned

        // Structure, i.e. correct version types already enforced by ZKFlow metadata
        require(output == input.upgrade()) {
            "Not a valid transition from ParentV2 to Parent"
        }
    }

    init {
        BFLSerializationScheme.ZkCommandDataSerializerMap.register(this::class)
    }
}
```

#### Generate Zinc upgrade transaction verification function

On the Zinc side ZKFlow will generate an upgrade circuit with the following verification function:

```rust
fn verify(ltx: LedgerTransaction) {
    let input = ltx.inputs.my_state[0].state;
    let output = ltx.outputs.my_state[0].state;
    
    assert!(output.equals(input.upgrade()));
}
```

#### Zinc Upgrade circuit generation

Based on the above principles we can generate zinc circuits for each individual version step. So the programmers do not
need to write any more zinc than necessary in the `@ZincUpgradeMethod`. From the `@ZincUpgradeMethod`, a method
`upgrade` is generated on the old version.

```rust
struct ParentV2 {
    name: Utf8String3,
}

impl ParentV2 {
    // Generated from @ZincUpgradeMethod and update method prototype.
    fn upgrade(self) -> Parent {
        Parent::new(0, self.name)
    } 
}

struct Parent {
    id: i64,
    name: Utf8String3,
}

impl Parent {
    // Generated by zinc-poet:bfl
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

- [ ] [protocol] Implement `Versioned` interface for versions in the same scope.
- [ ] [zinc-poet] Generate circuits for upgrade commands.
  - [ ] [zinc-poet] Include content of `@ZincUpgradeMethod` in zinc code.
- [ ] Create compiler plugin that generates upgrade commands.

Should haves:

- [ ] [zinc-poet] generate migration methods to/from collections of the same type with different capacities

Could haves:

- [ ] Implement `UpgradeToLatestFlow` to combine more consecutive version steps in a single transaction
- [ ] Run upgrade commands for stored states async in the background when idle
