# Deterministic State Ordering in ZKTransactionBuilder

Since zinc does not support polymorphism, the states of a zero-knowledge transaction should be grouped by their state class.
Because there should be no ambiguity in the expected order of states, we must ensure a deterministic ordering of those states in 
`ZKTransactionBuilder`, the `WireTransaction` it generates, and in the `Witness` which gets passed to zinc.

The way we do that is by ordering the states in the `ZKTransactionBuilder` lexicographically by their class name. 
In case two states have the same class name, the first that was added by the user in the `ZKTransactionBuilder` will come before the other. 
This is done to maintain an order that is as faithful as possible to the order in which the user added the states to the transaction.