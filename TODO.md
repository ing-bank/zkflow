- Expand `StateVisibilityContractTest` with more tests. Add test similar to `Additional unchecked public only outputs of different types are not allowed` for inputs too.
- Move `ZKNotaryServiceFlow.MAX_ALLOWED_STATES_IN_TX` to `ZKNetworkParameters` and enforce also in `ZKTransactionBuilder` and `ZKVerifierTransaction`.
- Complete `CommandContextFactory.generateCheckEncumbrancesValidMethod` by checking in the outputs list that:
  ```
  a) an encumbered state does not refer to itself as the encumbrance
  b) the number of outputs can contain the encumbrance
  c) the bi-directionality (full cycle) property is satisfied
  d) encumbered output states are assigned to the same notary.
  ```

  Example code: 

  ```
  val statesAndEncumbrance = ltx.outputs
      .withIndex()
      .filter { it.value.encumbrance != null }
      .map { Pair(it.index, it.value.encumbrance!!) }
  
  if (statesAndEncumbrance.isNotEmpty()) {
      checkBidirectionalOutputEncumbrances(statesAndEncumbrance)
      checkNotariesOutputEncumbrance(statesAndEncumbrance)
  }
  ```
- Fix definition the `capacity` of `StandardTypes.digest` to not be hardcoded to 32, but to use the algo length from the currently active `ZKNetworkParameters`.
- Make writing structure file for circuits configurable/optional in `CircuitGenerator.generateCircuitFor()`. Currently, always written to file like this:
  ```
  // Write the structure of the different component groups
  codeGenerationOptions.transactionComponentOptions.forEach {
      buildPath.ensureDirectory("structure")
          .ensureFile("${it.type.getModuleName()}.txt")
          .writeText(it.type.toStructureTree().toString())
  }
  ```
- Fix `ZKNotary.isZKValidating()` to correctly determine if a notary is actually zkvalidating.