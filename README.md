![ZK Notary SDK CI](https://github.com/ingzkp/zk-notary/workflows/ZK%20Notary%20SDK%20CI/badge.svg?branch=master)

## Prerequisites

This project makes use of our fork of Corda maintained here: https://github.com/ingzkp/corda/tree/ing-fork
This fork is based on the latest version of Corda and will have all our proposed PRs to Corda already merged.
The artifacts for our fork are deployed to Github Packages and this project is aware of that and will be able to find Corda dependencies there

If you want to make changes to our fork (please consult with Matthijs first), you can do the following:
```bash
$ git clone https://github.com/ingzkp/corda
$ cd corda
$ git checkout ing-fork
$ git checkout -b feature/foobar
```
Then make you changes in your branch and create a PR. Please note that if you want to make these change available through
Github Packages, that you should also bump the Corda patch version (`cordaVersion`) in the file `constants.properties`.
Normally we would just use a SNAPSHOT version for this, but unfortunately Github Packages does not properly support this at this time.
That means we will just have to bump the version when we introduce a feature that we want to make available.

## Contributing

In this repo, we will use Github Flow: https://guides.github.com/introduction/flow/

The aim is to have a commit history that looks like this, where feature branches always have linear history and no merge commits inside themselves.
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 89e8283 - Split DigestService for leaves and nodes 
| * 2f5b0bc - Add limitations doc
|/
*   dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
|\
| * 4ff33b7 - First version of e2e notarisation protocol
|/
*   ff0db31 - Merge pull request #13 from ingzkp/feature/zkwtx 
|\
| * db8d499 - Add ZKProverTx, ZKVerifierTx and JSON serialization 
|/
* 75b47d0 - Change Github Actions cache key 
```
One of the challenges in creating a meaningful history like this, is in keeping feature branches up to date with master without polluting their history with merge commits, and therefore polluting master when they are merged to master. Keeping a feature branch up to date by merging master into it regularly causes lots of merge commits from master like this This is not what we want to see:
```
* 58e7cea - (HEAD -> master, origin/master) Explicitly specify DigestServices in test
*   78b0bc4 - Merge pull request #18 from ingzkp/feature/simplify-flows
|\
| * 5328da4 - Upgrade ktlint version and import order
| * 12ab43c - Merge branch 'master' of github.com
| |\
| | * 98gf55c - Foo bar Baz
| | |
| * | 2f5b0bc - Add limitations doc
|/ /
|/   
* dad82db - Merge pull request #17 from ingzkp/feature/simple-e2e-zkp
```
There are two ways to prevent this. The best option is changing your development workflow to use regular rebasing on master as explained here: https://www.atlassian.com/git/tutorials/merging-vs-rebasing. Alternatively, if you really hate rebasing and especially the merge conflicts it can cause, you can also clean up your feature branch by using only one final rebase when you are ready to merge your branch into master: `git rebase -i origin/master`. Rebasing in itself already makes your feature branch history linear, but when you do it interactively, you can then `fixup` commits that you may not want to end up visibly in master, or `reword` bad commit messages.