# Changes in Zinc Code

This document describes the changes made in the Zinc code to assure its functionality with ZKFlow. Currently,
ZKFlow depends on Zinc version 0.1.5, the latest stable version that supports zero-knowledge proofs.
The changes we introduced in Zinc are either bug fixes or additional features that simplify the usage of Zinc in ZKFlow
operations. We implemented all changes in our `ing-fork` of Zinc which can be found publicly at https://github.com/ingzkp/zinc.

In the following, we list the changes made in the Zinc code with a brief explanation. The changes are also clarified within
the code where necessary.

## Adding Blake2s gadget ([PR#4](https://github.com/ingzkp/zinc/pull/4))
In ZKFlow, we use Blake2s as the hash function to perform Merkle tree operations. Zinc does not provide a gadget for Blake2s
despite the underlying cryptographic library, `franklin-crypto` has its implementation. Thus, we implemented the Blake2s gadget 
ourselves on top of Zinc. Blake2s gadget expects the input preimage in bits and outputs the digest in bits. Using the auxiliary
`to_bits` method of Zinc, Blake2s gadget can be used as follows:

```rust
use std::crypto::blake2s;
use std::convert::to_bits;

const BLAKE2S_HASH_SIZE: u64 = 256;
const INT32_BITS:u8 = 32;

fn main(preimage: u32) -> [bool; BLAKE2S_HASH_SIZE] {
    let preimage_bits: [bool; INT32_BITS] = to_bits(preimage);
    blake2s(preimage_bits)
}
```

### Fixing bit endianness in Blake2s gadget ([PR#2](https://github.com/ingzkp/zinc/pull/2))
In its original format, the hash digest of the `franklin_crypto` library does not match with the original Blake2 specification
and with the BouncyCastle library. Both the original spec and the BouncyCastle require a little-endian representation of 
**bytes** within the hash computation. And the same is for the `franklin_crypto`.
However, on top of that, `franklin_crypto` also requires little-endian ordering of **bits within each byte** due to the 
`UInt32` object type used in the implementation. `UInt32` is a representation of 32 Boolean objects as an unsigned integer, 
where the least significant bit is located in the first place.

To overcome the mismatch between the `franklin_crypto` and the original spec, we added a function in our gadget, 
`reverse_byte_bits()`, which reverses the bit order within every byte before and after hashing operation.

## Adding Blake2s multi-input gadget ([PR#7](https://github.com/ingzkp/zinc/pull/7))
In ZKFlow, we use Blake2s hash to compute the hash of two concatenated messages, such as `Hash(nonce || serialized_component)`.
We designed the `blake2s_multi_input` gadget to eliminate additional concatenation operations in the circuit. The gadget 
handles concatenation under the hood. `blake2s_mutli_input` expects exactly two preimages in bits as input. 
The gadget can be used within the circuity as follows:

```rust
use std::crypto::blake2s_multi_input;
use std::convert::to_bits;

const BLAKE2S_HASH_SIZE: u64 = 256;
const INT32_BITS:u8 = 32;

fn main(preimage1: u32, preimage2:u32) -> [bool; BLAKE2S_HASH_SIZE] {

    let preimage_bits_1: [bool; INT32_BITS] = to_bits(preimage1);
    let preimage_bits_2: [bool; INT32_BITS] = to_bits(preimage2);
    
    blake2s_multi_input(preimage_bits_1, preimage_bits_2)
    
}
```

The computations is equivalent to concatenation of two preimages and helps to eliminate concatenation steps:  

```rust
use std::crypto::blake2s;
use std::convert::to_bits;

const BLAKE2S_HASH_SIZE: u64 = 256;
const INT32_BITS:u8 = 32;

fn main(preimage1: u32, preimage2:u32) -> [bool; BLAKE2S_HASH_SIZE] {

    let preimage_bits_1: [bool; INT32_BITS] = to_bits(preimage1);
    let preimage_bits_2: [bool; INT32_BITS] = to_bits(preimage2);
    
    let mut preimage_bits_concatenated : [bool; 2 * INT32_BITS] = [false; 2 * INT32_BITS];
    preimage_bits_concatenated[0..INT32_BITS] = preimage_bits_1;
    preimage_bits_concatenated[INT32_BITS..(2* INT32_BITS)] = preimage_bits_2; 

    blake2s(preimage_bits_concatenated)
}
```

Similar to Blake2s gadget, the multi-input gadget also implements `reverse_byte_bits()` to assure correct ordering of the 
bits within each byte. Please check Blake2s gadget to learn more about this operation.

## Enabling `self` keyword ([PR#11](https://github.com/ingzkp/zinc/pull/11))

In Zinc version 0.1.5, `self` references are not supported despite `self` is reserved as a keyword. The implementation of
`self` references in Zinc are available from version 0.2.x. Thus, we backported this feature to Zinc 0.1.5, which provides
us the following enhancements:

- `Self`: type alias for the surrounding struct
- `self`: variable reference for the surrounding struct
- Method invocation: from `Struct::fun(self, vars)` to `self.fun(vars)`


## Enabling intermodule dependencies ([PR#6](https://github.com/ingzkp/zinc/pull/6))
Zinc version 0.1.5 does not support intermodule dependencies. Dependencies can only be used in the main module of the project.
With this update, we enable modules depending on other modules by topologically sorting source files based on their mod 
statements before starting compilation. This works, as long as there are no cyclic dependencies. If there are, compilation 
fails.


## Fixing `endif` memory bug ([PR#9](https://github.com/ingzkp/zinc/pull/9))

This update fixes a memory bug in the zinc VMs data stack. We observed that the data stack does not clean its assigned types
when it returns from a scope such as a function call or a conditional statement. If a new data type needs to be assigned
to the freed index in the stack, compilation fails. Below is an example code statement: 

```rust
fn pollute_stack(z: u8) {
}

fn testStack( ) {
    if true {
        let index: u16 = 22;
    } else {
        let index: i16 = 23;
    }
}

fn main() -> i8 {
    pollute_stack(8);
    testStack();
    -1
}
```
When we run this example, the compiler fails with the error message: 
```
[ERROR   zvm] type error: expected u16, got u8
                 at ./src/main.zn:12:5 (at testStack)
[ERROR   zvm] runtime error: type error: expected u16, got u8
[ERROR zargo] virtual machine failure: exit status: 1
```
The error is caused by the omitted type allocation in the data stack, which was assigned as type `u8` in `pollute_stack()`.
function. Ideally, within the scope of the `testStack()` function, this type should be cleaned and reassigned as `u16`.

Our fix provides a workaround to the problem, which enables value assignment only if old and new values are the same type and
does nothing otherwise.


## Fixing scope for `struct` ([PR#12](https://github.com/ingzkp/zinc/pull/12))

Zinc version 0.1.5 does not allow to have the same method names within the scope of different structs. With this update,
we add support for the same method name in separate structs, which enables us to implement methods of similar nature for
different structs. 




