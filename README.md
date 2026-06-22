# Create Aeronautics: Mekanism Compatibility

Compatibility fixes for Mekanism blocks that are moved into Sable sub-levels,
including Create Aeronautics contraptions.

## Currently Supported

All integrations are enabled by default.

- Mekanism Teleporters and Portable Teleporters resolve mounted target positions and energy cost from projected world coordinates.
- Mekanism Dimensional Stabilizers keep chunk tickets aligned with the moving contraption.
- Mekanism transmitters support configurator targeting, side selection, contents preservation, and visual state updates while mounted.
- Mekanism Fluid Tanks and Energy Cubes render stored contents from mounted sub-levels.
- Mekanism Mechanical Pipes, Universal Cables, Pressurized Tubes, Thermodynamic Conductors, and Logistical Transporters render mounted contents and overlays in the correct coordinate space.
- Mekanism Lasers keep ray hits and beam particles aligned across sub-level boundaries.
- Mekanism machine sounds use Sable-aware distance checks while mounted.
- Mekanism Wind Generators use projected height and sky checks while mounted when Mekanism Generators is installed.
- Mekanism Digital Miners scan, display queued targets, chunk-load scan targets, and mine from projected world coordinates.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.228+
- Mekanism 10.7.19+
- Sable 1.2.2+
- Mekanism Generators 10.7.19+ is optional at runtime and only enables the mounted Wind Generator integration.

Create and Create Aeronautics are not declared as hard dependencies by this mod.
The current release has been tested in a Create 6.0.10 / Create Aeronautics
1.2.1 stack.

## Build

This repository does not vendor third-party mod jars. To build locally, place
matching compile-time jars in `libs/mods/`:

- Mekanism
- Mekanism Generators
- Sable

Then run:

```sh
./gradlew build
```

License: MIT.
