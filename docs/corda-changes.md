
* All core flows need to be open for extension/closed for modification: CollectSignaturesFlow, FinalityFlow, NotaryFlow (clientside). As it is now, we need to copy/paste them. We want to reuse as much core logic as possible, but we need to be able to deviate where needed.
* Add support for ZKP-friendly hash algorithms (Pedersen Hashes, Blake2s). We have a contribution almost ready for this.
* Add support for ZKP-friendly signature scheme (TBD)
* Setting CorDapp config in tests: this is possible for tests based on MockNetworParameters (TestCordappImpl.withConfig), but is impossible for simpler tests, which make use of MockServices. MockSer
* JacksonSupport mixins are all private, we want to reuse them. Same for the configureMapper function. Or maybe more fundamental: can we make the whole thing extendable
* NotarisationPayload is not flexible enough. Either make it support any type of payload, or allow us to use a different payload