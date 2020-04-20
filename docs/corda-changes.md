
* Perhaps it should be made possible to add custom service to the ServiceHub. Since there is no other form of DI. This would solve a lot of problems. Both for real nodes an for the MockNet.
    * Example: we need custom config/services in our custom NotaryServiceFlow. In our case we need to for instance be able to set the serialization service and zkp verifier service. 
    * There is NotaryConfig.extraConfig, but this is not passed on to the NotaryService on construction. NotaryLoader.loadService does not handle it.
    * InternalMockNetwork.createNotaries only passes on the following from the NotarySpec: spec.validating and spec.className. Should also pass on extraConfig
* All core flows need to be open for extension/closed for modification: CollectSignaturesFlow, FinalityFlow, NotaryFlow (clientside). As it is now, we need to copy/paste them. We want to reuse as much core logic as possible, but we need to be able to deviate where needed.
* Add support for ZKP-friendly hash algorithms (Pedersen Hashes, Blake2s). We have a contribution almost ready for this.
* Add support for ZKP-friendly signature scheme (TBD)
