# AE2S Extended Crafting

Fork of `GaLicn/Applied-Extended-Crafting`, retargeted from standalone AE2 pattern-provider blocks to an AE2S addon for Minecraft 1.12.2/Cleanroom.

Author: WuXiaoYa. Original author: GaLi.

## Current AE2S Route

This fork adds AE2S Pattern Provider style assembly stations for Extended Crafting recipes. It does not add a new pattern item and it does not convert AE2S patterns. Encode an ordinary AE2S processing pattern in the AE2S pattern terminal, then place or upload that pattern into the matching assembly station.

The stations reuse AE2S `PatternProviderLogic` for network crafting and terminal integration, but use their own block entity instead of subclassing AE2S `TilePatternProvider`. This keeps the assembly stations from exposing the default provider's extra generic tank or gas-looking return slots to probe overlays.

Supported now:

- Basic, Advanced, Elite, and Ultimate table assembly stations for 3x3, 5x5, 7x7, and 9x9 Extended Crafting table recipes.
- Crafting Core assembly station for Extended Crafting Combination recipes.
- Ender Crafter assembly station for Extended Crafting Ender Crafter recipes.
- AE2S Pattern Access Terminal support through the native Pattern Provider container path.
- Automatic pattern recognition: unsupported processing patterns are hidden from the network and rejected by the station inventory.
- High-version Applied Extended Crafting block models and textures, backported into the AE2S 1.12 resource namespace.
- Upgrade card support:
  - All assembly stations accept AE2S Pattern Expansion Cards, Parallel Cards, and Speed Cards.
  - Parallel Cards follow the AE2S Molecular Assembler limits: 0/1/2/3 cards allow 1/4/16/64 parallel crafts.
  - Speed Cards on timed Crafting Core jobs use the AE2S Molecular Assembler speed multipliers. Table jobs finish immediately, so speed cards are supported but have no visible timing effect there.

Workflow:

1. Encode an Extended Crafting recipe as an AE2S processing pattern with the required inputs and primary output.
2. Put the encoded processing pattern into the matching assembly station, or upload it through the AE2S Pattern Access Terminal.
3. Request the output from AE2S autocrafting as usual.

Table stations finish immediately and return the crafted output to the ME network. The Crafting Core station uses the Extended Crafting Combination recipe cost and per-tick rate, drawing AE power from the network before returning the result.

The Ender Crafter station scans the same 3-block radius for Ender Alternators as Extended Crafting's native Ender Crafter. It pauses without an alternator, reduces processing time using the configured alternator effectiveness, and also applies installed AE2S Speed Cards to timed jobs.

Flux Crafter and Compressor recipes are intentionally separate follow-ups. Those recipe families carry different timing or machine-state rules and should get their own station behavior.

## JEI / HEI Integration

When JEI or HEI is installed, the assembly stations are listed as recipe catalysts on the left of Extended Crafting's existing recipe pages:

| Recipe category | Assembly station |
| --- | --- |
| Basic crafting (3x3) | Basic Table |
| Advanced crafting (5x5) | Advanced Table |
| Elite crafting (7x7) | Elite Table |
| Ultimate crafting (9x9) | Ultimate Table |
| Combination crafting | Crafting Core |
| Ender crafting | Ender Crafter |

Original catalysts and recipes are preserved. Categories disabled in Extended Crafting's configuration do not receive catalysts. This integration only adds machine entries: it does not transfer ingredients or encode patterns with the recipe transfer button. JEI/HEI is optional and is not bundled in the addon; the plugin uses the JEI 1.12 API shared by HEI. Client behavior in each viewer and dedicated-server loading still require in-game verification.

## Build Notes

The project uses RetrofuturaGradle for Minecraft 1.12.2/Cleanroom and targets AE2S `1.1.0`. Older AE2S versions are not supported by this build.

The AE2S dependency jar is expected at one of:

- `libs/AppliedEnergistics2-Supergiant-1.1.0-dev.jar` (preferred for compilation)
- `libs/AppliedEnergistics2-Supergiant-1.1.0.jar`

Download the dependency from the [official AE2S v1.1.0 release](https://github.com/FormlessDragon/Applied-Energistics-2-Supergiant/releases/tag/v1.1.0). Dependency jars are not committed upstream. Use the regular (non-dev) jar in your game instance, not the development jar.

Build with JDK 25 (the configured compilation toolchain):

`gradle --no-daemon jar`
