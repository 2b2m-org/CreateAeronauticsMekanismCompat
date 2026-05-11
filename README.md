# Create Aeronautics: Mekanism Compatibility

Compatibility fixes for Mekanism blocks on Create Aeronautics / Sable contraptions.

## Currently Supported

All integrations are enabled by default.

- Mekanism Teleporters resolve mounted target positions and energy cost from projected world coordinates.
- Mekanism Dimensional Stabilizers keep chunk tickets aligned with the moving contraption.
- Mekanism transmitters/pipes support configurator targeting, side selection, and visual state updates while mounted.
- Mekanism Wind Generators use projected height and sky checks while mounted.
- Mekanism Digital Miners scan, display queued targets, chunk-load scan targets, and mine from projected world coordinates.

## Targets

- Minecraft 1.21.1
- NeoForge 21.1.228
- Create 6.0.10
- Create Aeronautics 1.2.1 / Sable 1.2.2
- Mekanism 10.7.19

## Build

Place local dependency jars in `libs/mods/`, then run:

```sh
./gradlew build
```

License: MIT.
