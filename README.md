# Create Aeronautics: Mekanism Compatibility

Compatibility fixes for Mekanism blocks on Create Aeronautics / Sable contraptions.

## Currently Supported

- Mekanism Teleporter destination targeting on mounted contraptions.
- Mekanism Dimensional Stabilizer chunk tickets from mounted contraptions.
- Mekanism transmitters/pipes: configurator targeting, side selection, and visual updates while mounted.
- Mekanism Wind Generator projected height/sky checks while mounted. Disabled by default; enable in server config.
- Mekanism Digital Miner projected scanning/mining while mounted. Disabled by default; enable in server config.

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
