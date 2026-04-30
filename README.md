# Scribble

> Expertly edit your books with rich formatting options, page utilities and more!

![Version](https://img.shields.io/github/v/release/chrrs/scribble?include_prereleases&style=flat-square)
![Build status](https://img.shields.io/github/actions/workflow/status/chrrs/scribble/build.yml?style=flat-square)
[![Modrinth](https://img.shields.io/modrinth/dt/yXAvIk0x?style=flat-square&logo=modrinth)](https://modrinth.com/mod/scribble)
[![CurseForge](https://img.shields.io/curseforge/dt/1051344?style=flat-square&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/scribble)

Read more about it on [Modrinth](https://modrinth.com/mod/scribble)
or [CurseForge](https://curseforge.com/minecraft/mc-mods/scribble).

## Project Structure

Scribble supports multiple Minecraft versions using [Stonecutter](https://stonecutter.kikugie.dev/).
The easiest way to interact with this is by using an IDE such as IntelliJ. To switch between versions,
use the Gradle tasks under the `stonecutter` category. To run the game, use the `runActive{Fabric,Neoforge}`
tasks.

Make sure to switch back to the latest version to commit changes.

### Release checklist

- Add a changelog entry to `CHANGELOG.md`.
- Manually trigger the Publish workflow on GitHub.
