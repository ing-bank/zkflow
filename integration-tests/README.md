This is module is an 'integration' test module only in the sense that its submodules have the KSP and Arrow compiler plugins applied.
This means that if you have tests that use @ZKP annotations, or require other features these plugins provide, put them in one of these modules.
Otherwise, please put tests as close to the source they test, preferably in the same module.