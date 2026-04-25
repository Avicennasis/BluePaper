# Contributing to BluePaper

Thanks for considering a contribution. Bug reports, docs fixes, and small
improvements are all welcome.

## Dev setup

```bash
git clone https://github.com/Avicennasis/BluePaper.git
cd BluePaper
# Requires JDK 17. On Linux:
sudo apt install openjdk-17-jdk
```

## Running the tests

```bash
./gradlew :shared:desktopTest
```

CI also compiles the desktop and Android targets:

```bash
./gradlew :shared:compileKotlinDesktop :desktopApp:compileKotlinDesktop
./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlinAndroid
```

Make sure those pass locally before opening a PR.

## PR checklist

- [ ] Tests added or updated; `./gradlew :shared:desktopTest` is green locally.
- [ ] Both desktop and Android targets compile cleanly.
- [ ] README and docs updated if public behavior changed.
- [ ] `CHANGELOG.md` updated under `[Unreleased]`.

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
Be respectful; assume good faith.
