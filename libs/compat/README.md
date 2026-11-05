# Compat API jars (not in the repository)

The long energy bridges in `com.zhichaoxi.forestryoddities.compat.energy` are compiled against the
mods they talk to, so `build.gradle` adds their jars as `compileOnly` dependencies. Those jars are
third-party binaries and are **not committed** (`libs/compat/*.jar` is in `.gitignore`); this folder
only documents which ones are needed.

Copy these seven files in here before building, or `compileJava` fails on an unresolved dependency:

| file | mod | version this checkout was compiled against |
|---|---|---|
| `grandpower.jar` | GrandPower (`grandpower`) | 3.0.0 |
| `modern-industrialization.jar` | Modern Industrialization (`modern_industrialization`) | 2.5.11 |
| `flux-networks.jar` | Flux Networks (`fluxnetworks`) | 8.0.0 |
| `Mekanism-api.jar` | Mekanism API (`mekanism`) | 10.7.19 |
| `mekanism.jar` | Mekanism (`mekanism`) | 10.7.19 |
| `applied-flux.jar` | Applied Flux (`appflux`) | 1.21-2.1.5-neoforge |
| `ae2.jar` | Applied Energistics 2 (`ae2`) | 19.2.18 |

Each file is that mod's released NeoForge 1.21.1 jar (or, for `Mekanism-api.jar`, the API artifact of
the same release), taken from the mod's download page. They are the same jars Career Bees' equivalent
integrations compile against. They are `compileOnly`: nothing of them is shipped in the built jar, and
no class of them is ever read at runtime unless the mod itself is installed.

The compilation touches these classes, in case a jar is upgraded and one of them moves. Note that
three of the five integrations reach into a mod's own packages rather than an API package, so an
upstream refactor there is a compile error here, not a silent break:

* GrandPower - `dev.technici4n.grandpower.api.ILongEnergyStorage`,
  `dev.technici4n.grandpower.impl.NonLongWrapper` (internal: the wrapper GrandPower puts around a
  plain FE capability, which has to be recognised and skipped)
* Modern Industrialization - `aztech.modern_industrialization.api.energy.EnergyApi#SIDED`,
  `...api.energy.MIEnergyStorage` (implemented by the source view),
  `aztech.modern_industrialization.config.MIServerConfig` (for `forgeEnergyPerEu`)
* Flux Networks - `sonar.fluxnetworks.api.FluxCapabilities`, `...api.energy.IFNEnergyStorage`
* Mekanism - `mekanism.api.energy.IStrictEnergyHandler`, `mekanism.api.Action`,
  `mekanism.common.capabilities.Capabilities` and `mekanism.common.util.UnitDisplayUtils.EnergyUnit`
  (internal: both are in the full jar, which is why `mekanism.jar` is needed next to
  `Mekanism-api.jar`)
* Applied Flux - `com.glodblock.github.appflux.common.tileentities.TileFluxAccessor` and
  `...common.parts.PartFluxAccessor` (internal), plus AE2's `appeng.api.parts.IPart`/`IPartHost`

See the README's "Long energy (long 能量适配)" section for what the bridges do and how they are wired.
