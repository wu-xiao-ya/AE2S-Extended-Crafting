# AE2S Extended Crafting

Fork of `GaLicn/Applied-Extended-Crafting`, retargeted from standalone AE2 pattern-provider blocks to an AE2S addon for Minecraft 1.12.2/Cleanroom.

## Current AE2S Route

This fork adds AE2S Pattern Provider style assembly stations for Extended Crafting recipes. It does not add a new pattern item and it does not convert AE2S patterns. Encode an ordinary AE2S processing pattern in the AE2S pattern terminal, then place or upload that pattern into the matching assembly station.

The stations reuse AE2S `PatternProviderLogic` for network crafting and terminal integration, but use their own block entity instead of subclassing AE2S `TilePatternProvider`. This keeps the assembly stations from exposing the default provider's extra generic tank or gas-looking return slots to probe overlays.

Supported now:

- Basic, Advanced, Elite, and Ultimate table assembly stations for 3x3, 5x5, 7x7, and 9x9 Extended Crafting table recipes.
- Crafting Core assembly station for Extended Crafting Combination recipes.
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

Ender Crafter, Flux Crafter, and Compressor recipes are intentionally separate follow-ups. Those recipe families carry different timing or machine-state rules and should get their own station behavior.

## Build Notes

The project uses RetrofuturaGradle for Minecraft 1.12.2 and compiles against the current AE2S target, `1.0.10+552188496`, matching the Starlight Tech McPatch client pack. Runtime API calls are written for this AE2S line, not for the older `1.0.9` storage signature.

The AE2S dependency jar is expected at one of:

- `libs/AppliedEnergistics2-Supergiant-1.0.10+552188496-dev.jar`
- `libs/AppliedEnergistics2-Supergiant-1.0.10+552188496.jar`

It is copied from the local Gradle cache or the target pack on this machine during setup, not committed upstream by design.

Build with a modern JDK:

`gradle --no-daemon jar`
