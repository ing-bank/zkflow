# Optimizations

## TODO

* for visible components in the witness that are visible to the verifier and that we do not use for anything else inside the circuit other than Merkle root calculation, we can provide their leafHashes or even only GroupHash directly. This is likely the case for: timeWindow, networkParametersHash. Both are checked already by the non-validating notary. A special case is attachments. We don't use them now, but we may later. Perhaps leave them as AttachmentIds for now.

## IMPLEMENTED

*