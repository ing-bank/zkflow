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

impl NewState {
    // To be generated from @ZincUpgrade
    fn upgrade_from(previous_state: OldState) -> Self {
        // resize field foo from 4 td 8.
        let foo = {
            let int_array: [u32; 8] = [0 as u32; 8];
            for i in 0..4 {
                int_array[i] = previous_state.foo.values[i];
            }
            U32List8::new(previous_state.foo.size, int_array);
        };
        Self {
            foo: foo,
        }
    }
}
```

Initially we will leave this up to the user, and later we will generate migration functions between collections of the
same type. In that case the upgrade code could be reduced to: (note the `resizeTo8` method) 

```rust
impl New {
    // To be generated from @ZincUpgrade
    fn upgrade_from(previous_state: OldState) -> Self {
        // resize field foo from 4 to 8.
        Self::new(previous_state.foo.resizeTo8())
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

Versioned classes are recognized by their `family`, which is an interface that extends the `Versioned` interface.
All classes that implement the same interface belong to the same family, and should be able to be ordered. The
order of the classes in the family is determined by a secondary 'upgrade' constructor that instantiates itself
from an instance of the previous iteration. From these upgrade constructors a linked list can be build.
It is an error when multiple classes can be upgraded from the same version, as it makes the list into a tree.

The upgrade constructor MUST be annotated with `@ZincUpgrade` where the corresponding upgrade is described in Zinc code.

As a new class will have to be introduced for every upgrade, we recommend adding a version suffix to the class name,
i.e. `"${className}V${version}"`.

```kotlin
import org.intellij.lang.annotations.Language

annotation class ZKP
annotation class ZincUpgrade(@Language("rust") val body: String)

interface Versioned
interface AStateI : Versioned // Family A, notice no fields or methods
interface BStateI : Versioned // Family B

@ZKP
data class AStateV1(val a1: Int) : AStateI // Family A, first class, notice no upgrade constructor

@ZKP
data class BStateV1(val b1: Int) : BStateI // Family B, first class

@ZKP
data class AStateV2(val a1: Int, val a2: Int) : AStateI { // Family A, second class
    @ZincUpgrade("Self::new(previous_version.a_1, 0 as i32)") // Zinc upgrade method body, variable name is derived from constructor parameter name, also note field name
    constructor(previousVersion: AStateV1) : this(previousVersion.a1, 0) // upgrade constructor, links A2 -> A1
}

@ZKP
data class AStateV3(val a1: Int, val a2: Long) : AStateI { // Family A, third class
    // constructor(previousVersion: AStateV1): this(...) // FORBIDDEN, A2 already extends A1
    @ZincUpgrade("Self::new(previous_version.a_1, previous_version.a_2 as i64)")
    constructor(previousVersion: AStateV2) : this(
        previousVersion.a1,
        previousVersion.a2.toLong()
    ) // upgrade constructor, links A3 -> A2
}
```

When introducing a new version in a family, one can copy the latest version and make the necessary modifications.
The original classes MAY NOT be changed, except for the name in case the latest version doesn't have the
version in the name. Changing any field or constructors in original classes could break backwards compatibility,
meaning that old states cannot be deserialized anymore.

### State classes versus normal classes

There is a difference between state classes and classes that are used in state classes. To make sure that inside a
certain Contract or Command the latest version of the state class is used, one could choose to use a consistent name for
the latest version of a state class, more specifically the base name. This will make sure that all code that refers to
that state class is always using the latest version, and doesn't need to be explicitly upgraded.

This practice is strongly advised against for 'normal' classes that might be updated in the future, as otherwise all
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

import org.intellij.lang.annotations.Language
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction

/**
 * Interface that marks classes as versioned. This interface should always be applied to classes
 * using a marker interface, which is an empty interface that extends this interface.
 *
 * \`\`\`kotlin
 * interface Marker : Versioned
 * data class Marked(...): Marker { ... }
 * \`\`\`
 */
interface Versioned

/**
 * Annotation specifying the [body] of a static zinc method on the enclosing type.
 * The argument name of this method is derived from the parameter name in the constructor.
 *
 * \`\`\`rust
 * impl MyTypeV2 {
 *     fn upgrade_from(previous: MyTypeV1) -> Self {
 *         [body]
 *     }
 * }
 * \`\`\`
 */
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class ZincUpgrade(@Language("rust")val body: String)

/// Child data class versions
interface Child : Versioned

@ZKP
@Suppress("MagicNumber")
data class ChildV1(
    val name: @UTF8(8) String
) : Child

@ZKP
@Suppress("MagicNumber")
data class ChildV2(
    val id: Int,
    val name: @UTF8(8) String
) : Child {
    @ZincUpgrade("Self::new(0, previous.name)")
    constructor(previous: ChildV1): this(0, previous.name)
}

// Note that this is not a state class, so even though this is the latest version,
// the version IS included in the name.
@ZKP
@Suppress("MagicNumber")
data class ChildV3(
    val id: Int,
    val name: @UTF8(8) String,
    val description: @UTF8(32) String,
) : Child {
    @ZincUpgrade("Self::new(previous.id, previous.name, String32::empty())")
    constructor(previous: ChildV2): this(previous.id, previous.name, "")
}

/// Example contract
@Suppress("MagicNumber")
class MyContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        TODO("Not yet implemented")
    }

    /// Parent state class versions
    interface ParentI : Versioned

    @ZKP
    data class ParentV1(
        val child: ChildV1
    ) : ParentI, ContractState {
        override val participants: List<AnonymousParty> = emptyList()
    }

    @ZKP
    data class ParentV2(
        val child: ChildV2
    ) : ParentI, ContractState {
        @ZincUpgrade("Self::new(ChildV2::upgrade_from(previous.child))")
        constructor(previous: ParentV1): this(ChildV2(previous.child))

        override val participants: List<AnonymousParty> = emptyList()
    }

    // Note that the version is not included in the name, because this is the latest version of a state class.
    @ZKP
    data class Parent(
        val child: ChildV3
    ) : ParentI, ContractState {
        @ZincUpgrade("Self::new(ChildV3::upgrade_from(previous.child))")
        constructor(previous: ParentV2): this(ChildV3(previous.child))

        override val participants: List<AnonymousParty> = emptyList()
    }

    @ZKP
    class Issue : ZKCommandData {
        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                private(Parent::class) at 0 // here the state class is referred
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

- Example upgrade, notice the zinc verification is included:

```kotlin
class UpgradeParentV2ToParent : ZKCommandData {
    @Transient
    override val metadata = commandMetadata {
        numberOfSigners = 1
        inputs { private(hello.world.MyContract.ParentV2::class) at 0 }
        outputs { private(hello.world.MyContract.Parent::class) at 0 }
    }

    fun verify(ltx: LedgerTransaction) {
        val input = ltx.inputStates[0] as Versioned
        val output = ltx.outputStates[0] as Versioned

        // Structure, i.e. correct version types already enforced by ZKFlow metadata
        require(output == hello.world.MyContract.Parent(input)) {
            "Not a valid upgrade from ParentV2 to Parent"
        }
    }

    override fun verifyPrivate(): String = """
        mod command_context;
        use command_context::CommandContext;

        mod parent_v_2;
        use parent_v_2::ParentV2;

        mod parent;
        use parent::Parent;

        fn verify(ctx: CommandContext) {
            let input: ParentV2 = ctx.inputs.parent_v_2_0.data;
            let output: Parent = ctx.outputs.parent_0.data;

            assert!(output.equals(Parent::upgrade_from(input)), "Not a valid upgrade from ParentV2 to Parent");
        }
    """.trimIndent()
}
```

#### Zinc Upgrade circuit generation

Based on the above principles we can generate zinc circuits for each individual version step. So the programmers do not
need to write any more zinc than necessary in the `@ZincUpgrade`. From the `@ZincUpgrade`, a static function
`upgrade_from` is generated on the old version.

```rust
struct ParentV2 {
    name: Utf8String3,
}

struct Parent {
    id: i64,
    name: Utf8String3,
}

impl Parent {
    // Generated from @ZincUpgrade and constructor prototype.
    fn upgrade_from(previous: ParentV2) -> Self {
        Self::new(0, previous.name)
    }
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
