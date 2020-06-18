# Limitations compared to normal Corda

* Limited number of inputs/outputs/references (4 each?)
* No support for attachments withing the circuit, too heavy to have as part of witness 
* Smart contracts and linked cicruit are not in attachments but previously distributed. Tx contains a reference to that contract version
* All states in a transaction must belong to one contract and circuit
* All transactions require a notary: 
    * otherwise outputs will not be in the accumulator and can't be used as inputs.
    * 