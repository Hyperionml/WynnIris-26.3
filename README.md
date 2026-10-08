# WynnIris 26.3

> **This is a community port, not the original WynnIris.**
>
> This repository is a Minecraft **26.3** port of [**WynnIris**](https://github.com/clptvn/WynnIris)
> by [clptvn](https://github.com/clptvn) (further developed in [Hyperionml/WynnIris](https://github.com/Hyperionml/WynnIris)).
> The WynnIris 1.21.11 feature set was merged onto the upstream
> [Iris Shaders](https://github.com/IrisShaders/Iris) 26.3 codebase and then adapted to run on MC 26.3.
>
> **Not affiliated with, endorsed by, or supported by the WynnIris, Iris, or Wynncraft teams.**

## ⚠️ Vibe coded

This port is **vibe coded**: it was produced largely by AI-assisted, prompt-driven
development, with changes verified mainly by "does the game boot and render correctly"
rather than by careful, line-by-line human review.

Practically, that means:

- Treat this as **experimental software**. Expect rough edges.
- Some fixes are pragmatic workarounds rather than upstream-quality patches.
- Do not assume correctness, API cleanliness, or long-term maintainability.
- **Use at your own risk.** Back up your configs and worlds.

Bug reports and pull requests are welcome — just keep the above in mind.

## What this is

WynnIris adds native support for all Wynncraft item glint effects, rendered directly in
the shader pipeline. Effects work with any Iris-compatible shader pack, no resource pack
required.

If you are not playing on Wynncraft, use [official Iris](https://modrinth.com/mod/iris) instead.

## Differences from upstream WynnIris

- Ported to **Minecraft 26.3** (from WynnIris on 1.21.11)
- Rebased onto upstream Iris 26.3, so it also carries newer upstream fixes
- Port/adaptation commits are authored by `WynnIris Port <port@wynniris.local>`
- Everything else — features, config layout, shader pack folder — follows upstream WynnIris

## Features

Inherited from upstream WynnIris:

- **19 glint effects** — Tint, Rainbow, Glitch, Shadow, Aurora, Reflection, Plasma, Distort, Chrome, and more
- **Shader pack compatible** — Effects work on top of any Iris-compatible shader pack
- **Configurable brightness** — Adjust glint effect brightness via the Sodium settings slider (50%–200%)
- **Drop-in replacement** — Uses the same config files and shader pack folder as Iris

## Requirements

- Minecraft 26.3
- [Sodium](https://modrinth.com/mod/sodium)
- Fabric (NeoForge build available on GitHub releases)

## Support

**Do not report bugs of this port to the WynnIris or Iris developers.** They are not
responsible for it.

- **Bugs in this port:** [GitHub Issues](https://github.com/Hyperionml/WynnIris-26.3/issues)
- **Bugs in WynnIris itself (1.21.11):** [clptvn/WynnIris Issues](https://github.com/clptvn/WynnIris/issues)
- **Bugs in Iris itself:** [IrisShaders/Iris Issues](https://github.com/IrisShaders/Iris/issues)

## Building

```
./gradlew :fabric:build
```

Requires Java 21. Output jar is in `build/libs/`.

## Credits

- **[WynnIris](https://github.com/clptvn/WynnIris)** by clptvn — the original mod and all of its features
- **[Iris Shaders](https://github.com/IrisShaders/Iris)** — the base shader mod; all credit goes to the Iris team, coderbot, IMS212, and all Iris contributors
- The 26.3 port layer in this repository is community work and is not reviewed by either team

## License

Licensed under [GNU LGPLv3](LICENSE), same as Iris.

glsl-transformer is licensed under the GNU Affero General Public License version 3.
