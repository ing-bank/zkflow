# LaTex setup

Our gradle build is set up to build the LaTex docs in the `docs/` subproject.
It requires `pdflatex` and `bibtex` to be installed. On Mac, an easy way to do that is to install MikTex. (`brew cask install miktex-console`)

Easy editing:
* Any Jetbrains IDE, with the TeXiFy IDEA plugin.

Live preview:
* run the gradle build with `./gradlew buildLatex --continuous`
* use Skim (`brew cask install skim`) as PDF viewer which supports live reload: https://tex.stackexchange.com/a/43060
